package com.adnan.memoracloud.data.repository

import com.adnan.memoracloud.data.remote.telegram.RemoteFolderState

internal sealed interface RemoteFolderRead {
    data class Ok(val state: RemoteFolderState) : RemoteFolderRead
    data object Unreadable : RemoteFolderRead
    data object Unavailable : RemoteFolderRead
}
