package com.adnan.memoracloud.core.ai

import com.adnan.memoracloud.domain.ai.ImageLabeler
import org.koin.androidx.workmanager.dsl.workerOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

val aiModule = module {
    singleOf(::MlKitImageLabeler) bind ImageLabeler::class
    singleOf(::WorkPhotoTagScheduler) bind PhotoTagScheduler::class
    workerOf(::PhotoLabelWorker)
}
