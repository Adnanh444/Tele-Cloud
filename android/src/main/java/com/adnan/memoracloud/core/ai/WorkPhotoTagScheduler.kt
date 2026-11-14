package com.adnan.memoracloud.core.ai

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager

class WorkPhotoTagScheduler(
    private val context: Context
) : PhotoTagScheduler {

    override fun enqueue() {
        WorkManager.getInstance(context).enqueueUniqueWork(
            PhotoLabelWorker.UNIQUE_NAME,
            ExistingWorkPolicy.KEEP,
            OneTimeWorkRequestBuilder<PhotoLabelWorker>()
                .setConstraints(
                    Constraints.Builder()
                        .setRequiresBatteryNotLow(true)
                        .build()
                )
                .build()
        )
    }

    override fun cancel() {
        WorkManager.getInstance(context).cancelUniqueWork(PhotoLabelWorker.UNIQUE_NAME)
    }
}
