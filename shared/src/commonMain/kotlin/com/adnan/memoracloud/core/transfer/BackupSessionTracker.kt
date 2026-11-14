package com.adnan.memoracloud.core.transfer

interface BackupSessionTracker {

    suspend fun refreshActive()

    suspend fun refresh(sessionId: String?)
}
