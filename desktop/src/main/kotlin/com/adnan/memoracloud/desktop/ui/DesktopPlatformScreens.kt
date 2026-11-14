package com.adnan.memoracloud.desktop.ui

import androidx.compose.runtime.Composable
import com.adnan.memoracloud.presentation.platform.PlatformScreens

object DesktopPlatformScreens : PlatformScreens {

    @Composable
    override fun Preview(onBack: () -> Unit, onEditNote: (String, String) -> Unit) {
        DesktopPreviewScreen(onBack = onBack)
    }
}
