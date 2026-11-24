package com.adnan.memoracloud.core.transfer

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.adnan.memoracloud.core.common.SafeLog
import com.adnan.memoracloud.data.local.dao.TransferDao
import com.adnan.memoracloud.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.first

/**
 * A queued row is only picked up when something kicks the transfer worker. If that single kick is
 * lost — process killed between enqueue and run, an OEM freeze, expedited quota exhausted — the
 * queue sits there until the app is opened. This runs every fifteen minutes and kicks it again.
 */
class QueueWatchdogWorker(
    appContext: Context,
    params: WorkerParameters,
    private val transferDao: TransferDao,
    private val transferScheduler: TransferScheduler,
    private val settingsRepository: SettingsRepository
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val prefs = settingsRepository.preferences.first()
        // Not gated on auto backup: a manual upload queued just before the app died needs this too.
        val queued = transferDao.countQueued()
        if (queued == 0) return Result.success()
        SafeLog.d(TAG, "Re-kicking $queued queued transfers")
        transferScheduler.kick(allowMetered = !prefs.backupWifiOnly)
        return Result.success()
    }

    companion object {
        private const val TAG = "QueueWatchdog"
        const val UNIQUE_NAME = "queue_watchdog"
    }
}
