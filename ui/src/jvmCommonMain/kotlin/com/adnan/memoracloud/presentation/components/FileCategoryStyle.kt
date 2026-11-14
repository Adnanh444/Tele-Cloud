package com.adnan.memoracloud.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import org.jetbrains.compose.resources.stringResource
import com.adnan.memoracloud.resources.Res
import com.adnan.memoracloud.resources.common_category_archives
import com.adnan.memoracloud.resources.common_category_audio
import com.adnan.memoracloud.resources.common_category_documents
import com.adnan.memoracloud.resources.common_category_other
import com.adnan.memoracloud.resources.common_category_photos
import com.adnan.memoracloud.resources.common_category_videos
import com.adnan.memoracloud.domain.model.FileCategory
import com.adnan.memoracloud.presentation.theme.ChartPalette
import com.adnan.memoracloud.presentation.theme.harmonizedWithPrimary

@Composable
fun FileCategory.label(): String = stringResource(
    when (this) {
        FileCategory.IMAGE -> Res.string.common_category_photos
        FileCategory.VIDEO -> Res.string.common_category_videos
        FileCategory.AUDIO -> Res.string.common_category_audio
        FileCategory.DOCUMENT -> Res.string.common_category_documents
        FileCategory.ARCHIVE -> Res.string.common_category_archives
        FileCategory.OTHER -> Res.string.common_category_other
    }
)

@Composable
fun FileCategory.chartColor(): Color = when (this) {
    FileCategory.IMAGE -> ChartPalette.Blue
    FileCategory.VIDEO -> ChartPalette.Violet
    FileCategory.AUDIO -> ChartPalette.Teal
    FileCategory.DOCUMENT -> ChartPalette.Amber
    FileCategory.ARCHIVE -> ChartPalette.Rose
    FileCategory.OTHER -> ChartPalette.Slate
}.harmonizedWithPrimary()
