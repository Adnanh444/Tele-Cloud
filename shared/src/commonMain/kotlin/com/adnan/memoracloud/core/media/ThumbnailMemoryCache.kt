package com.adnan.memoracloud.core.media

interface ThumbnailMemoryCache {

    fun remove(fileId: String)

    fun clear()
}
