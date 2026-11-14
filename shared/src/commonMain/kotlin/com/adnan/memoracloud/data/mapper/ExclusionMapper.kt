package com.adnan.memoracloud.data.mapper

import com.adnan.memoracloud.data.local.entity.ExclusionEntity
import com.adnan.memoracloud.domain.model.Exclusion

fun ExclusionEntity.toDomain(): Exclusion = Exclusion(
    id = id,
    type = type,
    value = value,
    enabled = enabled,
    createdAt = createdAt
)
