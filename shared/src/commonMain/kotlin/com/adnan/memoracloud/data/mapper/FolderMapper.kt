package com.adnan.memoracloud.data.mapper

import com.adnan.memoracloud.data.local.dao.FolderWithCount
import com.adnan.memoracloud.data.local.entity.FolderEntity
import com.adnan.memoracloud.domain.model.DriveFolder

fun FolderEntity.toDomain(): DriveFolder = DriveFolder(
    id = id,
    parentId = parentId,
    name = name,
    isHidden = isHidden,
    isArchived = isArchived,
    isFavorite = isFavorite,
    isAvailableOffline = isAvailableOffline,
    trashedAt = trashedAt,
    createdAt = createdAt,
    modifiedAt = modifiedAt
)

fun FolderWithCount.toDomain(): DriveFolder = folder.toDomain().copy(
    fileCount = fileCount,
    folderCount = folderCount
)
