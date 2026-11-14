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

    /**
     * Two sources, cheapest and most reliable first:
     *
     * 1. `releases/latest/download/update.json` — a plain download link to an asset of the newest
     *    release. No REST API, so no anonymous rate limit and no quota shared with everyone else on
     *    the same mobile network.
     * 2. The REST API, kept as a fallback for releases published without update.json. It allows 60
     *    anonymous requests per hour per IP, which is why 403 "rate limited" used to break checks.
     */
    suspend fun check(): UpdateCheck = withContext(Dispatchers.IO) {
        directCheck()?.let { return@withContext it }
        apiCheck()
    }

    /** Reads the manifest the release publishes as a normal asset. Null when unavailable. */
    private fun directCheck(): UpdateCheck? {
        val payload = runCatching { fetch(DIRECT_MANIFEST_URL) }
            .onFailure { error ->
                SafeLog.d(TAG, "Direct manifest unavailable: ${error.message}")
            }
            .getOrNull() ?: return null

        val manifest = runCatching {
            json.decodeFromString(ReleaseManifest.serializer(), payload)
        }.getOrElse {
            SafeLog.w(TAG, "update.json unreadable", it)
            return null
        }

        val latest = manifest.version.removePrefix("v").trim()
        val apkName = manifest.apk?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        if (latest.isEmpty()) return null
        if (!isNewer(latest, currentVersion)) {
            return UpdateCheck.UpToDate(currentVersion, latest)
        }
        return UpdateCheck.Available(
            AppRelease(
                version = latest,
                notes = manifest.notes?.trim()?.takeIf { it.isNotEmpty() }
                    ?: "New version of Memora Cloud, signed and ready to install.",
                pageUrl = RELEASES_PAGE_URL,
                apkUrl = "$DIRECT_DOWNLOAD_BASE/$apkName",
                apkName = apkName,
                sha256 = manifest.sha256
                    ?.trim()
                    ?.lowercase()
                    ?.takeIf { it.length == SHA256_HEX_LENGTH },
                versionCode = manifest.versionCode,
                sizeBytes = manifest.size ?: 0L
            )
        )
    }

    private fun apiCheck(): UpdateCheck {
        val payload = try {
            fetch(RELEASES_URL)
        } catch (error: HttpStatusException) {
            SafeLog.w(TAG, "Update check HTTP ${error.code}")
            return UpdateCheck.Failed(
                when (error.code) {
                    // GitHub answers 404 both for "no releases" and for a private repository,
                    // because the app never sends credentials.
                    HttpURLConnection.HTTP_NOT_FOUND ->
                        "no release found (nothing published, or the repository is private)"

                    HttpURLConnection.HTTP_FORBIDDEN ->
                        "GitHub is limiting how often this network may check (temporary). " +
                                "Try again in a few minutes."

                    else -> "GitHub answered HTTP ${error.code}"
                }
            )
        } catch (error: Throwable) {
            SafeLog.w(TAG, "Update check failed", error)
            return UpdateCheck.Failed(networkReason(error))
        }

        val release = runCatching {
            json.decodeFromString(GitHubRelease.serializer(), payload)
        }.getOrElse {
            SafeLog.w(TAG, "Release JSON unreadable", it)
            return UpdateCheck.Failed("release data could not be read")
        }

        if (release.draft || release.preRelease) {
            return UpdateCheck.Failed("newest release is a draft or pre-release")
        }

        val latest = release.tag.removePrefix("v").trim()
        if (!isNewer(latest, currentVersion)) {
            return UpdateCheck.UpToDate(currentVersion, latest)
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
            ?: return UpdateCheck.Failed("release $latest has no APK to install")

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
        const val RELEASES_PAGE_URL = "https://github.com/Adnanh444/Tele-Cloud/releases/latest"

        /** Plain asset link: no API, no rate limit, always the newest published release. */
        const val DIRECT_MANIFEST_URL =
            "https://github.com/Adnanh444/Tele-Cloud/releases/latest/download/update.json"
        const val DIRECT_DOWNLOAD_BASE =
            "https://github.com/Adnanh444/Tele-Cloud/releases/latest/download"
        const val USER_AGENT = "MemoraCloud"
        const val MANIFEST_NAME = "update.json"
        const val SHA256_HEX_LENGTH = 64
        const val TIMEOUT_MS = 20_000
    }
}
