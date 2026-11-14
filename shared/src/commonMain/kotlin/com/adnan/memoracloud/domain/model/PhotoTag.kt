package com.adnan.memoracloud.domain.model

/** What the device recognised in one photo. [labels] is ordered, best match first. */
data class PhotoTag(
    val fileId: String,
    val labels: List<String>,
    val primaryLabel: String,
    val confidence: Float
) {
    val isEmpty: Boolean get() = labels.isEmpty()
}
