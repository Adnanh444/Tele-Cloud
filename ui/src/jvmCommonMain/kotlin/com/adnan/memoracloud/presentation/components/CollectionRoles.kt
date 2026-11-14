package com.adnan.memoracloud.presentation.components

import androidx.compose.ui.semantics.SemanticsPropertyReceiver

expect fun SemanticsPropertyReceiver.collectionRole(name: String, grid: Boolean)

expect fun SemanticsPropertyReceiver.tileRole()
