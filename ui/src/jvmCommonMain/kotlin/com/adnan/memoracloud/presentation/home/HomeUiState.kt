package com.adnan.memoracloud.presentation.home

import com.adnan.memoracloud.core.permissions.AppPermission
import com.adnan.memoracloud.core.telegram.TelegramConnectionState
import com.adnan.memoracloud.domain.model.BackupHold
import com.adnan.memoracloud.domain.model.BackupSession
import com.adnan.memoracloud.domain.model.DriveChannel
import com.adnan.memoracloud.domain.model.DriveFile
import com.adnan.memoracloud.domain.model.DriveFolder
import com.adnan.memoracloud.domain.model.StorageSlice

data class HomeUiState(
    val loading: Boolean = true,
    val connection: TelegramConnectionState = TelegramConnectionState.CONNECTING,
    val offline: Boolean = false,
    val totalFiles: Int = 0,
    val remoteBytes: Long = 0,
    val backedUpCount: Int = 0,
    val pendingCount: Int = 0,
    val localOnlyCount: Int = 0,
    val failedCount: Int = 0,
    val recentFiles: List<DriveFile> = emptyList(),
    val favoriteFolders: List<DriveFolder> = emptyList(),
    val activeBackup: BackupSession? = null,
    val missingPermissions: List<AppPermission> = emptyList(),
    val backupFoldersSelected: Boolean = true,
    val showArchivedSection: Boolean = false,
    val showHiddenSection: Boolean = false,
    val showRecentSection: Boolean = true,
    val rebuilding: Boolean = false,
    val appLockEnabled: Boolean = false,
    val activeTransferCount: Int = 0,
    val offlineBytes: Long = 0,
    val backupHold: BackupHold? = null,
    val activeChannel: DriveChannel? = null,
    val storage: List<StorageSlice> = emptyList(),
    val autoBackupEnabled: Boolean = false,
    val lastBackupAt: Long? = null
)
