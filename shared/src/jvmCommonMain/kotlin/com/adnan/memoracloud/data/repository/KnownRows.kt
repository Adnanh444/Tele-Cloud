package com.adnan.memoracloud.data.repository

import com.adnan.memoracloud.core.telegram.RemoteDocument
import com.adnan.memoracloud.data.local.entity.FileEntity
import com.adnan.memoracloud.data.remote.telegram.RemoteFileManifest

internal data class KnownRows(
    val manifests: Map<RemoteDocument, RemoteFileManifest?>,
    val byId: Map<String, FileEntity>,
    val byUniqueId: Map<String, FileEntity>,
    val splitCounts: Map<String, Int>,
    val firstPartStored: Set<String>
)
