package com.adnan.memoracloud.domain.repository

import com.adnan.memoracloud.domain.model.FaceSample
import com.adnan.memoracloud.domain.model.Person
import com.adnan.memoracloud.domain.people.PeopleHealth
import kotlinx.coroutines.flow.Flow

/**
 * On-device face grouping. The analyzer in the Android module produces [FaceSample]s; this
 * repository decides which known person each one belongs to.
 */
interface PeopleRepository {

    fun observePeople(): Flow<List<Person>>

    suspend fun health(): PeopleHealth

    /** Photos on this device that no face scan has looked at yet, newest first. */
    suspend fun nextUnscanned(limit: Int): List<Pair<String, String>>

    /**
     * Stores the faces of one photo. Returns how many of them were added to an existing person,
     * which is also how a new group gets discovered: the rest start new people.
     */
    suspend fun record(fileId: String, faces: List<FaceSample>): Int

    /** Every stored face crop, so orphaned image files can be cleaned up. */
    suspend fun knownCropPaths(): List<String>

    suspend fun rename(personId: Long, name: String)

    /** Forgets every face and every group. The photos themselves are untouched. */
    suspend fun forgetAll()
}
