package com.adnan.memoracloud.core.update

import com.adnan.memoracloud.core.common.SafeLog
import java.io.IOException
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.net.UnknownHostException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

/**
 * An anonymous read of public endpoints, so a check reveals no more than visiting the releases
 * page. The APK itself is downloaded later, by the platform layer, and verified there.
 */
class UpdateChecker(
    private val currentVersion: String
) {

    private val json = Json { ignoreUnknownKeys = true }

    /** Convenience for the background worker, which only cares whether there is something to show. */
    suspend fun newerRelease(): AppRelease? = when (val result = check()) {
        is UpdateCheck.Available -> result.release
        else -> null
    }

    suspend fun check(): UpdateCheck = withContext(Dispatchers.IO) {
        val payload = try {
            fetch(RELEASES_URL)
        } catch (error: HttpStatusException) {
            SafeLog.w(TAG, "Update check HTTP ${error.code}")
            return@withContext UpdateCheck.Failed(
                if (error.code == HttpURLConnection.HTTP_NOT_FOUND) {
                    // GitHub answers 404 both for "no releases" and for a private repository,
                    // because the app never sends credentials.
                    "no release found (nothing published, or the repository is private)"
                } else {
                    "GitHub answered HTTP ${error.code}"
                }
            )
        } catch (error: Throwable) {
            SafeLog.w(TAG, "Update check failed", error)
            return@withContext UpdateCheck.Failed(networkReason(error))
        }

        val release = runCatching {
            json.decodeFromString(GitHubRelease.serializer(), payload)
        }.getOrElse {
            SafeLog.w(TAG, "Release JSON unreadable", it)
            return@withContext UpdateCheck.Failed("release data could not be read")
        }

        if (release.draft || release.preRelease) {
            return@withContext UpdateCheck.Failed("newest release is a draft or pre-release")
        }

        val latest = release.tag.removePrefix("v").trim()
        if (!isNewer(latest, currentVersion)) {
            return@withContext UpdateCheck.UpToDate(currentVersion, latest)
        }

        // update.json is optional: without it the download is still offered, just without a
        // published hash (the signature check still runs before anything is installed).
        val manifest = release.assets
            .firstOrNull { it.name == MANIFEST_NAME }
            ?.let { asset ->
                runCatching {
                    json.decodeFromString(ReleaseManifest.serializer(), fetch(asset.downloadUrl))
                }.onFailure { SafeLog.w(TAG, "update.json unreadable", it) }.getOrNull()
            }

        val apkAsset = manifest?.apk
            ?.let { wanted -> release.assets.firstOrNull { it.name == wanted } }
            ?: release.assets.firstOrNull { it.name.endsWith(".apk", ignoreCase = true) }
            ?: return@withContext UpdateCheck.Failed("release $latest has no APK to install")

        UpdateCheck.Available(
            AppRelease(
                version = latest,
                notes = release.body.orEmpty().trim(),
                pageUrl = release.pageUrl,
                apkUrl = apkAsset.downloadUrl,
                apkName = apkAsset.name,
                sha256 = manifest?.sha256
                    ?.trim()
                    ?.lowercase()
                    ?.takeIf { it.length == SHA256_HEX_LENGTH },
                versionCode = manifest?.versionCode,
                sizeBytes = apkAsset.size
            )
        )
    }

    private fun fetch(url: String): String {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = TIMEOUT_MS
            readTimeout = TIMEOUT_MS
            instanceFollowRedirects = true
            setRequestProperty("Accept", "application/vnd.github+json, application/json")
            setRequestProperty("User-Agent", USER_AGENT)
        }
        return try {
            val code = connection.responseCode
            if (code != HttpURLConnection.HTTP_OK) throw HttpStatusException(code)
            connection.inputStream.bufferedReader().use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }

    /** Short enough for a settings row, specific enough to act on. */
    private fun networkReason(error: Throwable): String = when (error) {
        is UnknownHostException -> "no internet connection to GitHub"
        is SocketTimeoutException -> "GitHub took too long to answer"
        is IOException -> error.message ?: "network problem"
        else -> error.message ?: error::class.simpleName ?: "unknown error"
    }

    /** Compares dotted numbers so 1.10 counts as newer than 1.9. */
    private fun isNewer(candidate: String, current: String): Boolean {
        val left = candidate.numbers()
        val right = current.removePrefix("v").numbers()
        if (left.isEmpty()) return false
        for (index in 0 until maxOf(left.size, right.size)) {
            val a = left.getOrElse(index) { 0 }
            val b = right.getOrElse(index) { 0 }
            if (a != b) return a > b
        }
        return false
    }

    private fun String.numbers(): List<Int> = split('.', '-', '_')
        .mapNotNull { part -> part.takeWhile(Char::isDigit).toIntOrNull() }

    private class HttpStatusException(val code: Int) : IOException("HTTP $code")

    private companion object {
        const val TAG = "UpdateChecker"
        const val RELEASES_URL =
            "https://api.github.com/repos/Adnanh444/Tele-Cloud/releases/latest"
        const val USER_AGENT = "MemoraCloud"
        const val MANIFEST_NAME = "update.json"
        const val SHA256_HEX_LENGTH = 64
        const val TIMEOUT_MS = 20_000
    }
}
