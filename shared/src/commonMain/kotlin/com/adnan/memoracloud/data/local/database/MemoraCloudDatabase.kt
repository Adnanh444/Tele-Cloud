package com.adnan.memoracloud.data.local.database

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import com.adnan.memoracloud.data.local.dao.BackupDao
import com.adnan.memoracloud.data.local.dao.CacheDao
import com.adnan.memoracloud.data.local.dao.ExclusionDao
import com.adnan.memoracloud.data.local.dao.FileDao
import com.adnan.memoracloud.data.local.dao.FilePartDao
import com.adnan.memoracloud.data.local.dao.ProxyDao
import com.adnan.memoracloud.data.local.dao.FolderDao
import com.adnan.memoracloud.data.local.dao.FolderTombstoneDao
import com.adnan.memoracloud.data.local.dao.PendingDeleteDao
import com.adnan.memoracloud.data.local.dao.StorageChannelDao
import com.adnan.memoracloud.data.local.dao.ThumbnailDao
import com.adnan.memoracloud.data.local.dao.TransferDao
import com.adnan.memoracloud.data.local.entity.BackupRecordEntity
import com.adnan.memoracloud.data.local.entity.BackupSessionEntity
import com.adnan.memoracloud.data.local.entity.CacheEntryEntity
import com.adnan.memoracloud.data.local.entity.ExclusionEntity
import com.adnan.memoracloud.data.local.entity.FileEntity
import com.adnan.memoracloud.data.local.entity.FilePartEntity
import com.adnan.memoracloud.data.local.entity.ProxyEntity
import com.adnan.memoracloud.data.local.entity.FolderEntity
import com.adnan.memoracloud.data.local.entity.FolderTombstoneEntity
import com.adnan.memoracloud.data.local.entity.PendingDeleteEntity
import com.adnan.memoracloud.data.local.entity.StorageChannelEntity
import com.adnan.memoracloud.data.local.entity.ThumbnailEntity
import com.adnan.memoracloud.data.local.entity.TransferEntity

@Database(
    entities = [
        FileEntity::class,
        FolderEntity::class,
        TransferEntity::class,
        BackupSessionEntity::class,
        BackupRecordEntity::class,
        ExclusionEntity::class,
        ThumbnailEntity::class,
        CacheEntryEntity::class,
        StorageChannelEntity::class,
        PendingDeleteEntity::class,
        FilePartEntity::class,
        ProxyEntity::class,
        FolderTombstoneEntity::class
    ],
    version = 19,
    exportSchema = true
)
@ConstructedBy(MemoraCloudDatabaseConstructor::class)
abstract class MemoraCloudDatabase : RoomDatabase() {
    abstract fun fileDao(): FileDao
    abstract fun folderDao(): FolderDao
    abstract fun transferDao(): TransferDao
    abstract fun backupDao(): BackupDao
    abstract fun exclusionDao(): ExclusionDao
    abstract fun thumbnailDao(): ThumbnailDao
    abstract fun cacheDao(): CacheDao
    abstract fun storageChannelDao(): StorageChannelDao
    abstract fun pendingDeleteDao(): PendingDeleteDao
    abstract fun filePartDao(): FilePartDao
    abstract fun proxyDao(): ProxyDao
    abstract fun folderTombstoneDao(): FolderTombstoneDao

    companion object {
        const val NAME = "memoracloud.db"
    }
}
