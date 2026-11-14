package com.adnan.memoracloud.domain.usecase

import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

val useCaseModule = module {
    factoryOf(::DecideBackupActionUseCase)
    factoryOf(::EvaluateExclusionsUseCase)
    factoryOf(::MakeAvailableOfflineUseCase)
    factoryOf(::TrashExpiryCalculator)
    factoryOf(::ValidateUploadUseCase)
}
