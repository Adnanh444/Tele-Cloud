package com.adnan.memoracloud.domain.model

/**
 * Which copy a delete removes. Nothing is ever destroyed outright: the cloud copy goes to Trash for
 * 30 days, and a removed phone copy is still on Telegram, one download away.
 */
enum class DeleteScope {
    CLOUD,
    PHONE,
    BOTH
}
