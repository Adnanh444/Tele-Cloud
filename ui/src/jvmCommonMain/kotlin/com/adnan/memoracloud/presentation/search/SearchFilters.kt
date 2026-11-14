package com.adnan.memoracloud.presentation.search

import com.adnan.memoracloud.domain.model.FileCategory
import com.adnan.memoracloud.domain.model.FileSortField
import com.adnan.memoracloud.domain.model.SortDirection

data class SearchFilters(
    val category: FileCategory? = null,
    val backedUpOnly: Boolean = false,
    val notBackedUpOnly: Boolean = false,
    val minSizeMb: Int? = null,
    val sortField: FileSortField = FileSortField.DATE_MODIFIED,
    val sortDirection: SortDirection = SortDirection.DESCENDING
)
