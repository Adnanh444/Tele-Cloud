package com.adnan.memoracloud.presentation.platform

import androidx.compose.runtime.staticCompositionLocalOf
import com.adnan.memoracloud.core.update.AppRelease
import kotlinx.coroutines.flow.StateFlow

/**
 * What the update UI needs to know. Platform free, so the shared screens can render it; the Android
 * side does the downloading, verifying and installing.
 */
sealed interface UpdateInstallState {

    data object Idle : UpdateInstallState

    data class Downloading(val downloadedBytes: Long, val totalBytes: Long) : UpdateInstallState {
        val fraction: Float
            get() = if (totalBytes > 0L) {
                (downloadedBytes.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f)
            } else {
                0f
            }

        val isDeterminate: Boolean
            get() = totalBytes > 0L
    }

    /** Hash and signature are being compared with the installed app. */
    data object Verifying : UpdateInstallState

    /** Downloaded, verified, waiting for the user to tap Install. */
    data object ReadyToInstall : UpdateInstallState

    data class Failed(val reason: UpdateFailure) : UpdateInstallState
}

enum class UpdateFailure {
    NETWORK,
    CHECKSUM,
    SIGNATURE,
    INSTALL_PERMISSION,
    STORAGE,
    UNKNOWN
}

interface UpdateInstaller {

    val state: StateFlow<UpdateInstallState>

    /** Starts (or resumes) the download of [release]. */
    fun download(release: AppRelease)

    /** Hands the verified APK to Android's package installer. */
    fun install()

    /** Stops the download and forgets the partial file. */
    fun cancel()

    /** True when this platform can update itself in place. */
    val supported: Boolean
        get() = true
}

/** Null on platforms that update themselves some other way (the desktop builds). */
val LocalUpdateInstaller = staticCompositionLocalOf<UpdateInstaller?> { null }
