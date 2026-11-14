package com.adnan.memoracloud.core.files

import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val sharedFilesModule = module {
    singleOf(::NoteStore)
    singleOf(::StorageInspector)
}
