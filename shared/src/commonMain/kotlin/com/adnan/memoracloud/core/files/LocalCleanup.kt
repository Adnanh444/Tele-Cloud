package com.adnan.memoracloud.core.files

data class LocalCleanup(
    val deletedCount: Int,
    val consentRequest: DeleteConsentRequest? = null,
    val keptOffline: Int = 0
)
