package com.adnan.memoracloud.data.repository

import com.adnan.memoracloud.domain.model.UserPreferences
import com.adnan.memoracloud.domain.repository.SettingsRepository
import com.adnan.memoracloud.testing.unused
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

internal class FakeSettings(initial: UserPreferences) : SettingsRepository by unused() {
    private val state = MutableStateFlow(initial)
    override val preferences: Flow<UserPreferences> = state

    override suspend fun update(transform: (UserPreferences) -> UserPreferences) {
        state.value = transform(state.value)
    }
}
