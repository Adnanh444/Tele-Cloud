package com.adnan.memoracloud

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import com.adnan.memoracloud.core.people.FaceScanScheduler
import com.adnan.memoracloud.core.common.AppNotifications
import com.adnan.memoracloud.core.common.SafeLog
import com.adnan.memoracloud.core.logging.DebugLogController
import com.adnan.memoracloud.core.proxy.ProxyFailover
import com.adnan.memoracloud.core.publish.PublishScheduler
import com.adnan.memoracloud.core.transfer.MaintenanceScheduler
import com.adnan.memoracloud.core.transfer.MediaStoreWatcher
import com.adnan.memoracloud.di.appModules
import com.adnan.memoracloud.data.repository.FolderOwnershipRepair
import com.adnan.memoracloud.domain.repository.FileRepository
import com.adnan.memoracloud.domain.repository.SettingsRepository
import com.adnan.memoracloud.domain.repository.TransferRepository
import com.adnan.memoracloud.domain.repository.TrashRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.workmanager.koin.workManagerFactory
import org.koin.core.context.startKoin

class MemoraCloudApplication : Application(), SingletonImageLoader.Factory {

    private val imageLoader: ImageLoader by inject()

    private val appNotifications: AppNotifications by inject()

    private val maintenanceScheduler: MaintenanceScheduler by inject()

    private val settingsRepository: SettingsRepository by inject()

    private val transferRepository: TransferRepository by inject()

    private val fileRepository: FileRepository by inject()

    private val folderOwnershipRepair: FolderOwnershipRepair by inject()

    private val trashRepository: TrashRepository by inject()

    private val mediaStoreWatcher: MediaStoreWatcher by inject()

    private val publishScheduler: PublishScheduler by inject()

    private val proxyFailover: ProxyFailover by inject()

    private val faceScanScheduler: FaceScanScheduler by inject()

    private val debugLogController: DebugLogController by inject()

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun newImageLoader(context: PlatformContext): ImageLoader = imageLoader

    override fun onCreate() {
        super.onCreate()
        SafeLog.verbose = BuildConfig.DEBUG
        startKoin {
            androidContext(this@MemoraCloudApplication)
            workManagerFactory()
            modules(appModules)
        }
        appNotifications.createChannels()
        mediaStoreWatcher.start()
        proxyFailover.start(applicationScope)
        debugLogController.start(applicationScope, alwaysVerbose = BuildConfig.DEBUG)
        applicationScope.launch {
            transferRepository.recoverOrphanedTransfers()
            fileRepository.sweepImportOrphans()
            folderOwnershipRepair.runOnce()
            folderOwnershipRepair.refileStrayFiles()
            publishScheduler.kick()
            trashRepository.repairTrashTree()
            val prefs = settingsRepository.preferences.first()
            if (prefs.faceGroupingEnabled) faceScanScheduler.enqueue()
            maintenanceScheduler.scheduleAll(
                backupEnabled = prefs.autoBackupEnabled && prefs.backupIntervalHours > 0,
                backupIntervalHours = prefs.backupIntervalHours,
                wifiOnly = prefs.backupWifiOnly,
                chargingOnly = prefs.backupChargingOnly,
                instantBackup = prefs.instantBackupEnabled,
                updateChecks = prefs.updateCheckEnabled
            )
        }
    }
}
