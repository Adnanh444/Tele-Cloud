package com.adnan.memoracloud.core.update

/**
 * The outcome of an update check, including the boring and the broken cases: a check that finds
 * nothing used to look exactly like a check that failed, which made the updater impossible to
 * diagnose from the phone. Every branch now carries a short, human-readable reason.
 */
sealed interface UpdateCheck {

    data class Available(val release: AppRelease) : UpdateCheck

    /** Nothing to install. Keeps both versions so the UI can show which is which. */
    data class UpToDate(val current: String, val latest: String) : UpdateCheck

    data class Failed(val reason: String) : UpdateCheck
}
