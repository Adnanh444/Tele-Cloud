package com.adnan.memoracloud.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.adnan.memoracloud.resources.Res
import com.adnan.memoracloud.resources.common_cancel
import com.adnan.memoracloud.resources.common_save
import org.jetbrains.compose.resources.stringResource

/** One entry in a [MultiSwitchDialog]: a stable key, its label, and whether it is on. */
data class SwitchOption(
    val key: String,
    val label: String,
    val checked: Boolean
)

/**
 * A short list of independent on/off rows in a dialog. Changes apply immediately through [onToggle];
 * the buttons only close the dialog.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MultiSwitchDialog(
    title: String,
    options: List<SwitchOption>,
    onToggle: (key: String, checked: Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                options.forEach { option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onToggle(option.key, !option.checked) }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = option.label,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f)
                        )
                        Switch(
                            checked = option.checked,
                            onCheckedChange = { checked -> onToggle(option.key, checked) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                shapes = ButtonDefaults.shapes()
            ) { Text(stringResource(Res.string.common_save)) }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shapes = ButtonDefaults.shapes()
            ) { Text(stringResource(Res.string.common_cancel)) }
        }
    )
}
