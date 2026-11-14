package com.adnan.memoracloud.domain.model

/**
 * One person the device has grouped together from faces it recognised. Purely local: nothing about
 * faces leaves the phone, and the grouping can be wiped at any time from Settings.
 */
data class Person(
    val id: Long,
    val name: String,
    /** JPEG of the best face crop, written to app storage by the analyzer. */
    val coverPath: String?,
    val faceCount: Int,
    val photoCount: Int
)
