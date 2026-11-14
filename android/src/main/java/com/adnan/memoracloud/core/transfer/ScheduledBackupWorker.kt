package com.adnan.memoracloud.core.transfer

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.adnan.memoracloud.core.common.AppResult
import com.adnan.memoracloud.core.telegram.TelegramAuthState
import com.adnan.memoracloud.domain.model.BackupTrigger
import com.adnan.memoracloud.domain.repository.BackupRepository
import com.adnan.memoracloud.domain.repository.TelegramAuthRepository
import kotlinx.coroutines.flow.first

class ScheduledBackupWorker(
    appContext: Context,
    params: WorkerParameters,
    private val backupRepository: BackupRepository,
    private val telegramAuthRepository: TelegramAuthRepository
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val started = telegramAuthRepository.startFromStoredCredentials()
        if (started is AppResult.Failure || started.getOrNull() != true) return Result.retry()

        val state = telegramAuthRepository.authState.first { it != TelegramAuthState.Initializing }
        if (state != TelegramAuthState.Ready) return Result.retry()

        return when (backupRepository.startBackup(BackupTrigger.SCHEDULED)) {
            is AppResult.Success -> Result.success()
            is AppResult.Failure -> Result.retry()
        }
    }

    companion object {
        const val UNIQUE_NAME = "scheduled_backup"
    }
}
