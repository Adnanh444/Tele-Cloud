package com.adnan.memoracloud.core.people

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import com.adnan.memoracloud.core.common.SafeLog
import com.adnan.memoracloud.domain.model.FaceSample
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.google.mlkit.vision.face.FaceLandmark
import java.io.File
import java.util.UUID
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.hypot
import kotlin.math.max
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.tensorflow.lite.Interpreter
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Finds faces with ML Kit's bundled detector and turns each one into a 192-number signature with a
 * MobileFaceNet model bundled in the APK. Everything runs on the phone: the bitmap is decoded
 * locally, no image, crop or signature is ever uploaded.
 *
 * The model takes two faces per run ([2,112,112,3] in, [2,192] out), so faces are processed in
 * pairs; an odd face out is paired with itself and the duplicate result thrown away.
 */
class FaceAnalyzer(
    private val context: Context
) {

    private val detector by lazy {
        FaceDetection.getClient(
            FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
                .setContourMode(FaceDetectorOptions.CONTOUR_MODE_NONE)
                .setMinFaceSize(MIN_FACE_SIZE)
                .build()
        )
    }

    private val interpreter: Interpreter? by lazy { loadInterpreter() }

    suspend fun analyze(path: String): List<FaceSample> = withContext(Dispatchers.Default) {
        val source = decode(path)
        if (source == null) {
            SafeLog.w(TAG, "Could not decode $path")
            return@withContext emptyList()
        }
        try {
            val detected = runCatching {
                detector.process(InputImage.fromBitmap(source, 0)).await()
            }.onFailure { error ->
                SafeLog.w(TAG, "Face detection failed for $path", error)
            }.getOrDefault(emptyList())

            val faces = detected
                .sortedByDescending { it.boundingBox.width().toLong() * it.boundingBox.height() }
                .take(MAX_FACES_PER_PHOTO)
            if (faces.isEmpty()) return@withContext emptyList()

            val engine = interpreter ?: run {
                SafeLog.w(TAG, "Face model missing, cannot build signatures")
                return@withContext emptyList()
            }

            // Crop first, then embed in pairs: the model's input is a fixed batch of two.
            val crops = faces.mapNotNull { face -> cropFor(source, face)?.let { crop -> face to crop } }
            val samples = ArrayList<FaceSample>(crops.size)
            var index = 0
            while (index < crops.size) {
                val first = crops[index]
                val second = crops.getOrNull(index + 1) ?: first
                val pair = embed(engine, first.second, second.second)
                if (pair.isNotEmpty()) {
                    samples += toSample(first.first, first.second, pair.first())
                }
                if (index + 1 < crops.size && pair.size > 1) {
                    samples += toSample(second.first, second.second, pair[1])
                }
                index += 2
            }
            samples
        } finally {
            source.recycle()
        }
    }

    /** Removes crops whose face row is gone, so the folder does not grow forever. */
    fun pruneCrops(keep: Set<String>) {
        val dir = File(context.filesDir, CROP_DIR)
        val files = dir.listFiles() ?: return
        files.forEach { file ->
            if (file.absolutePath !in keep) runCatching { file.delete() }
        }
    }

    private fun toSample(face: Face, crop: Bitmap, embedding: FloatArray): FaceSample {
        val box = face.boundingBox
        val cropPath = saveCrop(crop)
        return FaceSample(
            embedding = normalize(embedding),
            cropPath = cropPath,
            left = box.left,
            top = box.top,
            right = box.right,
            bottom = box.bottom,
            score = 1f
        )
    }

    /**
     * Square crop centred on the eyes when both are visible (that is what the model was trained
     * on), otherwise an expanded square around the detection box.
     */
    private fun cropFor(source: Bitmap, face: Face): Bitmap? {
        val leftEye = face.getLandmark(FaceLandmark.LEFT_EYE)?.position
        val rightEye = face.getLandmark(FaceLandmark.RIGHT_EYE)?.position
        val box = face.boundingBox
        val centerX: Float
        val centerY: Float
        val side: Float
        if (leftEye != null && rightEye != null) {
            centerX = (leftEye.x + rightEye.x) / 2f
            centerY = (leftEye.y + rightEye.y) / 2f
            val eyeDistance = hypot(leftEye.x - rightEye.x, leftEye.y - rightEye.y)
            side = max(eyeDistance * 3.6f, box.width().toFloat())
        } else {
            centerX = box.exactCenterX()
            centerY = box.exactCenterY()
            side = max(box.width(), box.height()).toFloat() * 1.35f
        }
        val half = side / 2f
        val rect = Rect(
            (centerX - half).toInt().coerceAtLeast(0),
            (centerY - half).toInt().coerceAtLeast(0),
            (centerX + half).toInt().coerceAtMost(source.width),
            (centerY + half).toInt().coerceAtMost(source.height)
        )
        if (rect.width() < MIN_CROP_PIXELS || rect.height() < MIN_CROP_PIXELS) return null
        val cropped = Bitmap.createBitmap(source, rect.left, rect.top, rect.width(), rect.height())
        val scaled = Bitmap.createBitmap(MODEL_SIZE, MODEL_SIZE, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(scaled)
        canvas.drawBitmap(
            cropped,
            Rect(0, 0, cropped.width, cropped.height),
            Rect(0, 0, MODEL_SIZE, MODEL_SIZE),
            Paint(Paint.FILTER_BITMAP_FLAG)
        )
        if (cropped !== scaled) cropped.recycle()
        return scaled
    }

    /** Feeds one batch of two, returns [2,192] flattened as a list of embeddings. */
    private fun embed(engine: Interpreter, first: Bitmap, second: Bitmap): List<FloatArray> {
        val input = ByteBuffer
            .allocateDirect(BATCH * MODEL_SIZE * MODEL_SIZE * 3 * Float.SIZE_BYTES)
            .order(ByteOrder.nativeOrder())
        fill(input, first)
        fill(input, second)
        input.rewind()
        val output = Array(BATCH) { FloatArray(EMBEDDING_SIZE) }
        return runCatching {
            engine.run(input, output)
            output.toList()
        }.onFailure { error ->
            SafeLog.w(TAG, "Embedding failed", error)
        }.getOrDefault(emptyList())
    }

    private fun fill(buffer: ByteBuffer, bitmap: Bitmap) {
        val pixels = IntArray(MODEL_SIZE * MODEL_SIZE)
        bitmap.getPixels(pixels, 0, MODEL_SIZE, 0, 0, MODEL_SIZE, MODEL_SIZE)
        pixels.forEach { pixel ->
            val r = (pixel shr 16) and 0xFF
            val g = (pixel shr 8) and 0xFF
            val b = pixel and 0xFF
            buffer.putFloat((r - MEAN) / SCALE)
            buffer.putFloat((g - MEAN) / SCALE)
            buffer.putFloat((b - MEAN) / SCALE)
        }
    }

    private fun normalize(vector: FloatArray): FloatArray {
        var sum = 0f
        vector.forEach { sum += it * it }
        val length = kotlin.math.sqrt(sum)
        if (length == 0f) return vector
        return FloatArray(vector.size) { vector[it] / length }
    }

    private fun saveCrop(bitmap: Bitmap): String {
        val dir = File(context.filesDir, CROP_DIR).apply { mkdirs() }
        val file = File(dir, "${UUID.randomUUID()}.jpg")
        runCatching {
            file.outputStream().use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, CROP_QUALITY, out)
            }
        }.onFailure { error -> SafeLog.w(TAG, "Could not save face crop", error) }
        return file.absolutePath
    }

    private fun decode(path: String): Bitmap? = runCatching {
        val file = File(path)
        if (!file.exists() || file.length() == 0L) return null
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, bounds)
        val longest = max(bounds.outWidth, bounds.outHeight)
        if (longest <= 0) return null
        var sample = 1
        while (longest / sample > MAX_DECODE) sample *= 2
        val options = BitmapFactory.Options().apply {
            inSampleSize = sample
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        BitmapFactory.decodeFile(path, options)
    }.getOrNull()

    private fun loadInterpreter(): Interpreter? = runCatching {
        val bytes = context.assets.open(MODEL_ASSET).use { it.readBytes() }
        val buffer = ByteBuffer.allocateDirect(bytes.size).order(ByteOrder.nativeOrder())
        buffer.put(bytes)
        buffer.rewind()
        Interpreter(buffer, Interpreter.Options().apply { setNumThreads(2) })
    }.onFailure { error ->
        SafeLog.w(TAG, "Could not load $MODEL_ASSET", error)
    }.getOrNull()

    private suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { continuation ->
        addOnSuccessListener { value -> if (continuation.isActive) continuation.resume(value) }
        addOnFailureListener { error ->
            if (continuation.isActive) continuation.resumeWithException(error)
        }
    }

    private companion object {
        const val TAG = "FaceAnalyzer"
        const val MODEL_ASSET = "models/MobileFaceNet.tflite"
        const val CROP_DIR = "faces"
        const val MODEL_SIZE = 112
        const val BATCH = 2
        const val EMBEDDING_SIZE = 192
        const val MIN_FACE_SIZE = 0.06f
        const val MIN_CROP_PIXELS = 24
        const val MAX_FACES_PER_PHOTO = 8
        const val MAX_DECODE = 1600
        const val CROP_QUALITY = 88

        /** Pre-processing the bundled MobileFaceNet was exported with: (pixel - 127.5) / 128. */
        const val MEAN = 127.5f
        const val SCALE = 128f
    }
}
