package com.adnan.memoracloud.core.ai

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters

/**
 * Deliberately empty. 1.0.6 queued this worker for an experimental photo tagger and then the feature
 * was removed, but WorkManager still had the job in its database and re-created it **by class name**
 * on the next launch. A missing class there kills the process before the app can do anything.
 *
 * The class therefore stays, in its original package and under its original name, doing nothing.
 * It also needs the `-keepnames` rule in `proguard-rules.pro`, otherwise the class name stored in
 * WorkManager's database no longer matches.
 */
class PhotoLabelWorker(
    appContext: Context,
    params: WorkerParameters
) : Worker(appContext, params) {
    override fun doWork(): Result = Result.success()
}
