package com.adnan.memoracloud.presentation.onboarding

import com.adnan.memoracloud.presentation.common.UiText

import com.adnan.memoracloud.core.telegram.CodeDeliveryChannel
import com.adnan.memoracloud.domain.model.Country
import com.adnan.memoracloud.domain.model.DriveChannel

data class OnboardingUiState(
    val step: OnboardingStep = OnboardingStep.WELCOME,
    val working: Boolean = false,
    val error: UiText? = null,
    val codePhoneNumber: String = "",
    val codeChannel: CodeDeliveryChannel = CodeDeliveryChannel.TELEGRAM_APP,
    val codeLength: Int? = null,
    val passwordHint: String? = null,
    val registrationRequired: Boolean = false,
    val qrLink: String? = null,
    val qrMode: Boolean = false,
    val countries: List<Country> = emptyList(),
    val countryLoadState: CountryLoadState = CountryLoadState.LOADING,
    val selectedCountry: Country? = null,
    val channels: List<DriveChannel> = emptyList(),
    val selectedChatId: Long? = null,
    val channelCreated: Boolean = false,
    val backupDcim: Boolean = true,
    val backupPictures: Boolean = true,
    val backupMovies: Boolean = false,
    val autoBackupEnabled: Boolean = true,
    val wifiOnly: Boolean = false,
    val finishing: Boolean = false
)
