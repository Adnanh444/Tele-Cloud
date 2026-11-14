package com.adnan.memoracloud.core.update

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.adnan.memoracloud.core.common.SafeLog

/**
 * Downloads the announced APK in the background so the work survives the user leaving the app.
 * All state lives in [ApkUpdateInstaller], which the UI observes.
 */
class UpdateDownloadWorker(
    appContext: Context,
    params: WorkerParameters,
    private val installer: ApkUpdateInstaller
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val url = inputData.getString(KEY_URL)
        if (url.isNullOrBlank()) return Result.failure()
        val sha = inputData.getString(KEY_SHA)?.takeIf { it.isNotBlank() }
        val version = inputData.getString(KEY_VERSION).orEmpty()

        val retryable = installer.runDownload(url, sha, version)
        SafeLog.d(TAG, "Update download finished (retryable=$retryable)")
        return if (retryable && runAttemptCount < MAX_RETRIES) Result.retry() else Result.success()
    }

    companion object {
        const val KEY_URL = "url"
        const val KEY_SHA = "sha"
        const val KEY_VERSION = "version"
        const val KEY_TOTAL = "total"
        private const val TAG = "UpdateDownloadWorker"
        private const val MAX_RETRIES = 3
    }
}
