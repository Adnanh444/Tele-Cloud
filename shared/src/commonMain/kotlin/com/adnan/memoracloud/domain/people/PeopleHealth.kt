package com.adnan.memoracloud.domain.people

/** Simple progress numbers for the Settings screen. */
data class PeopleHealth(
    val people: Int,
    val faces: Int,
    val scannedPhotos: Int,
    val waitingPhotos: Int
) {
    val hasWork: Boolean get() = waitingPhotos > 0
}
