package com.adnan.memoracloud.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.adnan.memoracloud.domain.model.DeleteScope
import com.adnan.memoracloud.resources.Res
import com.adnan.memoracloud.resources.common_cancel
import com.adnan.memoracloud.resources.files_delete_scope_both
import com.adnan.memoracloud.resources.files_delete_scope_both_summary
import com.adnan.memoracloud.resources.files_delete_scope_cloud
import com.adnan.memoracloud.resources.files_delete_scope_cloud_summary
import com.adnan.memoracloud.resources.files_delete_scope_phone
import com.adnan.memoracloud.resources.files_delete_scope_phone_summary
import com.adnan.memoracloud.resources.files_delete_scope_title
import org.jetbrains.compose.resources.stringResource

/**
 * Deleting never happens straight away: the user picks which copy goes, and every choice leaves a
 * way back — Trash for 30 days, or a download from Telegram.
 */
@Composable
fun DeleteScopeDialog(
    count: Int,
    onSelect: (DeleteScope) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.files_delete_scope_title, count)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                DeleteScopeRow(
                    title = stringResource(Res.string.files_delete_scope_both),
                    summary = stringResource(Res.string.files_delete_scope_both_summary),
                    onClick = { onSelect(DeleteScope.BOTH) }
                )
                DeleteScopeRow(
                    title = stringResource(Res.string.files_delete_scope_cloud),
                    summary = stringResource(Res.string.files_delete_scope_cloud_summary),
                    onClick = { onSelect(DeleteScope.CLOUD) }
                )
                DeleteScopeRow(
                    title = stringResource(Res.string.files_delete_scope_phone),
                    summary = stringResource(Res.string.files_delete_scope_phone_summary),
                    onClick = { onSelect(DeleteScope.PHONE) }
                )
            }
        },
        confirmButton = {},
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text(stringResource(Res.string.common_cancel))
            }
        }
    )
}

@Composable
private fun DeleteScopeRow(
    title: String,
    summary: String,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = summary,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
