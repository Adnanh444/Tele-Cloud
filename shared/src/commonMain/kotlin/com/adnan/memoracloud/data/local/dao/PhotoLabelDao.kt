package com.adnan.memoracloud.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.adnan.memoracloud.data.local.entity.FileEntity
import com.adnan.memoracloud.data.local.entity.PhotoLabelEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PhotoLabelDao {

    @Query("SELECT * FROM photo_labels WHERE fileId = :fileId")
    suspend fun byFile(fileId: String): PhotoLabelEntity?

    @Query("SELECT * FROM photo_labels WHERE fileId = :fileId")
    fun observe(fileId: String): Flow<PhotoLabelEntity?>

    /** Local images that are still on the device and have no tags yet. */
    @Query(
        "SELECT * FROM files WHERE category = 'IMAGE' AND trashedAt IS NULL " +
                "AND localPath IS NOT NULL AND id NOT IN (SELECT fileId FROM photo_labels) " +
                "ORDER BY addedAt DESC LIMIT :limit"
    )
    suspend fun nextUnlabelled(limit: Int): List<FileEntity>

    @Query(
        "SELECT COUNT(*) FROM files WHERE category = 'IMAGE' AND trashedAt IS NULL " +
                "AND localPath IS NOT NULL AND id NOT IN (SELECT fileId FROM photo_labels)"
    )
    suspend fun pendingCount(): Int

    @Query("SELECT COUNT(*) FROM photo_labels")
    suspend fun labelledCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(rows: List<PhotoLabelEntity>)

    @Query("DELETE FROM photo_labels WHERE fileId = :fileId")
    suspend fun deleteForFile(fileId: String)

    @Query("DELETE FROM photo_labels")
    suspend fun clear()

    @Query("SELECT labels FROM photo_labels")
    suspend fun allLabels(): List<String>
}
