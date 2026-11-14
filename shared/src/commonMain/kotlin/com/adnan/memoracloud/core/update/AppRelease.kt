package com.adnan.memoracloud.core.update

/**
 * A newer release, as far as the app needs to care.
 *
 * [apkUrl] is filled in when the release actually ships an installable APK, which is what enables
 * in-app updating. Everything else is presentation data.
 */
data class AppRelease(
    val version: String,
    val notes: String,
    val pageUrl: String,
    val apkUrl: String? = null,
    val apkName: String? = null,
    /** Lowercase hex SHA-256 of the APK, published in the release's update.json. */
    val sha256: String? = null,
    val versionCode: Int? = null,
    val sizeBytes: Long = 0L
) {

    /** True when this release can be downloaded and installed without leaving the app. */
    val canInstallInApp: Boolean
        get() = !apkUrl.isNullOrBlank()
}
