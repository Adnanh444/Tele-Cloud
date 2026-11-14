package com.adnan.memoracloud.core.telegram

enum class TelegramConnectionState {
    WAITING_FOR_NETWORK,
    CONNECTING,
    UPDATING,
    READY
}
