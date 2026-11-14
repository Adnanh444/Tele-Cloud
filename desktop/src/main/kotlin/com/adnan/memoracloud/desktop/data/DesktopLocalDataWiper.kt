package com.adnan.memoracloud.desktop.data

import com.adnan.memoracloud.core.files.AppStoragePaths
import com.adnan.memoracloud.data.local.database.MemoraCloudDatabase
import com.adnan.memoracloud.data.repository.LocalDataWiper
import java.io.File

class DesktopLocalDataWiper(
    private val database: MemoraCloudDatabase,
    private val storagePaths: AppStoragePaths
) : LocalDataWiper {

    override suspend fun wipe() {
        runCatching { database.close() }
        val files = storagePaths.filesDir
        for (name in WIPED_DIRS) {
            runCatching { File(files, name).deleteRecursively() }
        }
        files.listFiles()?.forEach { file ->
            if (file.isFile && file.name.startsWith(DATABASE_PREFIX)) {
                runCatching { file.delete() }
            }
        }
        runCatching { storagePaths.cacheDir.deleteRecursively() }
    }

    private companion object {
        val WIPED_DIRS = listOf("tdlib", "keys", "datastore", "notes", "import")
        const val DATABASE_PREFIX = "memoracloud.db"
    }
}
