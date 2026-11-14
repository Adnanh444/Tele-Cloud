package com.adnan.memoracloud.desktop.ui

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.adnan.memoracloud.desktop.BuildInfo
import com.adnan.memoracloud.desktop.files.DesktopFileRevealer
import com.adnan.memoracloud.presentation.platform.DeleteConsentLauncher
import com.adnan.memoracloud.presentation.platform.DeviceOwnerGate
import com.adnan.memoracloud.presentation.platform.FilePicker
import com.adnan.memoracloud.presentation.platform.FileSharer
import com.adnan.memoracloud.presentation.platform.FolderPicker
import com.adnan.memoracloud.presentation.platform.LocalAppIcon
import com.adnan.memoracloud.presentation.platform.LocalAppVersion
import com.adnan.memoracloud.presentation.platform.LocalDeleteConsentLauncher
import com.adnan.memoracloud.presentation.platform.LocalDeviceOwnerGate
import com.adnan.memoracloud.presentation.platform.LocalDownloadLocationConfigurable
import com.adnan.memoracloud.presentation.platform.LocalFilePicker
import com.adnan.memoracloud.presentation.platform.LocalFileRevealer
import com.adnan.memoracloud.presentation.platform.LocalFileSharer
import com.adnan.memoracloud.presentation.platform.LocalFolderPicker
import com.adnan.memoracloud.presentation.platform.LocalMultiFilePicker
import com.adnan.memoracloud.presentation.platform.LocalPermissionRequester
import com.adnan.memoracloud.presentation.platform.LocalPlatformCapabilities
import com.adnan.memoracloud.presentation.platform.LocalPlatformScreens
import com.adnan.memoracloud.presentation.platform.LocalSystemScreens
import com.adnan.memoracloud.presentation.platform.LocalTelegramLinkOpener
import com.adnan.memoracloud.presentation.platform.LocalUrlOpener
import com.adnan.memoracloud.presentation.platform.MultiFilePicker
import com.adnan.memoracloud.presentation.platform.PermissionRequester
import com.adnan.memoracloud.presentation.platform.PickResult
import com.adnan.memoracloud.presentation.platform.PlatformCapabilities
import com.adnan.memoracloud.presentation.platform.SystemScreens
import com.adnan.memoracloud.presentation.platform.UrlOpener
import com.adnan.memoracloud.resources.Res
import com.adnan.memoracloud.resources.ic_launcher
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.koinInject
import java.awt.Desktop
import java.awt.FileDialog
import java.awt.Frame
import java.io.File
import java.net.URI
import javax.swing.JFileChooser
import kotlin.concurrent.thread

@Composable
fun ProvideDesktopPlatformActions(content: @Composable () -> Unit) {
    val capabilities = koinInject<PlatformCapabilities>()
    val urlOpener = remember {
        UrlOpener { url ->
            runCatching { Desktop.getDesktop().browse(URI(url)) }
        }
    }
    val folderPicker = remember {
        FolderPicker { onPicked ->
            thread {
                val chooser = JFileChooser().apply {
                    fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
                }
                val result = if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                    PickResult.Picked(chooser.selectedFile.absolutePath)
                } else {
                    PickResult.Canceled
                }
                onPicked(result)
            }
        }
    }
    val filePicker = remember {
        FilePicker { onPicked ->
            thread {
                val dialog = FileDialog(null as Frame?, "", FileDialog.LOAD)
                dialog.isVisible = true
                val file = dialog.file
                val result = if (file == null) {
                    PickResult.Canceled
                } else {
                    PickResult.Picked(File(dialog.directory, file).absolutePath)
                }
                onPicked(result)
            }
        }
    }
    val multiFilePicker = remember {
        MultiFilePicker { onPicked ->
            thread {
                val dialog = FileDialog(null as Frame?, "", FileDialog.LOAD)
                dialog.isMultipleMode = true
                dialog.isVisible = true
                onPicked(dialog.files.orEmpty().map { it.absolutePath })
            }
        }
    }
    val fileSharer = remember {
        FileSharer { paths, _, _ ->
            paths.firstOrNull()?.let { path ->
                runCatching {
                    Desktop.getDesktop().open(File(path).parentFile)
                }
            }
        }
    }
    val deleteConsentLauncher = remember {
        DeleteConsentLauncher { _, onResult -> onResult(true) }
    }
    val fileRevealer = remember { DesktopFileRevealer() }
    val telegramLinkOpener = remember { DesktopTelegramLinkOpener() }
    val permissionRequester = remember {
        PermissionRequester { _, onDone -> onDone() }
    }
    val systemScreens = remember {
        object : SystemScreens {
            override fun openAppSettings() = Unit
            override fun openAllFilesAccess() = Unit
            override fun openBatteryOptimization() = Unit
        }
    }
    val deviceOwnerGate = remember {
        DeviceOwnerGate { _, _, _, onConfirmed -> onConfirmed() }
    }

    val appIcon: @Composable (Modifier) -> Unit = { modifier ->
        Image(
            painter = painterResource(Res.drawable.ic_launcher),
            contentDescription = null,
            modifier = modifier
        )
    }

    CompositionLocalProvider(
        LocalAppIcon provides appIcon,
        LocalUrlOpener provides urlOpener,
        LocalTelegramLinkOpener provides telegramLinkOpener,
        LocalFolderPicker provides folderPicker,
        LocalFilePicker provides filePicker,
        LocalMultiFilePicker provides multiFilePicker,
        LocalFileSharer provides fileSharer,
        LocalDeleteConsentLauncher provides deleteConsentLauncher,
        LocalPermissionRequester provides permissionRequester,
        LocalSystemScreens provides systemScreens,
        LocalDeviceOwnerGate provides deviceOwnerGate,
        LocalAppVersion provides BuildInfo.VERSION,
        LocalDownloadLocationConfigurable provides true,
        LocalPlatformScreens provides DesktopPlatformScreens,
        LocalFileRevealer provides fileRevealer,
        LocalPlatformCapabilities provides capabilities,
        content = content
    )
}
