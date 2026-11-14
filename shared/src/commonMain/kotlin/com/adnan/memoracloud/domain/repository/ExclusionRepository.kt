package com.adnan.memoracloud.domain.repository

import com.adnan.memoracloud.domain.model.Exclusion
import com.adnan.memoracloud.domain.model.ExclusionType
import kotlinx.coroutines.flow.Flow

interface ExclusionRepository {

    fun observeAll(): Flow<List<Exclusion>>

    /** Only adds a rule the drive has none of, so a rule the user removed stays removed. */
    suspend fun ensureDefaults(chatId: Long)

    suspend fun getEnabled(): List<Exclusion>

    suspend fun add(type: ExclusionType, value: String): Exclusion

    suspend fun setEnabled(id: String, enabled: Boolean)

    suspend fun remove(id: String)
}
