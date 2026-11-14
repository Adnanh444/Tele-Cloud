package com.adnan.memoracloud.core.compat

import com.adnan.memoracloud.core.ai.PhotoLabelWorker
import com.adnan.memoracloud.core.people.FaceScanWorker
import org.koin.androidx.workmanager.dsl.workerOf
import org.koin.dsl.module

/** Koin still has to be able to build the two leftover job types, so they are registered here. */
val legacyWorkersModule = module {
    workerOf(::PhotoLabelWorker)
    workerOf(::FaceScanWorker)
}
