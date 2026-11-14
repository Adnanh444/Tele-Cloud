package com.adnan.memoracloud.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Tags produced on this device for one image. Kept in its own table so the photo index can be
 * rebuilt or wiped without touching file rows.
 */
@Entity(tableName = "photo_labels")
data class PhotoLabelEntity(
    @PrimaryKey val fileId: String,
    /** Lowercase, comma separated, best first. Empty when the model found nothing. */
    val labels: String,
    val primaryLabel: String,
    val confidence: Float,
    val updatedAt: Long
)
