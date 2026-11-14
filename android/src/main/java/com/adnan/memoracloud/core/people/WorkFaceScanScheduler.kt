package com.adnan.memoracloud.core.people

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager

class WorkFaceScanScheduler(
    private val context: Context
) : FaceScanScheduler {

    override fun enqueue() {
        WorkManager.getInstance(context).enqueueUniqueWork(
            FaceScanWorker.UNIQUE_NAME,
            ExistingWorkPolicy.KEEP,
            OneTimeWorkRequestBuilder<FaceScanWorker>()
                .setConstraints(
                    Constraints.Builder()
                        .setRequiresBatteryNotLow(true)
                        .build()
                )
                .build()
        )
    }

    override fun cancel() {
        WorkManager.getInstance(context).cancelUniqueWork(FaceScanWorker.UNIQUE_NAME)
    }
}
