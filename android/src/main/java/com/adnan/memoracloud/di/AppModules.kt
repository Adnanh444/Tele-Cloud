package com.adnan.memoracloud.di

import com.adnan.memoracloud.core.ai.aiModule
import com.adnan.memoracloud.core.common.commonModule
import com.adnan.memoracloud.core.crypto.cryptoModule
import com.adnan.memoracloud.core.dispatchers.dispatchersModule
import com.adnan.memoracloud.core.files.filesModule
import com.adnan.memoracloud.core.files.sharedFilesModule
import com.adnan.memoracloud.core.logging.debugLogModule
import com.adnan.memoracloud.core.media.mediaModule
import com.adnan.memoracloud.core.network.networkModule
import com.adnan.memoracloud.core.power.powerModule
import com.adnan.memoracloud.core.permissions.permissionsModule
import com.adnan.memoracloud.core.proxy.proxyModule
import com.adnan.memoracloud.core.publish.publishModule
import com.adnan.memoracloud.core.security.securityModule
import com.adnan.memoracloud.core.telegram.telegramModule
import com.adnan.memoracloud.core.transfer.transferEngineModule
import com.adnan.memoracloud.core.transfer.transferModule
import com.adnan.memoracloud.core.update.updateModule
import com.adnan.memoracloud.data.local.database.daosModule
import com.adnan.memoracloud.data.local.database.databaseModule
import com.adnan.memoracloud.data.local.preferences.preferencesModule
import com.adnan.memoracloud.data.repository.localDataModule
import com.adnan.memoracloud.data.repository.repositoryModule
import com.adnan.memoracloud.domain.usecase.useCaseModule
import com.adnan.memoracloud.presentation.presentationModule

val appModules = listOf(
    commonModule,
    aiModule,
    cryptoModule,
    dispatchersModule,
    filesModule,
    mediaModule,
    networkModule,
    powerModule,
    permissionsModule,
    proxyModule,
    publishModule,
    securityModule,
    telegramModule,
    transferModule,
    updateModule,
    databaseModule,
    daosModule,
    preferencesModule,
    repositoryModule,
    localDataModule,
    sharedFilesModule,
    debugLogModule,
    transferEngineModule,
    useCaseModule,
    presentationModule
)
