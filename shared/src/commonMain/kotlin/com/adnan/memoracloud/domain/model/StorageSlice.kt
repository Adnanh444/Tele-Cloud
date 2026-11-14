package com.adnan.memoracloud.domain.model

data class StorageSlice(
    val category: FileCategory,
    val fileCount: Int,
    val totalBytes: Long
)
