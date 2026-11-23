package com.adnan.memoracloud.desktop.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.adnan.memoracloud.core.crypto.CredentialCipher
import com.adnan.memoracloud.core.crypto.FileWrappedKeyRepository
import com.adnan.memoracloud.core.crypto.KeyBackupCodec
import com.adnan.memoracloud.core.crypto.PassphraseKdf
import com.adnan.memoracloud.core.crypto.SecureFileDeleter
import com.adnan.memoracloud.core.crypto.StreamCrypto
import com.adnan.memoracloud.core.crypto.TdlibDatabaseKeyProviderImpl
import com.adnan.memoracloud.core.crypto.WrappedKeyRepository
import com.adnan.memoracloud.core.dispatchers.DefaultDispatcherProvider
import com.adnan.memoracloud.core.dispatchers.DispatcherProvider
import com.adnan.memoracloud.core.files.AppStoragePaths
import com.adnan.memoracloud.core.files.DownloadWriter
import com.adnan.memoracloud.core.files.FileImporter
import com.adnan.memoracloud.core.files.LocalCopyDeleter
import com.adnan.memoracloud.core.files.PendingShare
import com.adnan.memoracloud.core.files.sharedFilesModule
import com.adnan.memoracloud.core.logging.debugLogModule
import com.adnan.memoracloud.core.media.MediaMetadataExtractor
import com.adnan.memoracloud.core.media.ThumbnailMemoryCache
import com.adnan.memoracloud.core.media.ThumbnailStore
import com.adnan.memoracloud.core.network.NetworkMonitor
import com.adnan.memoracloud.core.permissions.PermissionChecker
import com.adnan.memoracloud.core.power.PowerMonitor
import com.adnan.memoracloud.core.proxy.ProxyProbe
import com.adnan.memoracloud.core.publish.PublishOutboxDrainer
import com.adnan.memoracloud.core.publish.PublishScheduler
import com.adnan.memoracloud.core.security.AppLockManager
import com.adnan.memoracloud.core.telegram.DesktopTelegramClient
import com.adnan.memoracloud.core.telegram.TdlibDatabaseKeyProvider
import com.adnan.memoracloud.core.telegram.TelegramClient
import com.adnan.memoracloud.core.telegram.TelegramPacer
import com.adnan.memoracloud.core.transfer.BackupResumeScheduler
import com.adnan.memoracloud.core.transfer.BackupSessionTracker
import com.adnan.memoracloud.core.transfer.CountingBackupSessionTracker
import com.adnan.memoracloud.core.transfer.MaintenanceScheduler
import com.adnan.memoracloud.core.transfer.TransferErrorMessages
import com.adnan.memoracloud.core.transfer.TransferScheduler
import com.adnan.memoracloud.core.transfer.transferEngineModule
import com.adnan.memoracloud.core.update.UpdateChecker
import com.adnan.memoracloud.data.local.database.MIGRATION_10_11
import com.adnan.memoracloud.data.local.database.MIGRATION_11_12
import com.adnan.memoracloud.data.local.database.MIGRATION_12_13
import com.adnan.memoracloud.data.local.database.MIGRATION_13_14
import com.adnan.memoracloud.data.local.database.MIGRATION_14_15
import com.adnan.memoracloud.data.local.database.MIGRATION_15_16
import com.adnan.memoracloud.data.local.database.MIGRATION_16_17
import com.adnan.memoracloud.data.local.database.MIGRATION_1_2
import com.adnan.memoracloud.data.local.database.MIGRATION_2_3
import com.adnan.memoracloud.data.local.database.MIGRATION_3_4
import com.adnan.memoracloud.data.local.database.MIGRATION_4_5
import com.adnan.memoracloud.data.local.database.MIGRATION_5_6
import com.adnan.memoracloud.data.local.database.MIGRATION_6_7
import com.adnan.memoracloud.data.local.database.MIGRATION_7_8
import com.adnan.memoracloud.data.local.database.MIGRATION_8_9
import com.adnan.memoracloud.data.local.database.MIGRATION_9_10
import com.adnan.memoracloud.data.local.database.MemoraCloudDatabase
import com.adnan.memoracloud.data.local.database.daosModule
import com.adnan.memoracloud.data.repository.LocalDataWiper
import com.adnan.memoracloud.data.repository.repositoryModule
import com.adnan.memoracloud.desktop.BuildInfo
import com.adnan.memoracloud.desktop.DesktopPlatformCapabilities
import com.adnan.memoracloud.desktop.crypto.DpapiCredentialCipher
import com.adnan.memoracloud.desktop.crypto.LocalKeyCredentialCipher
import com.adnan.memoracloud.desktop.data.DesktopLocalDataWiper
import com.adnan.memoracloud.desktop.files.DesktopDownloadWriter
import com.adnan.memoracloud.desktop.files.DesktopFileImporter
import com.adnan.memoracloud.desktop.files.DesktopLocalCopyDeleter
import com.adnan.memoracloud.desktop.files.DesktopPendingShare
import com.adnan.memoracloud.desktop.files.DesktopStandardFolderPaths
import com.adnan.memoracloud.desktop.files.DesktopStoragePaths
import com.adnan.memoracloud.desktop.media.DesktopMediaMetadataExtractor
import com.adnan.memoracloud.desktop.media.DesktopThumbnailStore
import com.adnan.memoracloud.desktop.media.ExternalMediaPlayer
import com.adnan.memoracloud.desktop.media.MediaStreamServer
import com.adnan.memoracloud.desktop.media.NoopThumbnailMemoryCache
import com.adnan.memoracloud.desktop.network.DesktopNetworkMonitor
import com.adnan.memoracloud.desktop.permissions.DesktopPermissionChecker
import com.adnan.memoracloud.desktop.power.DesktopPowerMonitor
import com.adnan.memoracloud.desktop.publish.DesktopPublishScheduler
import com.adnan.memoracloud.desktop.transfer.DesktopBackupResumeScheduler
import com.adnan.memoracloud.desktop.transfer.DesktopMaintenanceScheduler
import com.adnan.memoracloud.desktop.transfer.DesktopTransferErrorMessages
import com.adnan.memoracloud.desktop.transfer.DesktopTransferScheduler
import com.adnan.memoracloud.domain.usecase.useCaseModule
import com.adnan.memoracloud.presentation.di.sharedUiModule
import com.adnan.memoracloud.presentation.platform.PlatformCapabilities
import com.adnan.memoracloud.presentation.platform.StandardFolderPaths
import java.io.File
import kotlinx.coroutines.Dispatchers
import okio.Path.Companion.toOkioPath
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

val desktopModule = module {
    includes(
        daosModule,
        module {
            single<com.adnan.memoracloud.core.files.DeviceFavorites> {
                com.adnan.memoracloud.core.files.DeviceFavorites.None
            }
        },
        repositoryModule,
        useCaseModule,
        sharedFilesModule,
        debugLogModule,
        transferEngineModule,
        sharedUiModule
    )

    singleOf(::DesktopStoragePaths) bind AppStoragePaths::class
    single<CredentialCipher> {
        val os = System.getProperty("os.name").orEmpty().lowercase()
        if (os.contains("win")) DpapiCredentialCipher() else LocalKeyCredentialCipher(get())
    }
    singleOf(::FileWrappedKeyRepository) bind WrappedKeyRepository::class
    singleOf(::TdlibDatabaseKeyProviderImpl) bind TdlibDatabaseKeyProvider::class
    singleOf(::DefaultDispatcherProvider) bind DispatcherProvider::class
    singleOf(::StreamCrypto)
    singleOf(::SecureFileDeleter)
    singleOf(::PassphraseKdf)
    singleOf(::KeyBackupCodec)
    singleOf(::TelegramPacer)
    singleOf(::ProxyProbe)
    singleOf(::AppLockManager)
    single { UpdateChecker(BuildInfo.VERSION) }
    singleOf(::DesktopPermissionChecker) bind PermissionChecker::class
    singleOf(::DesktopStandardFolderPaths) bind StandardFolderPaths::class
    singleOf(::DesktopPlatformCapabilities) bind PlatformCapabilities::class
    singleOf(::PublishOutboxDrainer)
    singleOf(::DesktopNetworkMonitor) bind NetworkMonitor::class
    singleOf(::DesktopPowerMonitor) bind PowerMonitor::class
    singleOf(::DesktopTransferErrorMessages) bind TransferErrorMessages::class
    singleOf(::DesktopDownloadWriter) bind DownloadWriter::class
    singleOf(::MediaStreamServer)
    singleOf(::ExternalMediaPlayer)
    singleOf(::DesktopFileImporter) bind FileImporter::class
    singleOf(::DesktopPendingShare) bind PendingShare::class
    singleOf(::DesktopLocalCopyDeleter) bind LocalCopyDeleter::class
    singleOf(::DesktopMediaMetadataExtractor) bind MediaMetadataExtractor::class
    singleOf(::DesktopThumbnailStore) bind ThumbnailStore::class
    singleOf(::NoopThumbnailMemoryCache) bind ThumbnailMemoryCache::class
    singleOf(::CountingBackupSessionTracker) bind BackupSessionTracker::class
    singleOf(::DesktopTransferScheduler) bind TransferScheduler::class
    singleOf(::DesktopBackupResumeScheduler) bind BackupResumeScheduler::class
    singleOf(::DesktopMaintenanceScheduler) bind MaintenanceScheduler::class
    singleOf(::DesktopLocalDataWiper) bind LocalDataWiper::class
    singleOf(::DesktopPublishScheduler) bind PublishScheduler::class
    single<TelegramClient> {
        DesktopTelegramClient(get(), get(), get(), BuildInfo.VERSION)
    }
    single<DataStore<Preferences>> {
        val storagePaths = get<AppStoragePaths>()
        PreferenceDataStoreFactory.createWithPath {
            File(File(storagePaths.filesDir, "datastore"), "settings.preferences_pb")
                .apply { parentFile?.mkdirs() }
                .toOkioPath()
        }
    }
    single<MemoraCloudDatabase> {
        val storagePaths = get<AppStoragePaths>()
        Room.databaseBuilder<MemoraCloudDatabase>(
            name = File(storagePaths.filesDir, "memoracloud.db").absolutePath
        )
            .addMigrations(
                MIGRATION_1_2,
                MIGRATION_2_3,
                MIGRATION_3_4,
                MIGRATION_4_5,
                MIGRATION_5_6,
                MIGRATION_6_7,
                MIGRATION_7_8,
                MIGRATION_8_9,
                MIGRATION_9_10,
                MIGRATION_10_11,
                MIGRATION_11_12,
                MIGRATION_12_13,
                MIGRATION_13_14,
                MIGRATION_14_15,
                MIGRATION_15_16,
                MIGRATION_16_17
            )
            .setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(Dispatchers.IO)
            .build()
    }
}
