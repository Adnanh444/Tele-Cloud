package com.adnan.memoracloud.data.repository

import com.adnan.memoracloud.core.common.SafeLog
import com.adnan.memoracloud.data.local.dao.FaceDao
import com.adnan.memoracloud.data.local.dao.PeopleDao
import com.adnan.memoracloud.data.local.entity.FaceEntity
import com.adnan.memoracloud.data.local.entity.FaceScanEntity
import com.adnan.memoracloud.data.local.entity.PersonEntity
import com.adnan.memoracloud.domain.model.FaceSample
import com.adnan.memoracloud.domain.model.Person
import com.adnan.memoracloud.domain.people.PeopleHealth
import com.adnan.memoracloud.domain.repository.PeopleRepository
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.UUID
import kotlin.math.sqrt
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Groups faces by comparing their signatures. A new face joins the closest known person when the
 * cosine similarity clears [ASSIGN_THRESHOLD]; otherwise it starts a new group called "Person N".
 *
 * The threshold is deliberately conservative: wrongly merging two people into one group makes the
 * group useless, while an extra group is only a small annoyance (two avatars to look at).
 */
class PeopleRepositoryImpl(
    private val peopleDao: PeopleDao,
    private val faceDao: FaceDao
) : PeopleRepository {

    override fun observePeople(): Flow<List<Person>> =
        peopleDao.observeSummaries().map { rows ->
            rows.map { row ->
                Person(
                    id = row.id,
                    name = row.name,
                    coverPath = row.coverPath,
                    faceCount = row.faceCount,
                    photoCount = row.photoCount
                )
            }
        }

    override suspend fun health(): PeopleHealth = PeopleHealth(
        people = peopleDao.count(),
        faces = faceDao.faceCount(),
        scannedPhotos = faceDao.scannedCount(),
        waitingPhotos = faceDao.unscannedCount()
    )

    override suspend fun nextUnscanned(limit: Int): List<Pair<String, String>> =
        faceDao.nextUnscanned(limit).mapNotNull { file ->
            file.localPath?.let { path -> file.id to path }
        }

    override suspend fun record(fileId: String, faces: List<FaceSample>): Int {
        val now = System.currentTimeMillis()
        if (faces.isEmpty()) {
            faceDao.markScanned(FaceScanEntity(fileId, 0, now))
            return 0
        }

        // Known signatures, newest first. Kept in memory and extended as this photo is processed so
        // two faces of the same unknown person in one picture do not create two groups.
        val known = faceDao.recentEmbeddings(MAX_COMPARISONS)
            .mapNotNull { row ->
                val vector = decode(row.embedding)
                if (vector.isEmpty()) null else row.personId to vector
            }
            .toMutableList()

        val rows = ArrayList<FaceEntity>(faces.size)
        var matched = 0

        for (face in faces) {
            val best = bestPerson(face.embedding, known)
            val personId = if (best != null && best.second >= ASSIGN_THRESHOLD) {
                matched++
                best.first
            } else {
                peopleDao.insert(
                    PersonEntity(
                        name = nextName(),
                        coverPath = null,
                        createdAt = now
                    )
                )
            }
            rows += FaceEntity(
                id = UUID.randomUUID().toString(),
                fileId = fileId,
                personId = personId,
                embedding = encode(face.embedding),
                cropPath = face.cropPath,
                score = face.score,
                addedAt = now
            )
            known.add(0, personId to face.embedding)
            if (peopleDao.byId(personId)?.coverPath == null) {
                peopleDao.setCover(personId, face.cropPath)
            }
        }

        faceDao.insertFaces(rows)
        faceDao.markScanned(FaceScanEntity(fileId, faces.size, now))
        SafeLog.d(TAG, "Photo $fileId: ${faces.size} face(s), $matched already known")
        return matched
    }

    override suspend fun knownCropPaths(): List<String> = faceDao.allCropPaths()

    override suspend fun rename(personId: Long, name: String) {
        val cleaned = name.trim().take(MAX_NAME)
        if (cleaned.isNotEmpty()) peopleDao.rename(personId, cleaned)
    }

    override suspend fun forgetAll() {
        faceDao.clearFaces()
        faceDao.clearScans()
        peopleDao.clear()
        SafeLog.d(TAG, "Forgot every face and group")
    }

    /** Highest similarity per person, so one person with many good examples scores well. */
    private fun bestPerson(embedding: FloatArray, known: List<Pair<Long, FloatArray>>): Pair<Long, Float>? {
        var bestId: Long? = null
        var bestScore = -1f
        for ((personId, vector) in known) {
            val score = similarity(embedding, vector)
            if (score > bestScore) {
                bestScore = score
                bestId = personId
            }
        }
        return bestId?.let { it to bestScore }
    }

    private fun similarity(a: FloatArray, b: FloatArray): Float {
        if (a.size != b.size || a.isEmpty()) return -1f
        var dot = 0f
        var na = 0f
        var nb = 0f
        for (i in a.indices) {
            dot += a[i] * b[i]
            na += a[i] * a[i]
            nb += b[i] * b[i]
        }
        if (na == 0f || nb == 0f) return -1f
        return dot / (sqrt(na) * sqrt(nb))
    }

    private suspend fun nextName(): String = "Person ${peopleDao.count() + 1}"

    private fun encode(vector: FloatArray): ByteArray {
        val buffer = ByteBuffer.allocate(vector.size * Float.SIZE_BYTES)
            .order(ByteOrder.LITTLE_ENDIAN)
        vector.forEach { buffer.putFloat(it) }
        return buffer.array()
    }

    private fun decode(bytes: ByteArray): FloatArray {
        if (bytes.size % Float.SIZE_BYTES != 0) return FloatArray(0)
        val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        return FloatArray(bytes.size / Float.SIZE_BYTES) { buffer.float }
    }

    private companion object {
        const val TAG = "PeopleRepository"

        /** Similarity above which a face joins an existing person. */
        const val ASSIGN_THRESHOLD = 0.62f

        /** How many stored signatures one photo is compared against. */
        const val MAX_COMPARISONS = 600

        const val MAX_NAME = 40
    }
}
