package com.adnan.memoracloud.domain.usecase

import com.adnan.memoracloud.core.common.AppResult
import com.adnan.memoracloud.domain.repository.TransferRepository
import com.adnan.memoracloud.testing.unused

internal class FakeTransferRepository : TransferRepository by unused() {
    val downloads = mutableListOf<String>()

    override suspend fun enqueueDownload(fileId: String, priority: Int): AppResult<String> {
        downloads += fileId
        return AppResult.Success(fileId)
    }
}
