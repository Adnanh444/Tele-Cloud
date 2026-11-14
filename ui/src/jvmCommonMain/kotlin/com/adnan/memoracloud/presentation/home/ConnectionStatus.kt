package com.adnan.memoracloud.presentation.home

import com.adnan.memoracloud.presentation.components.ConnectionIndicator
import org.jetbrains.compose.resources.StringResource

internal data class ConnectionStatus(
    val indicator: ConnectionIndicator,
    val labelRes: StringResource
)
