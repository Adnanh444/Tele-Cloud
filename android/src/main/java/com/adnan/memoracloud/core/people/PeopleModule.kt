package com.adnan.memoracloud.core.people

import org.koin.androidx.workmanager.dsl.workerOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

val peopleModule = module {
    singleOf(::FaceAnalyzer)
    singleOf(::WorkFaceScanScheduler) bind FaceScanScheduler::class
    workerOf(::FaceScanWorker)
}
