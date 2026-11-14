package com.adnan.memoracloud.core.ai

/** Kicks the background tagger. Android implements it with WorkManager. */
interface PhotoTagScheduler {

    /** Queues a tagging run; a run already scheduled is kept rather than duplicated. */
    fun enqueue()

    fun cancel()
}
