package com.adnan.memoracloud.presentation.components

import androidx.compose.runtime.Composable

internal data class GroupedListItem(
    val visible: Boolean,
    val content: @Composable () -> Unit
)
