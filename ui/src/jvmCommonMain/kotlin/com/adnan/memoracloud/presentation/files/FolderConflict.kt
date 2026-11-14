package com.adnan.memoracloud.presentation.files

import com.adnan.memoracloud.core.files.ImportSource

data class FolderConflict(
    val names: List<String>,
    val renamed: Map<String, String>,
    val sources: List<ImportSource>,
    val target: String?
)
