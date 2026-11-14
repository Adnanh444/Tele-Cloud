package com.adnan.memoracloud.desktop.transfer

import com.adnan.memoracloud.core.transfer.MaintenanceScheduler

/** No background scheduler yet, so maintenance runs only while the app is open. */
class DesktopMaintenanceScheduler : MaintenanceScheduler {

    override fun scheduleUpdateCheck(enabled: Boolean) {
    }

    override fun scheduleAll(
        backupEnabled: Boolean,
        backupIntervalHours: Int,
        wifiOnly: Boolean,
        chargingOnly: Boolean,
        instantBackup: Boolean,
        updateChecks: Boolean
    ) {
    }
}
