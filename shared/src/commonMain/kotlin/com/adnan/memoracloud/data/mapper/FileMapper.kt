package com.adnan.memoracloud.data.mapper

import com.adnan.memoracloud.data.local.entity.FileEntity
import com.adnan.memoracloud.domain.model.DriveFile

fun FileEntity.toDomain(): DriveFile = DriveFile(
    id = id,
    folderId = folderId,
    name = name,
    sizeBytes = sizeBytes,
    mimeType = mimeType,
    category = category,
    localPath = localPath,
    contentHash = contentHash,
    chatId = chatId,
    messageId = messageId,
    remoteFileId = remoteFileId,
    remoteUniqueId = remoteUniqueId,
    backupState = backupState,
    isHidden = isHidden,
    isArchived = isArchived,
    isFavorite = isFavorite,
    isAvailableOffline = isAvailableOffline,
    isEncrypted = isEncrypted,
    width = width,
    height = height,
    durationMs = durationMs,
    trashedAt = trashedAt,
    iconFileId = iconFileId,
    createdAt = createdAt,
    modifiedAt = modifiedAt,
    addedAt = addedAt
)

fun DriveFile.toEntity(preTrashFolderId: String? = null): FileEntity = FileEntity(
    id = id,
    folderId = folderId,
    name = name,
    sizeBytes = sizeBytes,
    mimeType = mimeType,
    category = category,
    localPath = localPath,
    contentHash = contentHash,
    chatId = chatId,
    messageId = messageId,
    remoteFileId = remoteFileId,
    remoteUniqueId = remoteUniqueId,
    backupState = backupState,
    isHidden = isHidden,
    isArchived = isArchived,
    isFavorite = isFavorite,
    isAvailableOffline = isAvailableOffline,
    isEncrypted = isEncrypted,
    width = width,
    height = height,
    durationMs = durationMs,
    trashedAt = trashedAt,
    preTrashFolderId = preTrashFolderId,
    iconFileId = iconFileId,
    createdAt = createdAt,
    modifiedAt = modifiedAt,
    addedAt = addedAt
)
