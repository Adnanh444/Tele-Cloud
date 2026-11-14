package com.adnan.memoracloud.core.ai

import android.content.Context
import android.net.Uri
import com.adnan.memoracloud.core.common.SafeLog
import com.adnan.memoracloud.domain.ai.DetectedLabel
import com.adnan.memoracloud.domain.ai.ImageLabeler
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext

/**
 * Image tagging with an on-device model (the base labels are bundled in the dependency, so it works
 * offline and without Play Services). Nothing leaves the phone: the file is decoded locally, the
 * labels are written to the local database, and no network call is made here.
 */
class MlKitImageLabeler(
    private val context: Context
) : ImageLabeler {

    private val client by lazy {
        ImageLabeling.getClient(
            ImageLabelerOptions.Builder()
                .setConfidenceThreshold(MIN_CONFIDENCE)
                .build()
        )
    }

    override suspend fun label(path: String): List<DetectedLabel> = withContext(Dispatchers.IO) {
        val file = File(path)
        if (!file.exists() || file.length() == 0L) return@withContext emptyList()
        runCatching {
            val image = InputImage.fromFilePath(context, Uri.fromFile(file))
            client.process(image).await().map { label ->
                DetectedLabel(label.text.lowercase(), label.confidence)
            }
        }.onFailure { error ->
            SafeLog.w(TAG, "Could not label ${file.name}", error)
        }.getOrDefault(emptyList())
    }

    /** Bridges a Play Services task to coroutines without pulling in another dependency. */
    private suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { continuation ->
        addOnSuccessListener { value -> if (continuation.isActive) continuation.resume(value) }
        addOnFailureListener { error ->
            if (continuation.isActive) continuation.resumeWithException(error)
        }
    }

    private companion object {
        const val TAG = "MlKitImageLabeler"

        /** Below this the tags are noise ("structure", "material") rather than recognisable things. */
        const val MIN_CONFIDENCE = 0.55f
    }
}
