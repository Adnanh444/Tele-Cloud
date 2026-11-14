package com.adnan.memoracloud.core.people

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters

/** Empty counterpart of [com.adnan.memoracloud.core.ai.PhotoLabelWorker] for the unpublished 1.0.7. */
class FaceScanWorker(
    appContext: Context,
    params: WorkerParameters
) : Worker(appContext, params) {
    override fun doWork(): Result = Result.success()
}
