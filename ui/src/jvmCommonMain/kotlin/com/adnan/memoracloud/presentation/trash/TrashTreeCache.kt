package com.adnan.memoracloud.presentation.trash

import com.adnan.memoracloud.domain.model.TrashItem

internal data class TrashTreeCache(
    val children: Map<String, List<TrashItem>> = emptyMap(),
    val counts: Map<String, Int> = emptyMap()
)
