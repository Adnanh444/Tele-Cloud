package com.adnan.memoracloud.desktop.network

import com.adnan.memoracloud.core.network.NetworkMonitor
import com.adnan.memoracloud.core.network.NetworkStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class DesktopNetworkMonitor : NetworkMonitor {

    private val state = MutableStateFlow(NetworkStatus.UNMETERED)

    override val status: Flow<NetworkStatus> = state

    override fun currentStatus(): NetworkStatus = state.value
}
