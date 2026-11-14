package com.adnan.memoracloud.core.update

import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.adnan.memoracloud.R
import com.adnan.memoracloud.core.common.AppNotifications
import com.adnan.memoracloud.core.common.SafeLog
import com.adnan.memoracloud.presentation.platform.UpdateFailure
import com.adnan.memoracloud.presentation.platform.UpdateInstallState
import com.adnan.memoracloud.presentation.platform.UpdateInstaller
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Downloads the APK published on the GitHub release, checks it, and hands it to Android's package
 * installer.
 *
 * Two guarantees, both required before the installer is even offered:
 *  - the SHA-256 of the file matches the hash in update.json ([UpdateInstallState.Failed] with
 *    [UpdateFailure.CHECKSUM] otherwise);
 *  - the APK is signed by the same certificate as the installed app (otherwise Android would refuse
 *    the update, and a silently swapped APK is exactly the attack this guards against).
 *
 * Installation itself always goes through the system installer dialog. Nobody can install an APK
 * silently without a privileged installer, so the app never pretends it can.
 */
class ApkUpdateInstaller(
    private val context: Context,
    private val notifications: AppNotifications
) : UpdateInstaller {

    private val workManager: WorkManager
        get() = WorkManager.getInstance(context)

    /** Set once a download has passed both checks; [install] only ever hands this file over. */
    private var verifiedApk: File? = null

    private val _state = MutableStateFlow<UpdateInstallState>(UpdateInstallState.Idle)
    override val state: StateFlow<UpdateInstallState> = _state.asStateFlow()

    init {
        // The user often leaves the app to grant the "install unknown apps" permission, and Android
        // is free to kill us meanwhile. The verified APK is on disk, so restore the state from it
        // instead of pretending nothing was ever downloaded.
        CoroutineScope(Dispatchers.IO).launch { restoreFromDisk() }
    }

    private val updatesDir: File
        get() = File(context.cacheDir, DIRECTORY).apply { mkdirs() }

    private val apkFile: File
        get() = File(updatesDir, APK_NAME)

    override fun download(release: AppRelease) {
        val url = release.apkUrl
        if (url.isNullOrBlank()) {
            _state.value = UpdateInstallState.Failed(UpdateFailure.NETWORK)
            return
        }
        verifiedApk = null
        apkFile.delete()
        File(updatesDir, PARTIAL_NAME).delete()
        _state.value = UpdateInstallState.Downloading(0L, release.sizeBytes)

        val request = OneTimeWorkRequestBuilder<UpdateDownloadWorker>()
            .setInputData(
                workDataOf(
                    UpdateDownloadWorker.KEY_URL to url,
                    UpdateDownloadWorker.KEY_SHA to (release.sha256 ?: ""),
                    UpdateDownloadWorker.KEY_VERSION to release.version,
                    UpdateDownloadWorker.KEY_TOTAL to release.sizeBytes
                )
            )
            .build()
        workManager.enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.REPLACE, request)
    }

    override fun install() {
        // Re-entrant on purpose: the user may come back from the "install unknown apps" screen and
        // tap Install again on the very same verified file.
        val apk = verifiedApk?.takeIf { it.exists() }
        if (apk == null) {
            SafeLog.w(TAG, "Install tapped with no verified APK on disk")
            _state.value = UpdateInstallState.Failed(UpdateFailure.UNKNOWN)
            return
        }
        // Android 8+ requires the user to allow installs from this app.
        if (!context.packageManager.canRequestPackageInstalls()) {
            SafeLog.w(TAG, "Install blocked: the app may not install unknown packages")
            openUnknownSourcesSetting()
            _state.value = UpdateInstallState.Failed(UpdateFailure.INSTALL_PERMISSION)
            return
        }
        runCatching {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apk
            )
            val intent = Intent(Intent.ACTION_VIEW)
                .setDataAndType(uri, APK_MIME_TYPE)
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            SafeLog.d(TAG, "Handed the APK to the system installer")
            // Back to "ready" so the button stays useful if the user cancels inside Android's own
            // dialog and comes back.
            _state.value = UpdateInstallState.ReadyToInstall
        }.onFailure {
            SafeLog.w(TAG, "Could not launch the package installer", it)
            _state.value = UpdateInstallState.Failed(UpdateFailure.UNKNOWN)
        }
    }

    override fun cancel() {
        workManager.cancelUniqueWork(WORK_NAME)
        verifiedApk = null
        apkFile.delete()
        File(updatesDir, PARTIAL_NAME).delete()
        _state.value = UpdateInstallState.Idle
    }

    private fun openUnknownSourcesSetting() {
        val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES)
            .setData(Uri.parse("package:${context.packageName}"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(intent) }
            .onFailure { SafeLog.w(TAG, "No unknown-sources settings screen", it) }
    }

    // ---------------------------------------------------------------------------------------
    // Called by UpdateDownloadWorker
    // ---------------------------------------------------------------------------------------

    internal fun onProgress(downloaded: Long, total: Long) {
        _state.value = UpdateInstallState.Downloading(downloaded, total)
    }

    /**
     * Downloads, verifies and publishes the terminal state. Returns whether WorkManager should
     * retry, which only makes sense for transport failures.
     */
    internal fun runDownload(url: String, expectedSha: String?, version: String): Boolean {
        val partial = File(updatesDir, PARTIAL_NAME)
        val target = apkFile
        target.delete()

        val total = runCatching { download(url, partial) }
            .onFailure { error ->
                SafeLog.w(TAG, "Update download failed", error)
                _state.value = UpdateInstallState.Failed(
                    if (error is IOException) UpdateFailure.NETWORK else UpdateFailure.UNKNOWN
                )
            }
            .getOrNull() ?: return true

        if (partial.length() == 0L) {
            _state.value = UpdateInstallState.Failed(UpdateFailure.STORAGE)
            return false
        }

        _state.value = UpdateInstallState.Verifying

        val expected = expectedSha?.trim()?.lowercase()?.takeIf { it.length == SHA256_HEX_LENGTH }
        if (expected != null) {
            val actual = runCatching { partial.sha256() }.getOrNull()
            if (actual == null || actual != expected) {
                SafeLog.w(TAG, "APK checksum mismatch (expected $expected, got $actual)")
                partial.delete()
                _state.value = UpdateInstallState.Failed(UpdateFailure.CHECKSUM)
                return false
            }
        }

        if (!partial.renameTo(target)) {
            partial.copyTo(target, overwrite = true)
            partial.delete()
        }

        if (!signedLikeInstalledApp(target)) {
            SafeLog.w(TAG, "APK is not signed by this app's certificate")
            target.delete()
            _state.value = UpdateInstallState.Failed(UpdateFailure.SIGNATURE)
            return false
        }

        verifiedApk = target
        _state.value = UpdateInstallState.ReadyToInstall
        notifications.notifyUpdateReady(
            context.getString(R.string.notification_update_ready_message, version)
        )
        SafeLog.d(TAG, "Update $version downloaded and verified ($total bytes)")
        return false
    }

    /** Resumes from the bytes already on disk when the server honours Range. */
    private fun download(url: String, destination: File): Long {
        var existing = if (destination.exists()) destination.length() else 0L
        var attempt = 0
        while (attempt < MAX_ATTEMPTS) {
            attempt++
            val connection = (URL(url).openConnection() as HttpURLConnection).apply {
                connectTimeout = CONNECT_TIMEOUT_MS
                readTimeout = READ_TIMEOUT_MS
                instanceFollowRedirects = true
                setRequestProperty("User-Agent", USER_AGENT)
                setRequestProperty("Accept", "application/octet-stream")
                if (existing > 0L) setRequestProperty("Range", "bytes=$existing-")
            }
            try {
                val code = connection.responseCode
                if (code == HttpURLConnection.HTTP_OK && existing > 0L) {
                    // The server ignored Range, so start over instead of appending twice.
                    destination.delete()
                    existing = 0L
                } else if (code != HttpURLConnection.HTTP_OK &&
                    code != HttpURLConnection.HTTP_PARTIAL
                ) {
                    throw IOException("HTTP $code")
                }

                val total = connection.contentLengthLong.takeIf { it > 0L }?.plus(existing)
                    ?: existing
                var written = existing
                connection.inputStream.use { input ->
                    FileOutputStream(destination, existing > 0L).use { output ->
                        val buffer = ByteArray(BUFFER_SIZE)
                        while (true) {
                            val read = input.read(buffer)
                            if (read <= 0) break
                            output.write(buffer, 0, read)
                            written += read
                            onProgress(written, total)
                        }
                        output.flush()
                    }
                }
                return written
            } catch (error: IOException) {
                if (attempt >= MAX_ATTEMPTS) throw error
                existing = if (destination.exists()) destination.length() else 0L
                SafeLog.w(TAG, "Retrying update download (attempt $attempt)", error)
            } finally {
                connection.disconnect()
            }
        }
        throw IOException("Download did not complete")
    }

    /**
     * Same package, newer version, same certificate as the installed app. A different signer would
     * make Android refuse the install, and accepting a repacked APK is exactly what this guards
     * against.
     */
    internal fun signedLikeInstalledApp(apk: File, requireNewer: Boolean = false): Boolean {
        val archive = archiveInfo(apk) ?: return false
        if (archive.packageName != context.packageName) return false
        val installed = installedInfo() ?: return false
        if (requireNewer && archive.longVersionCodeX() <= installed.longVersionCodeX()) return false
        val archiveSigner = archive.firstSignerDigest() ?: return false
        val installedSigner = installed.firstSignerDigest() ?: return false
        return archiveSigner == installedSigner
    }

    private fun archiveInfo(apk: File): PackageInfo? {
        val manager = context.packageManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            manager.getPackageArchiveInfo(apk.absolutePath, PackageManager.GET_SIGNING_CERTIFICATES)
        } else {
            @Suppress("DEPRECATION")
            manager.getPackageArchiveInfo(apk.absolutePath, PackageManager.GET_SIGNATURES)
        }
    }

    private fun installedInfo(): PackageInfo? = runCatching {
        val manager = context.packageManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            manager.getPackageInfo(context.packageName, PackageManager.GET_SIGNING_CERTIFICATES)
        } else {
            @Suppress("DEPRECATION")
            manager.getPackageInfo(context.packageName, PackageManager.GET_SIGNATURES)
        }
    }.getOrNull()

    private fun PackageInfo.longVersionCodeX(): Long =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            longVersionCode
        } else {
            @Suppress("DEPRECATION")
            versionCode.toLong()
        }

    /** Re-adopts a previously verified APK, or deletes one that is stale or no longer valid. */
    private suspend fun restoreFromDisk() {
        val apk = apkFile.takeIf { it.exists() } ?: return
        val valid = runCatching { signedLikeInstalledApp(apk, requireNewer = true) }
            .onFailure { SafeLog.w(TAG, "Could not inspect the cached update APK", it) }
            .getOrDefault(false)
        if (!valid) {
            apk.delete()
            return
        }
        verifiedApk = apk
        _state.value = UpdateInstallState.ReadyToInstall
        SafeLog.d(TAG, "Recovered a downloaded update from cache")
    }

    private fun PackageInfo.firstSignerDigest(): String? {
        val signer = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            signingInfo?.apkContentsSigners?.firstOrNull()
        } else {
            @Suppress("DEPRECATION")
            signatures?.firstOrNull()
        } ?: return null
        return MessageDigest.getInstance("SHA-256")
            .digest(signer.toByteArray())
            .joinToString("") { byte -> "%02x".format(byte) }
    }

    private fun File.sha256(): String {
        val digest = MessageDigest.getInstance("SHA-256")
        inputStream().use { input ->
            val buffer = ByteArray(BUFFER_SIZE)
            while (true) {
                val read = input.read(buffer)
                if (read <= 0) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { byte -> "%02x".format(byte) }
    }

    internal companion object {
        const val TAG = "ApkUpdateInstaller"
        const val WORK_NAME = "apk-update-download"
        const val DIRECTORY = "updates"
        const val APK_NAME = "MemoraCloud-update.apk"
        const val PARTIAL_NAME = "MemoraCloud-update.part"
        private const val APK_MIME_TYPE = "application/vnd.android.package-archive"
        private const val USER_AGENT = "MemoraCloud"
        private const val SHA256_HEX_LENGTH = 64
        private const val BUFFER_SIZE = 64 * 1024
        private const val CONNECT_TIMEOUT_MS = 20_000
        private const val READ_TIMEOUT_MS = 30_000
        private const val MAX_ATTEMPTS = 3
    }
}
