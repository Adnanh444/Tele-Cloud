package com.adnan.memoracloud.presentation.home

import com.adnan.memoracloud.domain.model.DriveChannel
import com.adnan.memoracloud.domain.model.UserPreferences

internal data class HomeSettings(
    val prefs: UserPreferences,
    val syncing: Boolean,
    val activeDrive: DriveChannel?,
    val charging: Boolean
)
