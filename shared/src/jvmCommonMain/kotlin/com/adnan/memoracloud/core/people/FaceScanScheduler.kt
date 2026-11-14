package com.adnan.memoracloud.core.people

/** Kicks the background face scanner. Android implements it with WorkManager. */
interface FaceScanScheduler {

    /** Queues a scan; an already queued scan is kept instead of duplicated. */
    fun enqueue()

    fun cancel()
}
