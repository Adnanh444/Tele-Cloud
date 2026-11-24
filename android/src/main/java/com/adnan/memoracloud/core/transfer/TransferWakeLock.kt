package com.adnan.memoracloud.core.transfer

import android.content.Context
import android.os.PowerManager
import com.adnan.memoracloud.core.common.SafeLog

/**
 * WorkManager holds a lock only while it allows the worker to run, and a screen-off phone on an
 * aggressive OEM build can still pull the CPU away mid-upload. This holds it for the drain, with a
 * timeout so a crash cannot leave the device awake.
 */
class TransferWakeLock(
    private val context: Context
) {

    fun acquire(timeoutMs: Long): PowerManager.WakeLock? {
        return runCatching {
            val power = context.getSystemService(PowerManager::class.java)
                ?: return@runCatching null
            power.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG).apply {
                setReferenceCounted(false)
                acquire(timeoutMs)
            }
        }.onFailure { error ->
            SafeLog.w(TAG, "Could not hold the CPU awake", error)
        }.getOrNull()
    }

    fun release(lock: PowerManager.WakeLock?) {
        runCatching { lock?.takeIf { it.isHeld }?.release() }
    }

    private companion object {
        const val TAG = "MemoraCloud:transfer"
    }
}
