package com.adnan.memoracloud.core.logging

import com.adnan.memoracloud.core.files.AppStoragePaths
import java.io.File

/**
 * Reads the crash text the Android layer wrote, so the app can show it after it manages to start.
 * On desktop there is no crash handler, so [read] simply returns null.
 */
class CrashLogStore(private val storagePaths: AppStoragePaths) {

    private fun file(): File = File(File(storagePaths.filesDir, "crash"), "last.txt")

    fun read(): String? = runCatching {
        val file = file()
        if (file.isFile && file.length() > 0) file.readText() else null
    }.getOrNull()

    fun clear() {
        runCatching { file().delete() }
    }
}
