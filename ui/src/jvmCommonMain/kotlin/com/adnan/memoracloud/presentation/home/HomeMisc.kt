package com.adnan.memoracloud.presentation.home

import com.adnan.memoracloud.core.network.NetworkStatus
import com.adnan.memoracloud.core.permissions.AppPermission
import com.adnan.memoracloud.core.telegram.TelegramConnectionState
import com.adnan.memoracloud.domain.model.BackupHold
import com.adnan.memoracloud.domain.model.BackupSession
import com.adnan.memoracloud.domain.model.DriveChannel

internal data class HomeMisc(
    val syncing: Boolean,
    val session: BackupSession?,
    val connection: TelegramConnectionState,
    val network: NetworkStatus,
    val missing: List<AppPermission>,
    val activeDrive: DriveChannel?,
    val autoBackupEnabled: Boolean,
    val appLockEnabled: Boolean,
    val showArchivedSection: Boolean,
    val showHiddenSection: Boolean,
    val showRecentSection: Boolean,
    val backupHold: BackupHold?
)
