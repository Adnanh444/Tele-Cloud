package com.adnan.memoracloud.core.compat

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.adnan.memoracloud.core.common.SafeLog
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Writes the last uncaught exception to `files/crash/last.txt` before the process dies, so a crash
 * can be read *without* a computer and logcat: the next successful start shows it in a dialog.
 *
 * Installed from `attachBaseContext`, because content providers (WorkManager among them) run before
 * `Application.onCreate` and can be the thing that crashes.
 */
object CrashHandler {

    private const val DIR = "crash"
    private const val FILE = "last.txt"

    /** Also written to Downloads/MemoraCloud so it is reachable without the app. */
    private const val PUBLIC_FILE = "memora-crash.txt"

    fun install(context: Context) {
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            runCatching { write(context.applicationContext, thread, error) }
            previous?.uncaughtException(thread, error)
                ?: Runtime.getRuntime().halt(1)
        }
    }

    private fun publishToDownloads(context: Context, text: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, PUBLIC_FILE)
                put(MediaStore.Downloads.MIME_TYPE, "text/plain")
                put(
                    MediaStore.Downloads.RELATIVE_PATH,
                    Environment.DIRECTORY_DOWNLOADS + "/MemoraCloud"
                )
            }
            val uri = context.contentResolver.insert(
                MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                values
            ) ?: return
            context.contentResolver.openOutputStream(uri)?.use { out ->
                out.write(text.toByteArray())
            }
        }
    }

    private fun write(context: Context, thread: Thread, error: Throwable) {
        val text = buildString {
            append("Memora Cloud crash\n")
            append(SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())).append('\n')
            append("thread: ").append(thread.name).append('\n')
            append("device: ").append(android.os.Build.MANUFACTURER).append(' ')
                .append(android.os.Build.MODEL)
                .append(" / Android ").append(android.os.Build.VERSION.RELEASE).append('\n')
            append('\n')
            append(error.stackTraceToString())
        }
        val dir = File(context.filesDir, DIR).apply { mkdirs() }
        runCatching { File(dir, FILE).writeText(text) }
        // A copy the user can open from any file manager, in case the app never starts again.
        runCatching { publishToDownloads(context, text) }
        SafeLog.e("Crash", "Uncaught exception on ${thread.name}", error)
    }
}
