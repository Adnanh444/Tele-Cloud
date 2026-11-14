package com.adnan.memoracloud.domain.usecase

import com.adnan.memoracloud.core.common.AppError
import com.adnan.memoracloud.core.common.AppResult
import com.adnan.memoracloud.domain.model.DriveFile
import com.adnan.memoracloud.domain.model.FileCategory
import com.adnan.memoracloud.domain.model.FileQuerySpec
import com.adnan.memoracloud.domain.repository.FileRepository
import com.adnan.memoracloud.domain.repository.TransferRepository

/** What the user wants back: everything in the backup, or one kind of file. */
enum class RestoreScope {
    EVERYTHING,
    MEDIA,
    DOCUMENTS,
    AUDIO;

    internal fun categories(): List<FileCategory> = when (this) {
        EVERYTHING -> emptyList()
        MEDIA -> listOf(FileCategory.IMAGE, FileCategory.VIDEO)
        DOCUMENTS -> listOf(FileCategory.DOCUMENT)
        AUDIO -> listOf(FileCategory.AUDIO)
    }
}

/** Counts and sizes shown before anything is downloaded. */
data class RestorePlan(
    val files: Int = 0,
    val bytes: Long = 0,
    val missingFiles: Int = 0,
    val missingBytes: Long = 0
) {
    val nothingMissing: Boolean get() = files > 0 && missingFiles == 0
    val empty: Boolean get() = files == 0
}

/**
 * Restores files from the Telegram backup to this device.
 *
 * Only files that exist in the backup are considered, and with [onlyMissing] a file already on the
 * phone is skipped instead of downloaded again — the "no duplicate downloads" rule applied while
 * queueing, not after the fact.
 */
class RestoreFilesUseCase(
    private val fileRepository: FileRepository,
    private val transferRepository: TransferRepository
) {

    suspend fun plan(scope: RestoreScope): RestorePlan {
        val files = load(scope)
        val missing = files.filter { !it.hasLocalCopy }
        return RestorePlan(
            files = files.size,
            bytes = files.sumOf { it.sizeBytes },
            missingFiles = missing.size,
            missingBytes = missing.sumOf { it.sizeBytes }
        )
    }

    /** Queues the restore and returns how many downloads were accepted. */
    suspend fun restore(scope: RestoreScope, onlyMissing: Boolean): AppResult<Int> {
        val targets = load(scope).filter { !onlyMissing || !it.hasLocalCopy }
        if (targets.isEmpty()) return AppResult.Success(0)

        var queued = 0
        var firstError: AppError? = null
        targets.forEach { file ->
            when (val result = transferRepository.enqueueDownload(file.id)) {
                is AppResult.Success -> queued++
                is AppResult.Failure -> if (firstError == null) firstError = result.error
            }
        }
        return when {
            queued > 0 -> AppResult.Success(queued)
            firstError != null -> AppResult.Failure(firstError!!)
            else -> AppResult.Success(0)
        }
    }

    /**
     * Files that really live in Telegram: a row without a remote copy cannot be restored.
     * Ids are read in chunks because a single IN (…) with tens of thousands of values hits
     * SQLite's variable limit.
     */
    private suspend fun load(scope: RestoreScope): List<DriveFile> {
        val ids = fileRepository.fileIds(
            FileQuerySpec(
                categories = scope.categories(),
                backedUpOnly = true,
                showHidden = true,
                showArchived = true
            )
        )
        val files = ArrayList<DriveFile>(ids.size)
        ids.chunked(ID_CHUNK).forEach { chunk ->
            files += fileRepository.getFiles(chunk).filter { it.remoteFileId != null }
        }
        return files
    }

    private companion object {
        const val ID_CHUNK = 500
    }
}
