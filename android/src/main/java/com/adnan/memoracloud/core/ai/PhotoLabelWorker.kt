package com.adnan.memoracloud.core.ai

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.adnan.memoracloud.core.common.SafeLog
import com.adnan.memoracloud.domain.ai.ImageLabeler
import com.adnan.memoracloud.domain.repository.PhotoIntelligenceRepository
import com.adnan.memoracloud.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.first

/**
 * Tags local photos in small batches. Runs only when the user has photo intelligence switched on,
 * stops when the queue is empty, and never retries the whole batch just because one file failed
 * (the failed file simply gets no tags and will not be picked up again).
 */
class PhotoLabelWorker(
    appContext: Context,
    params: WorkerParameters,
    private val settingsRepository: SettingsRepository,
    private val repository: PhotoIntelligenceRepository,
    private val labeler: ImageLabeler
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        if (!settingsRepository.preferences.first().aiLabelsEnabled) {
            SafeLog.d(TAG, "Photo intelligence is switched off")
            return Result.success()
        }

        var processed = 0
        while (processed < MAX_PER_RUN) {
            val batch = repository.nextBatch(BATCH_SIZE)
            if (batch.isEmpty()) break
            for ((fileId, path) in batch) {
                val labels = labeler.label(path)
                val confidence = labels.maxOfOrNull { it.confidence } ?: 0f
                // Saving even an empty result records "we looked at this one", so a photo the model
                // cannot read is not retried on every run.
                repository.save(fileId, labels, confidence)
                processed++
            }
        }

        SafeLog.d(TAG, "Tagged $processed photo(s)")
        return Result.success()
    }

    companion object {
        const val UNIQUE_NAME = "photo-labels"
        private const val TAG = "PhotoLabelWorker"
        private const val BATCH_SIZE = 12
        private const val MAX_PER_RUN = 60
    }
}
