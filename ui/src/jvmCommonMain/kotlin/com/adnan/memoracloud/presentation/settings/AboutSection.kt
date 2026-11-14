package com.adnan.memoracloud.presentation.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Update
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.MutableStateFlow
import com.adnan.memoracloud.resources.Res
import com.adnan.memoracloud.resources.about_auto_check
import com.adnan.memoracloud.resources.about_auto_check_summary
import com.adnan.memoracloud.resources.about_check_updates
import com.adnan.memoracloud.resources.about_check_updates_summary
import com.adnan.memoracloud.resources.about_checking
import com.adnan.memoracloud.resources.about_developer
import com.adnan.memoracloud.resources.about_license
import com.adnan.memoracloud.resources.about_upstream
import com.adnan.memoracloud.resources.about_upstream_summary
import com.adnan.memoracloud.resources.about_section_project
import com.adnan.memoracloud.resources.about_section_updates
import com.adnan.memoracloud.resources.about_source_code
import com.adnan.memoracloud.resources.about_source_code_summary
import com.adnan.memoracloud.resources.about_tagline
import com.adnan.memoracloud.resources.about_update_ready
import com.adnan.memoracloud.resources.about_version
import com.adnan.memoracloud.resources.app_name
import com.adnan.memoracloud.core.update.AppLinks
import com.adnan.memoracloud.presentation.platform.LocalAppIcon
import com.adnan.memoracloud.presentation.platform.LocalAppVersion
import com.adnan.memoracloud.presentation.platform.LocalUrlOpener
import com.adnan.memoracloud.presentation.platform.LocalUpdateInstaller
import com.adnan.memoracloud.presentation.platform.UpdateInstallState
import com.adnan.memoracloud.presentation.components.UpdateDialog

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AboutSection(state: SettingsUiState, viewModel: SettingsViewModel) {
    val urlOpener = LocalUrlOpener.current
    val updateState by viewModel.updateState.collectAsStateWithLifecycle()
    val checking = updateState is UpdateState.Checking

    // Android supplies the installer; the desktop builds leave this null and simply open the
    // release page instead.
    val installer = LocalUpdateInstaller.current
    val installState by remember(installer) {
        installer?.state ?: MutableStateFlow<UpdateInstallState>(UpdateInstallState.Idle)
    }.collectAsStateWithLifecycle()

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        LocalAppIcon.current(
            Modifier
                .size(88.dp)
                .clip(MaterialTheme.shapes.extraLarge)
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = stringResource(Res.string.app_name),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = stringResource(Res.string.about_version, LocalAppVersion.current),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = stringResource(Res.string.about_tagline),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Text(
            text = stringResource(Res.string.about_developer),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Text(
            text = stringResource(Res.string.about_license),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }

    SettingsSectionTitle(stringResource(Res.string.about_section_updates))
    SettingsGroup {
        add {
            SettingsClickRow(
                title = stringResource(Res.string.about_check_updates),
                icon = Icons.Filled.Update,
                subtitle = when {
                    // A downloaded, verified APK outranks everything else: the row becomes the
                    // Install button, even if the check result was forgotten.
                    installState is UpdateInstallState.ReadyToInstall ->
                        stringResource(Res.string.about_update_ready_install)

                    installState is UpdateInstallState.Downloading -> stringResource(
                        Res.string.about_update_downloading,
                        ((installState as UpdateInstallState.Downloading).fraction * 100).toInt()
                    )

                    installState is UpdateInstallState.Verifying ->
                        stringResource(Res.string.about_update_verifying)

                    else -> when (val current = updateState) {
                        is UpdateState.Available -> stringResource(
                            Res.string.about_update_ready,
                            current.release.version
                        )

                        is UpdateState.Message -> current.text
                        UpdateState.Checking -> stringResource(Res.string.about_checking)
                        UpdateState.Idle -> stringResource(Res.string.about_check_updates_summary)
                    }
                },
                onClick = {
                    when {
                        installState is UpdateInstallState.ReadyToInstall -> installer?.install()
                        !checking -> viewModel.checkForUpdates()
                    }
                },
                trailing = if (checking || installState is UpdateInstallState.Downloading) {
                    { LoadingIndicator(modifier = Modifier.size(24.dp)) }
                } else null
            )
        }
        add {
            SettingsSwitchRow(
                title = stringResource(Res.string.about_auto_check),
                icon = Icons.Filled.Autorenew,
                subtitle = stringResource(Res.string.about_auto_check_summary),
                checked = state.preferences.updateCheckEnabled,
                onChange = { value ->
                    viewModel.update { it.copy(updateCheckEnabled = value) }
                }
            )
        }
    }

    SettingsSectionTitle(stringResource(Res.string.about_section_project))
    SettingsGroup {
        add {
            SettingsClickRow(
                title = stringResource(Res.string.about_source_code),
                icon = Icons.Filled.Code,
                subtitle = stringResource(Res.string.about_source_code_summary),
                onClick = { urlOpener.open(AppLinks.REPOSITORY) }
            )
        }
        add {
            SettingsClickRow(
                title = stringResource(Res.string.about_upstream),
                icon = Icons.Filled.Info,
                subtitle = stringResource(Res.string.about_upstream_summary),
                onClick = { urlOpener.open(AppLinks.UPSTREAM) }
            )
        }
    }

    (updateState as? UpdateState.Available)?.let { available ->
        val release = available.release
        val supported = installer != null && installer.supported && release.canInstallInApp
        UpdateDialog(
            currentVersion = LocalAppVersion.current,
            release = release,
            installState = installState,
            inAppInstallSupported = supported,
            onOpenUrl = urlOpener::open,
            onDownload = {
                if (supported) {
                    installer?.download(release)
                } else {
                    viewModel.dismissUpdate()
                    urlOpener.open(release.pageUrl)
                }
            },
            onInstall = { installer?.install() },
            onCancelDownload = { installer?.cancel() },
            onDismiss = viewModel::dismissUpdate
        )
    }
}
