package com.adnan.memoracloud.core.files

import com.adnan.memoracloud.presentation.platform.DeviceFolderScanner
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

val filesModule = module {
    singleOf(::AndroidStoragePaths) bind AppStoragePaths::class
    singleOf(::MediaStoreDownloadWriter) bind DownloadWriter::class
    singleOf(::AndroidFileImporter) bind FileImporter::class
    singleOf(::MediaStoreLocalCopyDeleter) bind LocalCopyDeleter::class
    singleOf(::AndroidPendingShare) bind PendingShare::class
    singleOf(::MediaStoreFolderScanner) bind DeviceFolderScanner::class
}
