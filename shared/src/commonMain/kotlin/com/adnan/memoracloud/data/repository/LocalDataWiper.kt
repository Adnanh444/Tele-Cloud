package com.adnan.memoracloud.data.repository

interface LocalDataWiper {

    suspend fun wipe()
}
