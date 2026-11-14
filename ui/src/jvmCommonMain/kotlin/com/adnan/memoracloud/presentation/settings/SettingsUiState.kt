package com.adnan.memoracloud.presentation.settings

import com.adnan.memoracloud.core.telegram.TelegramConnectionState
import com.adnan.memoracloud.core.telegram.TelegramUser
import com.adnan.memoracloud.domain.model.UserPreferences
import com.adnan.memoracloud.domain.repository.CacheStats

data class SettingsUiState(
    val preferences: UserPreferences = UserPreferences(),
    val user: TelegramUser? = null,
    val connection: TelegramConnectionState = TelegramConnectionState.CONNECTING,
    val cacheStats: CacheStats = CacheStats(0, 0, 0, 0, 0),
    val syncing: Boolean = false,
    val loading: Boolean = true
)
