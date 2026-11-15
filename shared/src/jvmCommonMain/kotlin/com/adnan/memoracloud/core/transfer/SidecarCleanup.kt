package com.adnan.memoracloud.core.transfer

import com.adnan.memoracloud.core.common.AppResult
import com.adnan.memoracloud.domain.model.DriveFile
import com.adnan.memoracloud.domain.model.FileQuerySpec
import com.adnan.memoracloud.domain.repository.FileRepository
import com.adnan.memoracloud.domain.repository.TransferRepository
import com.adnan.memoracloud.domain.repository.TrashRepository
import kotlinx.coroutines.flow.first

/**
 * Google Takeout puts a small JSON next to every photo it exports, holding the metadata Google kept
 * (`IMG_20250101_120000.jpg.supplemental-metadata.json`, 750-odd bytes each). Importing an export
 * therefore fills the channel with them: one extra message per photo, and one extra row in the
 * index, for no benefit - the app stores the same information in the caption of the photo itself.
 *
 * Turning the "Other" file-type switch off stops new ones, but anything already uploaded has to be
 * removed, and doing that by hand in the file browser is unreasonable at this scale.
 */
class SidecarCleanup(
    private val fileRepository: FileRepository,
    private val transferRepository: TransferRepository,
    private val trashRepository: TrashRepository
) {

    /** Everything in the index that looks like a Takeout metadata sidecar. */
    suspend fun find(): List<DriveFile> =
        fileRepository.observeFiles(FileQuerySpec(showHidden = true, showArchived = true))
            .first()
            .filter { isSidecar(it.name) }

    /**
     * Stops any upload of [ids] first, then deletes them for good: rows, local copies and the
     * Telegram messages. Chunked, because a Takeout import can hold thousands.
     */
    suspend fun delete(ids: List<String>): Int {
        var deleted = 0
        for (chunk in ids.chunked(CHUNK)) {
            runCatching { transferRepository.cancelForFiles(chunk) }
            when (trashRepository.deleteFilesPermanently(chunk)) {
                is AppResult.Success -> deleted += chunk.size
                is AppResult.Failure -> Unit
            }
        }
        return deleted
    }

    companion object {

        private const val CHUNK = 200

        /** Media extensions that Takeout appends `.json` to in older exports. */
        private val MEDIA_SUFFIXES = listOf(
            ".jpg", ".jpeg", ".png", ".gif", ".webp", ".bmp", ".heic", ".heif", ".tif", ".tiff",
            ".mp4", ".m4v", ".mov", ".3gp", ".mkv", ".avi", ".webm"
        )

        fun isSidecar(name: String): Boolean {
            val lower = name.lowercase()
            if (lower.endsWith(".supplemental-metadata.json")) return true
            // Older exports used the plain "<photo>.json" naming.
            if (!lower.endsWith(".json")) return false
            val stem = lower.removeSuffix(".json")
            return MEDIA_SUFFIXES.any { stem.endsWith(it) }
        }
    }
}
