package com.adnan.memoracloud.presentation.preview

import com.adnan.memoracloud.domain.model.LinkMetadata

internal sealed interface LinkState {
    data object Loading : LinkState
    data object Bare : LinkState
    data class Article(val metadata: LinkMetadata) : LinkState
}
