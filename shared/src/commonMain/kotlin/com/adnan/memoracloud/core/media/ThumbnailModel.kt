package com.adnan.memoracloud.core.media

data class ThumbnailModel(val fileId: String)

fun thumbnailCacheKey(fileId: String): String = "thumb:$fileId"
