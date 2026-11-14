package com.adnan.memoracloud.data.mapper

import com.adnan.memoracloud.data.local.entity.AlbumSummary
import com.adnan.memoracloud.domain.model.MediaAlbum

fun AlbumSummary.toDomain(): MediaAlbum = MediaAlbum(
    folderId = folderId,
    name = name,
    itemCount = itemCount,
    coverFileId = coverFileId,
    latestAt = latestAt
)
