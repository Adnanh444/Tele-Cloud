package com.adnan.memoracloud.data.local.database

import org.koin.dsl.module

val daosModule = module {
    single { get<MemoraCloudDatabase>().storageChannelDao() }
    single { get<MemoraCloudDatabase>().fileDao() }
    single { get<MemoraCloudDatabase>().folderDao() }
    single { get<MemoraCloudDatabase>().transferDao() }
    single { get<MemoraCloudDatabase>().backupDao() }
    single { get<MemoraCloudDatabase>().exclusionDao() }
    single { get<MemoraCloudDatabase>().thumbnailDao() }
    single { get<MemoraCloudDatabase>().cacheDao() }
    single { get<MemoraCloudDatabase>().pendingDeleteDao() }
    single { get<MemoraCloudDatabase>().filePartDao() }
    single { get<MemoraCloudDatabase>().proxyDao() }
    single { get<MemoraCloudDatabase>().folderTombstoneDao() }
    single { get<MemoraCloudDatabase>().photoLabelDao() }
}
