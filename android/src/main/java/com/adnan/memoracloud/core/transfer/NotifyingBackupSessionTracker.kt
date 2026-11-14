package com.adnan.memoracloud.core.transfer

import android.content.Context
import com.adnan.memoracloud.R
import com.adnan.memoracloud.core.common.AppNotifications
import com.adnan.memoracloud.data.local.dao.BackupDao
import com.adnan.memoracloud.data.local.dao.TransferDao
import com.adnan.memoracloud.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.first

class NotifyingBackupSessionTracker(
    transferDao: TransferDao,
    backupDao: BackupDao,
    private val settingsRepository: SettingsRepository,
    private val appNotifications: AppNotifications,
    private val context: Context
) : CountingBackupSessionTracker(transferDao, backupDao) {

    override suspend fun onSessionSettled(counts: BackupSessionCounts) {
        if (!settingsRepository.preferences.first().backupNotifications) return
        appNotifications.notifyBackupResult(
            title = context.getString(R.string.notification_backup_complete),
            message = context.getString(
                R.string.notification_backup_summary,
                counts.completedFiles,
                counts.totalFiles
            )
        )
    }
}
