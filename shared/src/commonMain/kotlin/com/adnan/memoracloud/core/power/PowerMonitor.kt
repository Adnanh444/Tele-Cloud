package com.adnan.memoracloud.core.power

import kotlinx.coroutines.flow.Flow

interface PowerMonitor {
    val charging: Flow<Boolean>
    fun isCharging(): Boolean
}
