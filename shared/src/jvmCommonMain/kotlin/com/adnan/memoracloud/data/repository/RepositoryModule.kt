package com.adnan.memoracloud.data.repository

import com.adnan.memoracloud.core.telegram.TelegramSession
import com.adnan.memoracloud.core.transfer.SidecarCleanup
import com.adnan.memoracloud.data.remote.telegram.ManifestCodec
import com.adnan.memoracloud.domain.repository.BackupRepository
import com.adnan.memoracloud.domain.repository.CacheRepository
import com.adnan.memoracloud.domain.repository.ChannelRepository
import com.adnan.memoracloud.domain.repository.ExclusionRepository
import com.adnan.memoracloud.domain.repository.FileRepository
import com.adnan.memoracloud.domain.repository.KeyBackupRepository
import com.adnan.memoracloud.domain.repository.ProxyRepository
import com.adnan.memoracloud.domain.repository.SettingsRepository
import com.adnan.memoracloud.domain.repository.SyncRepository
import com.adnan.memoracloud.domain.repository.TelegramAuthRepository
import com.adnan.memoracloud.domain.repository.TransferRepository
import com.adnan.memoracloud.domain.repository.TrashRepository
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

val repositoryModule = module {
    singleOf(::ManifestCodec)
    singleOf(::SidecarCleanup)
    singleOf(::ActiveChannel)
    singleOf(::ChannelOwnership)
    singleOf(::FolderOwnershipRepair)
    singleOf(::FileManifestPublisher)
    singleOf(::FolderPathResolver)
    singleOf(::FolderMerger)
    singleOf(::IndexSnapshot)
    singleOf(::FolderStateSynchronizer)
    singleOf(::SettingsRepositoryImpl) bind SettingsRepository::class
    singleOf(::FileRepositoryImpl) bind FileRepository::class
    singleOf(::TrashRepositoryImpl) bind TrashRepository::class
    singleOf(::TransferRepositoryImpl) bind TransferRepository::class
    singleOf(::BackupRepositoryImpl) bind BackupRepository::class
    singleOf(::ExclusionRepositoryImpl) bind ExclusionRepository::class
    singleOf(::TelegramAuthRepositoryImpl) bind TelegramAuthRepository::class
    singleOf(::TelegramSession)
    singleOf(::ProxyRepositoryImpl) bind ProxyRepository::class
    singleOf(::SyncRepositoryImpl) bind SyncRepository::class
    singleOf(::ChannelRepositoryImpl) bind ChannelRepository::class
    singleOf(::CacheRepositoryImpl) bind CacheRepository::class
    singleOf(::KeyBackupRepositoryImpl) bind KeyBackupRepository::class
}
