package com.adnan.memoracloud.core.update

import kotlinx.serialization.Serializable

/**
 * The `update.json` asset published next to every APK by the release workflow. It is the contract
 * between the build and the in-app updater: which file to fetch and what hash to expect.
 */
@Serializable
internal data class ReleaseManifest(
    val version: String,
    val versionCode: Int? = null,
    val apk: String? = null,
    val sha256: String? = null,
    val minSdk: Int? = null,
    /** Size of the APK in bytes, so the update dialog can show it without asking the API. */
    val size: Long? = null,
    /** Release notes, optional. */
    val notes: String? = null
)
