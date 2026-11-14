package com.adnan.memoracloud.presentation.platform

import androidx.compose.runtime.staticCompositionLocalOf

/**
 * A folder found on the device that actually contains photos or videos.
 *
 * [path] is an absolute file-system path, the same form the backup scan walks and the same value
 * stored in the backup-folder selection, so ticking a discovered folder needs no conversion.
 */
data class DeviceFolder(
    val path: String,
    val name: String,
    val photoCount: Int,
    val videoCount: Int,
    val sizeBytes: Long
) {
    val mediaCount: Int
        get() = photoCount + videoCount
}

/**
 * Asks the device which folders hold media. Implemented with MediaStore on Android, with a bounded
 * file-system walk as a fallback for devices where MediaStore is empty or only partially readable.
 */
interface DeviceFolderScanner {

    suspend fun scan(): List<DeviceFolder>
}

/** Null on platforms that cannot enumerate the device (desktop). The UI then stays as it was. */
val LocalDeviceFolderScanner = staticCompositionLocalOf<DeviceFolderScanner?> { null }
