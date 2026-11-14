package com.adnan.memoracloud.data.repository

import com.adnan.memoracloud.core.files.AppStoragePaths
import com.adnan.memoracloud.testing.unused
import java.io.File

internal class FakeStoragePaths : AppStoragePaths by unused() {
    private val root = File.createTempFile("teledrive-paths", "").also {
        it.delete()
        it.mkdirs()
    }
    override val cacheDir: File = File(root, "cache").also { it.mkdirs() }
}
