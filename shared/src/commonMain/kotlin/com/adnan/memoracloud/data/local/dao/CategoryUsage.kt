package com.adnan.memoracloud.data.local.dao

import com.adnan.memoracloud.domain.model.FileCategory

data class CategoryUsage(
    val category: FileCategory,
    val fileCount: Int,
    val totalBytes: Long
)
