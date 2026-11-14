package com.adnan.memoracloud.desktop.media

import com.adnan.memoracloud.core.media.ThumbnailMemoryCache

class NoopThumbnailMemoryCache : ThumbnailMemoryCache {

    override fun remove(fileId: String) {
    }

    override fun clear() {
    }
}
