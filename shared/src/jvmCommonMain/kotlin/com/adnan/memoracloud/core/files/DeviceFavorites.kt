package com.adnan.memoracloud.core.files

/**
 * Keeps the app's star and the phone's own "favourite" mark in step.
 *
 * Android stores a favourite flag per media row in MediaStore (`IS_FAVORITE`, API 30+), which is what
 * the system gallery shows. Marking a file here makes it show up as favourite in the phone's gallery
 * too, and [favoritePaths] reads the flag back so a favourite picked in the gallery can be brought
 * into the app.
 */
interface DeviceFavorites {

    /** No-op for platforms without MediaStore (desktop builds). */
    object None : DeviceFavorites {
        override fun setFavorite(absolutePath: String, favorite: Boolean) = Unit

        override fun favoritePaths(): List<String> = emptyList()
    }

    fun setFavorite(absolutePath: String, favorite: Boolean)

    /** Absolute paths of media the device currently marks as favourite. */
    fun favoritePaths(): List<String>
}
