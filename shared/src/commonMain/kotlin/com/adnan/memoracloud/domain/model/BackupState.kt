package com.adnan.memoracloud.domain.model

enum class BackupState {
    NONE,
    QUEUED,
    UPLOADING,
    BACKED_UP,
    FAILED
}
