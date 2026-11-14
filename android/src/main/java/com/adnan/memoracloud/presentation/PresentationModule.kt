package com.adnan.memoracloud.presentation

import com.adnan.memoracloud.presentation.di.sharedUiModule
import com.adnan.memoracloud.presentation.platform.AndroidPlatformCapabilities
import com.adnan.memoracloud.presentation.platform.AndroidStandardFolderPaths
import com.adnan.memoracloud.presentation.platform.PlatformCapabilities
import com.adnan.memoracloud.presentation.platform.StandardFolderPaths
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.bind
import org.koin.dsl.module

val presentationModule = module {
    singleOf(::AndroidStandardFolderPaths) bind StandardFolderPaths::class
    singleOf(::AndroidPlatformCapabilities) bind PlatformCapabilities::class
    includes(sharedUiModule)
}
