package com.adnan.memoracloud.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One detected face. [embedding] is the raw 192-float signature, kept so faces can be re-grouped
 * later without decoding the photo again.
 */
@Entity(
    tableName = "faces",
    indices = [Index("fileId"), Index("personId")]
)
data class FaceEntity(
    @PrimaryKey val id: String,
    val fileId: String,
    /** Null only between detection and assignment; a stored face always ends up in a group. */
    val personId: Long?,
    val embedding: ByteArray,
    val cropPath: String,
    val score: Float,
    val addedAt: Long
) {
    override fun equals(other: Any?): Boolean =
        other is FaceEntity && other.id == id && other.personId == personId

    override fun hashCode(): Int = id.hashCode() * 31 + (personId?.hashCode() ?: 0)
}
