package com.adnan.memoracloud.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.adnan.memoracloud.data.local.entity.PersonEntity
import kotlinx.coroutines.flow.Flow

/** One row of the People list: a person plus the counts the grid shows. */
data class PersonSummary(
    val id: Long,
    val name: String,
    val coverPath: String?,
    val faceCount: Int,
    val photoCount: Int
)

@Dao
interface PeopleDao {

    @Query(
        "SELECT people.id AS id, people.name AS name, people.coverPath AS coverPath, " +
                "COUNT(faces.id) AS faceCount, COUNT(DISTINCT faces.fileId) AS photoCount " +
                "FROM people LEFT JOIN faces ON faces.personId = people.id " +
                "GROUP BY people.id, people.name, people.coverPath " +
                "ORDER BY people.name COLLATE NOCASE"
    )
    fun observeSummaries(): Flow<List<PersonSummary>>

    @Query("SELECT * FROM people WHERE id = :id")
    suspend fun byId(id: Long): PersonEntity?

    @Insert
    suspend fun insert(person: PersonEntity): Long

    @Query("UPDATE people SET name = :name WHERE id = :id")
    suspend fun rename(id: Long, name: String)

    @Query("UPDATE people SET coverPath = :path WHERE id = :id")
    suspend fun setCover(id: Long, path: String?)

    @Query("SELECT COUNT(*) FROM people")
    suspend fun count(): Int

    @Query("DELETE FROM people")
    suspend fun clear()
}
