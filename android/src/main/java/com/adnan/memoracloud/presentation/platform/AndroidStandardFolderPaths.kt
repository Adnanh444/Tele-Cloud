package com.adnan.memoracloud.presentation.platform

import com.adnan.memoracloud.core.files.StandardBackupFolder

class AndroidStandardFolderPaths : StandardFolderPaths {

    override val camera: String
        get() = StandardBackupFolder.CAMERA.path

    override val pictures: String
        get() = StandardBackupFolder.PICTURES.path

    override val movies: String
        get() = StandardBackupFolder.MOVIES.path
}
