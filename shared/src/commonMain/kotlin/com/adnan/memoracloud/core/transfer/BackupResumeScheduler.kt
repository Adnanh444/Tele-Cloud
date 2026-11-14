package com.adnan.memoracloud.core.transfer

interface BackupResumeScheduler {
    fun resumeWhen(chargingOnly: Boolean, wifiOnly: Boolean)
}
