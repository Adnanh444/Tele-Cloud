package com.adnan.memoracloud.domain.usecase

data class ExistingBackupRecord(
    val sizeBytes: Long,
    val modifiedAt: Long,
    val contentHash: String?
)
