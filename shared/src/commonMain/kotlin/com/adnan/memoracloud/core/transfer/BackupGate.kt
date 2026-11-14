package com.adnan.memoracloud.core.transfer

import com.adnan.memoracloud.core.network.NetworkStatus
import com.adnan.memoracloud.domain.model.BackupHold
import com.adnan.memoracloud.domain.model.UserPreferences

/** Applies to backup transfers only; uploads and downloads the user starts are never held. */
object BackupGate {

    fun hold(prefs: UserPreferences, network: NetworkStatus, charging: Boolean): BackupHold? = when {
        prefs.backupChargingOnly && !charging -> BackupHold.CHARGER
        prefs.backupWifiOnly && network != NetworkStatus.UNMETERED -> BackupHold.WIFI
        else -> null
    }
}
