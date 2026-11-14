package com.adnan.memoracloud.core.people

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.adnan.memoracloud.core.common.SafeLog
import com.adnan.memoracloud.domain.repository.PeopleRepository
import com.adnan.memoracloud.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.first

/**
 * Scans local photos for faces in small batches, then groups them. Runs only while the user has
 * People switched on, stops when nothing is left to scan, and records photos without faces so they
 * are not decoded again on every run.
 */
class FaceScanWorker(
    appContext: Context,
    params: WorkerParameters,
    private val settingsRepository: SettingsRepository,
    private val peopleRepository: PeopleRepository,
    private val analyzer: FaceAnalyzer
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        if (!settingsRepository.preferences.first().faceGroupingEnabled) {
            SafeLog.d(TAG, "People is switched off")
            return Result.success()
        }

        // Crops of photos that are gone would otherwise stay in app storage forever.
        runCatching { pruneCrops() }

        var scanned = 0
        var faces = 0
        while (scanned < MAX_PER_RUN) {
            val batch = peopleRepository.nextUnscanned(BATCH_SIZE)
            if (batch.isEmpty()) break
            for ((fileId, path) in batch) {
                runCatching { analyzer.analyze(path) }
                    .onSuccess { samples ->
                        faces += samples.size
                        peopleRepository.record(fileId, samples)
                    }
                    .onFailure { error ->
                        // A broken file still counts as looked at, otherwise it blocks the queue.
                        SafeLog.w(TAG, "Scan failed for $fileId", error)
                        peopleRepository.record(fileId, emptyList())
                    }
                scanned++
            }
        }

        SafeLog.d(TAG, "Scanned $scanned photo(s), found $faces face(s)")
        return Result.success()
    }

    private suspend fun pruneCrops() {
        val keep = peopleRepository.knownCropPaths()
        analyzer.pruneCrops(keep.toSet())
    }

    companion object {
        const val UNIQUE_NAME = "face-scan"
        private const val TAG = "FaceScanWorker"
        private const val BATCH_SIZE = 8
        private const val MAX_PER_RUN = 40
    }
}
