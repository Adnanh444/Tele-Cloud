package com.adnan.memoracloud.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.adnan.memoracloud.data.local.entity.FaceEntity
import com.adnan.memoracloud.data.local.entity.FaceScanEntity
import com.adnan.memoracloud.data.local.entity.FileEntity

/** A stored signature with the group it belongs to, used when a new face needs comparing. */
data class StoredEmbedding(
    val personId: Long,
    val embedding: ByteArray
)

@Dao
interface FaceDao {

    /** Local images no scan has looked at yet. */
    @Query(
        "SELECT * FROM files WHERE category = 'IMAGE' AND trashedAt IS NULL " +
                "AND localPath IS NOT NULL AND id NOT IN (SELECT fileId FROM face_scans) " +
                "ORDER BY addedAt DESC LIMIT :limit"
    )
    suspend fun nextUnscanned(limit: Int): List<FileEntity>

    @Query(
        "SELECT COUNT(*) FROM files WHERE category = 'IMAGE' AND trashedAt IS NULL " +
                "AND localPath IS NOT NULL AND id NOT IN (SELECT fileId FROM face_scans)"
    )
    suspend fun unscannedCount(): Int

    @Query("SELECT COUNT(*) FROM face_scans")
    suspend fun scannedCount(): Int

    @Query("SELECT COUNT(*) FROM faces")
    suspend fun faceCount(): Int

    /** Most recent signatures across all groups; the caller caps how many it compares against. */
    @Query(
        "SELECT personId, embedding FROM faces WHERE personId IS NOT NULL " +
                "ORDER BY addedAt DESC LIMIT :limit"
    )
    suspend fun recentEmbeddings(limit: Int): List<StoredEmbedding>

    @Query("SELECT COUNT(DISTINCT personId) FROM faces WHERE personId IS NOT NULL")
    suspend fun personCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFaces(faces: List<FaceEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun markScanned(row: FaceScanEntity)

    @Query("SELECT cropPath FROM faces")
    suspend fun allCropPaths(): List<String>

    @Query("DELETE FROM faces WHERE fileId = :fileId")
    suspend fun deleteForFile(fileId: String)

    @Query("DELETE FROM faces")
    suspend fun clearFaces()

    @Query("DELETE FROM face_scans")
    suspend fun clearScans()
}
