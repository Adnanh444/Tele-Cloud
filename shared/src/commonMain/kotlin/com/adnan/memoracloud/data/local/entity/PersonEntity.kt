package com.adnan.memoracloud.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** A group of faces the device believes belong to one person. Named by the user, never uploaded. */
@Entity(tableName = "people")
data class PersonEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    /** Best face crop, used as the avatar. */
    val coverPath: String?,
    val createdAt: Long
)
