package com.adnan.memoracloud.core.telegram

interface TdlibDatabaseKeyProvider {
    fun databaseKey(): ByteArray
}
