package com.adnan.memoracloud.core.update

import com.adnan.memoracloud.BuildConfig
import org.koin.androidx.workmanager.dsl.workerOf
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val updateModule = module {
    single { UpdateChecker(BuildConfig.VERSION_NAME) }
    single { ApkUpdateInstaller(androidContext(), get()) }
    workerOf(::UpdateCheckWorker)
    workerOf(::UpdateDownloadWorker)
}
