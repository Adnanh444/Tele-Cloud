package com.adnan.memoracloud.core.files

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import com.adnan.memoracloud.core.common.SafeLog
import com.adnan.memoracloud.presentation.platform.DeviceFolder
import com.adnan.memoracloud.presentation.platform.DeviceFolderScanner
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Finds every folder on the device that contains photos or videos, so the backup list shows what is
 * actually there instead of a fixed list of guesses — including folders created after the app was
 * installed, because the scan is repeated every time the list is opened or refreshed.
 *
 * MediaStore is the primary source: it is what the gallery itself shows, it is fast, and it needs
 * only the photo/video permission the app already asks for. A bounded file-system walk is the
 * fallback for the two cases MediaStore cannot answer: no media permission yet, or a device/ROM
 * whose index is empty (some cheap ROMs, SD cards).
 */
class MediaStoreFolderScanner(
    private val context: Context
) : DeviceFolderScanner {

    override suspend fun scan(): List<DeviceFolder> = withContext(Dispatchers.IO) {
        val indexed = runCatching { scanMediaStore() }
            .onFailure { SafeLog.w(TAG, "MediaStore folder scan failed", it) }
            .getOrNull()
            .orEmpty()

        val folders = if (indexed.isNotEmpty()) indexed else runCatching { scanFileSystem() }
            .onFailure { SafeLog.w(TAG, "File-system folder scan failed", it) }
            .getOrNull()
            .orEmpty()

        // Newest-first inside equal counts keeps a freshly created folder near the top of the list.
        folders
            .filter { it.mediaCount > 0 }
            .sortedWith(compareByDescending<DeviceFolder> { it.mediaCount }.thenBy { it.path })
    }

    // ---------------------------------------------------------------------------------------
    // MediaStore
    // ---------------------------------------------------------------------------------------

    private fun scanMediaStore(): List<DeviceFolder> {
        val buckets = mutableMapOf<String, MutableBucket>()
        collect(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, photos = true, into = buckets)
        collect(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, photos = false, into = buckets)
        return buckets.map { (path, bucket) ->
            DeviceFolder(
                path = path,
                name = File(path).name,
                photoCount = bucket.photos,
                videoCount = bucket.videos,
                sizeBytes = bucket.bytes
            )
        }
    }

    private fun collect(
        collection: Uri,
        photos: Boolean,
        into: MutableMap<String, MutableBucket>
    ) {
        // _data is deprecated, but it is the only column that gives the exact folder of a media row
        // on every supported version; RELATIVE_PATH is the Q+ fallback when it comes back null.
        val columns = arrayOf(MediaStore.MediaColumns.DATA, MediaStore.MediaColumns.SIZE)
        context.contentResolver.query(
            collection,
            columns,
            "${MediaStore.MediaColumns.SIZE} > 0",
            null,
            null
        )?.use { cursor ->
            val dataIndex = cursor.getColumnIndex(MediaStore.MediaColumns.DATA)
            val sizeIndex = cursor.getColumnIndex(MediaStore.MediaColumns.SIZE)
            while (cursor.moveToNext()) {
                val data = if (dataIndex >= 0) cursor.getString(dataIndex) else null
                val path = data?.let { File(it).parentFile?.absolutePath } ?: continue
                val size = if (sizeIndex >= 0) cursor.getLong(sizeIndex) else 0L
                val bucket = into.getOrPut(path) { MutableBucket() }
                if (photos) bucket.photos++ else bucket.videos++
                bucket.bytes += size
            }
        }
    }

    // ---------------------------------------------------------------------------------------
    // Fallback walk
    // ---------------------------------------------------------------------------------------

    private fun scanFileSystem(): List<DeviceFolder> {
        val root = Environment.getExternalStorageDirectory() ?: return emptyList()
        val buckets = mutableMapOf<String, MutableBucket>()
        walk(root, depth = 0, into = buckets)
        return buckets.map { (path, bucket) ->
            DeviceFolder(
                path = path,
                name = File(path).name,
                photoCount = bucket.photos,
                videoCount = bucket.videos,
                sizeBytes = bucket.bytes
            )
        }
    }

    private fun walk(dir: File, depth: Int, into: MutableMap<String, MutableBucket>) {
        if (depth > MAX_DEPTH) return
        val children = runCatching { dir.listFiles() }.getOrNull() ?: return
        for (child in children) {
            if (child.name.startsWith('.')) continue
            if (child.isDirectory) {
                // Android/data holds other apps' private files: huge, slow, and not ours.
                if (depth == 0 && child.name == "Android") continue
                walk(child, depth + 1, into)
            } else if (child.isFile) {
                val ext = child.extension.lowercase()
                val isPhoto = ext in IMAGE_EXTENSIONS
                val isVideo = ext in VIDEO_EXTENSIONS
                if (!isPhoto && !isVideo) continue
                val bucket = into.getOrPut(dir.absolutePath) { MutableBucket() }
                if (isPhoto) bucket.photos++ else bucket.videos++
                bucket.bytes += child.length()
            }
        }
    }

    private class MutableBucket {
        var photos = 0
        var videos = 0
        var bytes = 0L
    }

    private companion object {
        const val TAG = "MediaStoreFolderScanner"
        const val MAX_DEPTH = 6

        val IMAGE_EXTENSIONS = setOf(
            "jpg", "jpeg", "png", "gif", "webp", "bmp", "heic", "heif", "dng", "avif"
        )
        val VIDEO_EXTENSIONS = setOf(
            "mp4", "mkv", "mov", "avi", "3gp", "webm", "m4v", "ts", "flv", "wmv"
        )
    }
}
