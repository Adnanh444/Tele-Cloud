package com.adnan.memoracloud.presentation.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.adnan.memoracloud.core.update.AppRelease
import com.adnan.memoracloud.presentation.common.MarkdownText
import com.adnan.memoracloud.presentation.platform.UpdateFailure
import com.adnan.memoracloud.presentation.platform.UpdateInstallState
import com.adnan.memoracloud.resources.Res
import com.adnan.memoracloud.resources.about_update_cancel_download
import com.adnan.memoracloud.resources.about_update_download
import com.adnan.memoracloud.resources.about_update_downloading
import com.adnan.memoracloud.resources.about_update_failed_checksum
import com.adnan.memoracloud.resources.about_update_failed_network
import com.adnan.memoracloud.resources.about_update_failed_permission
import com.adnan.memoracloud.resources.about_update_failed_signature
import com.adnan.memoracloud.resources.about_update_failed_storage
import com.adnan.memoracloud.resources.about_update_failed_unknown
import com.adnan.memoracloud.resources.about_update_from
import com.adnan.memoracloud.resources.about_update_install
import com.adnan.memoracloud.resources.about_update_later
import com.adnan.memoracloud.resources.about_update_ready_install
import com.adnan.memoracloud.resources.about_update_retry
import com.adnan.memoracloud.resources.about_update_title
import com.adnan.memoracloud.resources.about_update_verifying
import org.jetbrains.compose.resources.stringResource

@Composable
fun UpdateDialog(
    release: AppRelease,
    currentVersion: String,
    installState: UpdateInstallState,
    inAppInstallSupported: Boolean,
    onOpenUrl: (String) -> Unit,
    onDownload: () -> Unit,
    onInstall: () -> Unit,
    onCancelDownload: () -> Unit,
    onDismiss: () -> Unit
) {
    val downloading = installState as? UpdateInstallState.Downloading
    val verifying = installState is UpdateInstallState.Verifying
    val ready = installState is UpdateInstallState.ReadyToInstall
    val failed = installState as? UpdateInstallState.Failed
    val busy = downloading != null || verifying

    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
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

                if (downloading != null) {
                    Spacer(Modifier.height(16.dp))
                    if (downloading.isDeterminate) {
                        LinearProgressIndicator(
                            progress = { downloading.fraction },
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = stringResource(
                            Res.string.about_update_downloading,
                            (downloading.fraction * 100).toInt()
                        ),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                if (verifying) {
                    Spacer(Modifier.height(16.dp))
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = stringResource(Res.string.about_update_verifying),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                if (ready) {
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = stringResource(Res.string.about_update_ready_install),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                if (failed != null) {
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = stringResource(failed.reason.messageRes()),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }

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
            when {
                ready -> Button(
                    onClick = onInstall,
                    shapes = ButtonDefaults.shapes()
                ) { Text(stringResource(Res.string.about_update_install)) }

                downloading != null -> OutlinedButton(
                    onClick = onCancelDownload,
                    shapes = ButtonDefaults.shapes()
                ) { Text(stringResource(Res.string.about_update_cancel_download)) }

                verifying -> Button(
                    onClick = {},
                    enabled = false,
                    shapes = ButtonDefaults.shapes()
                ) { Text(stringResource(Res.string.about_update_verifying)) }

                failed != null -> {
                    val needsPermission =
                        failed.reason == UpdateFailure.INSTALL_PERMISSION && inAppInstallSupported
                    Button(
                        onClick = when {
                            needsPermission -> onInstall
                            inAppInstallSupported -> onDownload
                            else -> onCancelDownload
                        },
                        shapes = ButtonDefaults.shapes()
                    ) {
                        Text(
                            stringResource(
                                when {
                                    needsPermission -> Res.string.about_update_install
                                    inAppInstallSupported -> Res.string.about_update_retry
                                    else -> Res.string.about_update_later
                                }
                            )
                        )
                    }
                }

                else -> Button(
                    onClick = onDownload,
                    shapes = ButtonDefaults.shapes()
                ) { Text(stringResource(Res.string.about_update_download)) }
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                enabled = !busy,
                shapes = ButtonDefaults.shapes()
            ) { Text(stringResource(Res.string.about_update_later)) }
        }
    )
}

internal fun UpdateFailure.messageRes() = when (this) {
    UpdateFailure.NETWORK -> Res.string.about_update_failed_network
    UpdateFailure.CHECKSUM -> Res.string.about_update_failed_checksum
    UpdateFailure.SIGNATURE -> Res.string.about_update_failed_signature
    UpdateFailure.INSTALL_PERMISSION -> Res.string.about_update_failed_permission
    UpdateFailure.STORAGE -> Res.string.about_update_failed_storage
    UpdateFailure.UNKNOWN -> Res.string.about_update_failed_unknown
}
