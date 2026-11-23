package com.adnan.memoracloud.core.files

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.provider.MediaStore
import com.adnan.memoracloud.core.common.SafeLog

/**
 * `IS_FAVORITE` exists from Android 11. Below that the flag is silently ignored rather than failing,
 * because favouriting must still work inside the app.
 */
class MediaStoreFavorites(private val context: Context) : DeviceFavorites {

    override fun setFavorite(absolutePath: String, favorite: Boolean) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return
        runCatching {
            for (collection in COLLECTIONS) {
                val uri = ContentUris.withAppendedId(collection, rowId(collection, absolutePath) ?: continue)
                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.IS_FAVORITE, if (favorite) 1 else 0)
                }
                context.contentResolver.update(uri, values, null, null)
            }
        }.onFailure { SafeLog.w(TAG, "Could not set the device favourite flag", it) }
    }

    override fun favoritePaths(): List<String> {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return emptyList()
        val paths = mutableListOf<String>()
        for (collection in COLLECTIONS) {
            runCatching {
                context.contentResolver.query(
                    collection,
                    arrayOf(MediaStore.MediaColumns.DATA),
                    "${MediaStore.MediaColumns.IS_FAVORITE} = 1",
                    null,
                    null
                )?.use { cursor ->
                    val column = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA)
                    while (cursor.moveToNext()) {
                        cursor.getString(column)?.let { paths.add(it) }
                    }
                }
            }.onFailure { SafeLog.w(TAG, "Could not read the device favourite flag", it) }
        }
        return paths.distinct()
    }

    private fun rowId(collection: android.net.Uri, absolutePath: String): Long? =
        context.contentResolver.query(
            collection,
            arrayOf(MediaStore.MediaColumns._ID),
            "${MediaStore.MediaColumns.DATA} = ?",
            arrayOf(absolutePath),
            null
        )?.use { cursor -> if (cursor.moveToFirst()) cursor.getLong(0) else null }

    private companion object {
        const val TAG = "DeviceFavorites"

        /** Images and video carry the flag that the system gallery displays. */
        val COLLECTIONS = listOf(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
    }
}
