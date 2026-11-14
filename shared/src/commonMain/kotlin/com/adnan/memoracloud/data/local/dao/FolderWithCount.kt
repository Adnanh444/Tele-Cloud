package com.adnan.memoracloud.data.local.dao

import androidx.room.Embedded
import com.adnan.memoracloud.data.local.entity.FolderEntity

data class FolderWithCount(
    @Embedded val folder: FolderEntity,
    val fileCount: Int,
    val folderCount: Int
)
