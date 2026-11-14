package com.adnan.memoracloud.desktop.power

import com.adnan.memoracloud.core.power.PowerMonitor
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class DesktopPowerMonitor : PowerMonitor {
    override val charging: Flow<Boolean> = flowOf(true)
    override fun isCharging(): Boolean = true
}
