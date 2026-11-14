package com.adnan.memoracloud.presentation.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.adnan.memoracloud.domain.usecase.RestorePlan
import com.adnan.memoracloud.domain.usecase.RestoreScope
import com.adnan.memoracloud.presentation.common.Formatters
import com.adnan.memoracloud.resources.Res
import com.adnan.memoracloud.resources.common_cancel
import com.adnan.memoracloud.resources.restore_counting
import com.adnan.memoracloud.resources.restore_nothing_to_restore
import com.adnan.memoracloud.resources.restore_only_missing
import com.adnan.memoracloud.resources.restore_plan_missing
import com.adnan.memoracloud.resources.restore_plan_summary
import com.adnan.memoracloud.resources.restore_scope_audio
import com.adnan.memoracloud.resources.restore_scope_documents
import com.adnan.memoracloud.resources.restore_scope_everything
import com.adnan.memoracloud.resources.restore_scope_media
import com.adnan.memoracloud.resources.restore_start
import com.adnan.memoracloud.resources.settings_restore_from_telegram
import org.jetbrains.compose.resources.stringResource

/**
 * The restore wizard: pick what to bring back, see how much it is before anything starts, and skip
 * files that are already on the phone. Counts come from the index, so nothing is downloaded to
 * find them out.
 */
@Composable
fun RestoreDialog(
    plan: RestorePlan?,
    onScopeChange: (RestoreScope) -> Unit,
    onConfirm: (RestoreScope, Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var scope by remember { mutableStateOf(RestoreScope.EVERYTHING) }
    var onlyMissing by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.settings_restore_from_telegram)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                RestoreScope.entries.forEach { option ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                scope = option
                                onScopeChange(option)
                            }
                            .padding(vertical = 2.dp)
                    ) {
                        RadioButton(selected = scope == option, onClick = {
                            scope = option
                            onScopeChange(option)
                        })
                        Text(text = stringResource(option.label()), style = MaterialTheme.typography.bodyLarge)
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(Res.string.restore_only_missing),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    Switch(checked = onlyMissing, onCheckedChange = { onlyMissing = it })
                }

                val summary = when {
                    plan == null -> stringResource(Res.string.restore_counting)
                    plan.empty -> stringResource(Res.string.restore_nothing_to_restore)
                    else -> stringResource(
                        Res.string.restore_plan_summary,
                        plan.files,
                        Formatters.bytes(plan.bytes)
                    ) + " · " + stringResource(
                        Res.string.restore_plan_missing,
                        plan.missingFiles,
                        Formatters.bytes(plan.missingBytes)
                    )
                }
                Text(
                    text = summary,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = plan != null && !plan.empty && (!onlyMissing || !plan.nothingMissing),
                onClick = { onConfirm(scope, onlyMissing) }
            ) { Text(stringResource(Res.string.restore_start)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(Res.string.common_cancel)) }
        }
    )
}

private fun RestoreScope.label() = when (this) {
    RestoreScope.EVERYTHING -> Res.string.restore_scope_everything
    RestoreScope.MEDIA -> Res.string.restore_scope_media
    RestoreScope.DOCUMENTS -> Res.string.restore_scope_documents
    RestoreScope.AUDIO -> Res.string.restore_scope_audio
}
