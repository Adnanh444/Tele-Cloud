package com.adnan.memoracloud.domain.ai

/** One thing the on-device model recognised, with its confidence in 0..1. */
data class DetectedLabel(
    val text: String,
    val confidence: Float
)

/**
 * Turns an image file into a handful of tags. Implemented on Android by an on-device model; nothing
 * is uploaded anywhere and no network is used.
 */
interface ImageLabeler {

    suspend fun label(path: String): List<DetectedLabel>
}
