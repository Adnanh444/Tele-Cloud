package com.adnan.memoracloud.core.update

import com.adnan.memoracloud.core.common.SafeLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.net.HttpURLConnection
import java.net.URL

/**
 * An anonymous read of public endpoints, so a check reveals no more than visiting the releases
 * page. The APK itself is downloaded later, by the platform layer, and verified there.
 */
class UpdateChecker(
    private val currentVersion: String
) {

    private val json = Json { ignoreUnknownKeys = true }

    suspend fun newerRelease(): AppRelease? = withContext(Dispatchers.IO) {
        val payload = runCatching { fetch(RELEASES_URL) }
            .onFailure { SafeLog.w(TAG, "Update check failed", it) }
            .getOrNull()
            ?: return@withContext null

        val release = runCatching {
            json.decodeFromString(GitHubRelease.serializer(), payload)
        }.getOrNull() ?: return@withContext null

        if (release.draft || release.preRelease) return@withContext null
        val latest = release.tag.removePrefix("v").trim()
        if (!isNewer(latest, currentVersion)) return@withContext null

        // update.json is optional: without it the app still offers the download, just without a
        // published hash to compare against (the signature check still runs).
        val manifest = release.assets
            .firstOrNull { it.name == MANIFEST_NAME }
            ?.let { asset ->
                runCatching {
                    json.decodeFromString(
                        ReleaseManifest.serializer(),
                        fetch(asset.downloadUrl)
                    )
                }.onFailure { SafeLog.w(TAG, "update.json unreadable", it) }.getOrNull()
            }

        val apkAsset = manifest?.apk
            ?.let { wanted -> release.assets.firstOrNull { it.name == wanted } }
            ?: release.assets.firstOrNull { it.name.endsWith(".apk", ignoreCase = true) }

        AppRelease(
            version = latest,
            notes = release.body.orEmpty().trim(),
            pageUrl = release.pageUrl,
            apkUrl = apkAsset?.downloadUrl,
            apkName = apkAsset?.name,
            sha256 = manifest?.sha256
                ?.trim()
                ?.lowercase()
                ?.takeIf { it.length == SHA256_HEX_LENGTH },
            versionCode = manifest?.versionCode,
            sizeBytes = apkAsset?.size ?: 0L
        )
    }

    private fun fetch(url: String): String {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = TIMEOUT_MS
            readTimeout = TIMEOUT_MS
            setRequestProperty("Accept", "application/vnd.github+json, application/json")
            setRequestProperty("User-Agent", USER_AGENT)
        }
        return try {
            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                error("HTTP ${connection.responseCode}")
            }
            connection.inputStream.bufferedReader().use { it.readText() }
        } finally {
            connection.disconnect()
        }
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

    private companion object {
        const val TAG = "UpdateChecker"
        const val RELEASES_URL =
            "https://api.github.com/repos/Adnanh444/Tele-Cloud/releases/latest"
        const val USER_AGENT = "MemoraCloud"
        const val MANIFEST_NAME = "update.json"
        const val SHA256_HEX_LENGTH = 64
        const val TIMEOUT_MS = 15_000
    }
}
