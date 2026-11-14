package com.adnan.memoracloud.presentation.settings

import com.adnan.memoracloud.core.update.AppRelease

sealed interface UpdateState {
    data object Idle : UpdateState
    data object Checking : UpdateState
    data class Available(val release: AppRelease) : UpdateState

    /** Why there is nothing to install: up to date, or the check failed. Shown in the About row. */
    data class Message(val text: String) : UpdateState
}
