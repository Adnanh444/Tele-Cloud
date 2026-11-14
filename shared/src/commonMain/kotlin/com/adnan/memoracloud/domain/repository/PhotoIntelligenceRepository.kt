package com.adnan.memoracloud.domain.repository

import com.adnan.memoracloud.domain.ai.DetectedLabel
import com.adnan.memoracloud.domain.model.PhotoTag
import kotlinx.coroutines.flow.Flow

interface PhotoIntelligenceRepository {

    fun observeTag(fileId: String): Flow<PhotoTag?>

    suspend fun pendingCount(): Int

    suspend fun labelledCount(): Int

    /** Local images waiting for tags, newest first. */
    suspend fun nextBatch(limit: Int): List<Pair<String, String>>

    suspend fun save(fileId: String, labels: List<DetectedLabel>, confidence: Float)

    suspend fun forgetAll()

    /** Everything currently known, for a tag cloud / debugging. */
    suspend fun labelInventory(): Map<String, Int>
}
