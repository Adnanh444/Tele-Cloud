package com.adnan.memoracloud.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Marks a photo as looked at, whether or not it contained a face. Without this row every photo
 * without a face would be decoded again on every run.
 */
@Entity(tableName = "face_scans")
data class FaceScanEntity(
    @PrimaryKey val fileId: String,
    val faceCount: Int,
    val scannedAt: Long
)
