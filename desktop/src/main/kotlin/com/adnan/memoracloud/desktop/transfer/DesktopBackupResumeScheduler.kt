package com.adnan.memoracloud.desktop.transfer

import com.adnan.memoracloud.core.transfer.BackupResumeScheduler

class DesktopBackupResumeScheduler : BackupResumeScheduler {
    override fun resumeWhen(chargingOnly: Boolean, wifiOnly: Boolean) = Unit
}
