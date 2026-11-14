package com.adnan.memoracloud.data.repository

import com.adnan.memoracloud.core.common.SafeLog
import com.adnan.memoracloud.data.local.dao.PhotoLabelDao
import com.adnan.memoracloud.data.local.entity.PhotoLabelEntity
import com.adnan.memoracloud.domain.ai.DetectedLabel
import com.adnan.memoracloud.domain.model.PhotoTag
import com.adnan.memoracloud.domain.repository.PhotoIntelligenceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * On-device tags, stored locally. Nothing here talks to the network; the worker in the Android
 * module does the actual inference through [com.adnan.memoracloud.domain.ai.ImageLabeler].
 */
class PhotoIntelligenceRepositoryImpl(
    private val photoLabelDao: PhotoLabelDao
) : PhotoIntelligenceRepository {

    override fun observeTag(fileId: String): Flow<PhotoTag?> =
        photoLabelDao.observe(fileId).map { row -> row?.toDomain() }

    override suspend fun pendingCount(): Int = photoLabelDao.pendingCount()

    override suspend fun labelledCount(): Int = photoLabelDao.labelledCount()

    override suspend fun nextBatch(limit: Int): List<Pair<String, String>> =
        photoLabelDao.nextUnlabelled(limit)
            .mapNotNull { file -> file.localPath?.let { path -> file.id to path } }

    override suspend fun save(fileId: String, labels: List<DetectedLabel>, confidence: Float) {
        val cleaned = labels
            .map { it.text.trim().lowercase() to it.confidence }
            .filter { it.first.isNotBlank() }
            .distinctBy { it.first }
            .sortedByDescending { it.second }
            .take(MAX_LABELS)

        photoLabelDao.upsert(
            listOf(
                PhotoLabelEntity(
                    fileId = fileId,
                    labels = cleaned.joinToString(",") { it.first },
                    primaryLabel = cleaned.firstOrNull()?.first.orEmpty(),
                    confidence = confidence,
                    updatedAt = System.currentTimeMillis()
                )
            )
        )
        SafeLog.d(TAG, "Tagged $fileId with ${cleaned.size} label(s)")
    }

    override suspend fun forgetAll() = photoLabelDao.clear()

    override suspend fun labelInventory(): Map<String, Int> =
        photoLabelDao.allLabels()
            .flatMap { it.split(',') }
            .filter { it.isNotBlank() }
            .groupingBy { it }
            .eachCount()

    private fun PhotoLabelEntity.toDomain(): PhotoTag = PhotoTag(
        fileId = fileId,
        labels = labels.split(',').filter { it.isNotBlank() },
        primaryLabel = primaryLabel,
        confidence = confidence
    )

    private companion object {
        const val TAG = "PhotoIntelligence"
        const val MAX_LABELS = 8
    }
}
