package com.adnan.memoracloud.domain.model

enum class BackupDecision {
    BACKUP,
    SKIP_UNCHANGED,
    SKIP_EXCLUDED,
    SKIP_TOO_LARGE,
    SKIP_DUPLICATE,

    /** The file's kind (photo, video, …) is switched off in settings. */
    SKIP_DISABLED_CATEGORY
}
