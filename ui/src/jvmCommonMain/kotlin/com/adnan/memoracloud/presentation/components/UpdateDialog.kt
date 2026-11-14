package com.adnan.memoracloud.presentation.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.unit.dp
import com.adnan.memoracloud.resources.Res
import com.adnan.memoracloud.resources.about_update_download
import com.adnan.memoracloud.resources.about_update_from
import com.adnan.memoracloud.resources.about_update_later
import com.adnan.memoracloud.resources.about_update_title
import com.adnan.memoracloud.core.update.AppRelease
import com.adnan.memoracloud.presentation.common.MarkdownText

@Composable
fun UpdateDialog(
    release: AppRelease,
    currentVersion: String,
    onOpenUrl: (String) -> Unit,
    onDownload: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.about_update_title, release.version)) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    text = stringResource(
                        Res.string.about_update_from,
                        currentVersion,
                        release.version
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (release.notes.isNotBlank()) {
                    Spacer(Modifier.height(12.dp))
                    MarkdownText(
                        text = release.notes,
                        onOpenUrl = onOpenUrl
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = onDownload, shapes = ButtonDefaults.shapes()) {
                Text(stringResource(Res.string.about_update_download))
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss, shapes = ButtonDefaults.shapes()) {
                Text(stringResource(Res.string.about_update_later))
            }
        }
    )
}
