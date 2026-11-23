package com.adnan.memoracloud.core.transfer

sealed interface TransferOutcome {
    data object Completed : TransferOutcome
    data object Paused : TransferOutcome
    data object Canceled : TransferOutcome
    data class Failed(val message: String, val retryAfterSeconds: Int? = null) : TransferOutcome

    /**
     * Cannot succeed however often it is retried - the source file is gone from the device, for
     * instance. Retrying just keeps a dead row at the head of the queue.
     */
    data class Abandoned(val message: String) : TransferOutcome
}
