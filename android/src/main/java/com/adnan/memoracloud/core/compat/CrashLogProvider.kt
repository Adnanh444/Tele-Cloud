package com.adnan.memoracloud.core.compat

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri

/**
 * Exists only to install [CrashHandler] before anything else runs. It is declared first in the
 * manifest, and content providers are created before `Application.onCreate`, so a crash inside
 * another provider (WorkManager's, for instance) is still written to disk.
 */
class CrashLogProvider : ContentProvider() {

    override fun onCreate(): Boolean {
        context?.let { CrashHandler.install(it.applicationContext) }
        return true
    }

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?
    ): Cursor? = null

    override fun getType(uri: Uri): String? = null

    override fun insert(uri: Uri, values: ContentValues?): Uri? = null

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?
    ): Int = 0
}
