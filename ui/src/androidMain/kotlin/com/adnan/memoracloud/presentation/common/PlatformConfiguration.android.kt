package com.adnan.memoracloud.presentation.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration

@Composable
actual fun platformConfiguration(): Any? = LocalConfiguration.current
