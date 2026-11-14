package com.adnan.memoracloud.core.compat

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import org.koin.androidx.workmanager.dsl.workerOf
import org.koin.dsl.module

/**
 * 1.0.6 queued background work for an experimental photo tagger, and the unpublished 1.0.7 queued a
 * face scanner. Those features are gone, but WorkManager keeps its own database: on start-up it
 * re-creates every unfinished job and asks the factory for the worker class by name. A class that no
 * longer exists throws there — before any of the app's own code runs — which is what made 1.0.8
 * crash on launch for anyone updating from 1.0.6.
 *
 * These stubs keep the old class names alive so the leftover jobs run once, do nothing, and are
 * pruned. They can be deleted several versions later, once no device can still hold such a job.
 */
class PhotoLabelWorker(
    appContext: Context,
    params: WorkerParameters
) : Worker(appContext, params) {
    override fun doWork(): Result = Result.success()
}

class FaceScanWorker(
    appContext: Context,
    params: WorkerParameters
) : Worker(appContext, params) {
    override fun doWork(): Result = Result.success()
}

val legacyWorkersModule = module {
    workerOf(::PhotoLabelWorker)
    workerOf(::FaceScanWorker)
}
