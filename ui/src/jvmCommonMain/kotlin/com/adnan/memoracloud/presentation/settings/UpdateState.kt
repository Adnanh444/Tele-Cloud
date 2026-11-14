package com.adnan.memoracloud.presentation.settings

import com.adnan.memoracloud.core.update.AppRelease

sealed interface UpdateState {
    data object Idle : UpdateState
    data object Checking : UpdateState
    data class Available(val release: AppRelease) : UpdateState
}
