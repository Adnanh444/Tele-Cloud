package com.adnan.memoracloud.data.local.database

import androidx.room.RoomDatabaseConstructor

@Suppress("KotlinNoActualForExpect")
expect object MemoraCloudDatabaseConstructor : RoomDatabaseConstructor<MemoraCloudDatabase> {
    override fun initialize(): MemoraCloudDatabase
}
