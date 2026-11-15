package com.adnan.memoracloud.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.adnan.memoracloud.core.common.AppResult
import com.adnan.memoracloud.core.files.DeleteConsentRequest
import com.adnan.memoracloud.core.logging.DebugLogRecorder
import com.adnan.memoracloud.core.permissions.PermissionChecker
import com.adnan.memoracloud.core.security.AppLockManager
import com.adnan.memoracloud.core.telegram.TelegramClient
import com.adnan.memoracloud.core.telegram.TelegramUser
import com.adnan.memoracloud.core.transfer.MaintenanceScheduler
import com.adnan.memoracloud.core.transfer.TransferScheduler
import com.adnan.memoracloud.core.update.UpdateCheck
import com.adnan.memoracloud.core.update.UpdateChecker
import com.adnan.memoracloud.data.repository.LocalDataWiper
import com.adnan.memoracloud.domain.model.BackupTrigger
import com.adnan.memoracloud.domain.model.UserPreferences
import com.adnan.memoracloud.domain.repository.BackupRepository
import com.adnan.memoracloud.domain.repository.CacheRepository
import com.adnan.memoracloud.domain.repository.ChannelRepository
import com.adnan.memoracloud.domain.repository.FileRepository
import com.adnan.memoracloud.domain.repository.KeyBackupRepository
import com.adnan.memoracloud.domain.repository.SettingsRepository
import com.adnan.memoracloud.domain.usecase.RestoreFilesUseCase
import com.adnan.memoracloud.domain.usecase.RestorePlan
import com.adnan.memoracloud.domain.usecase.RestoreScope
import com.adnan.memoracloud.resources.restore_nothing_to_do
import com.adnan.memoracloud.resources.restore_queued
import com.adnan.memoracloud.domain.repository.SyncRepository
import com.adnan.memoracloud.domain.repository.TelegramAuthRepository
import com.adnan.memoracloud.presentation.common.UiText
import com.adnan.memoracloud.presentation.common.toUiText
import com.adnan.memoracloud.presentation.files.ShareRequest
import com.adnan.memoracloud.resources.Res
import com.adnan.memoracloud.resources.backup_cleanup_done
import com.adnan.memoracloud.resources.about_no_update
import com.adnan.memoracloud.resources.files_removed_local_copies
import com.adnan.memoracloud.resources.message_backing_up_new_folder
import com.adnan.memoracloud.resources.message_cache_cleared
import com.adnan.memoracloud.resources.message_key_backup_saved
import com.adnan.memoracloud.resources.message_key_restored
import com.adnan.memoracloud.resources.message_rebuild_done
import com.adnan.memoracloud.resources.message_thumbnails_cleared
import com.adnan.memoracloud.resources.message_wrong_passphrase
import com.adnan.memoracloud.resources.rebuild_locked_files
import com.adnan.memoracloud.resources.settings_debug_log_empty
import com.adnan.memoracloud.resources.settings_free_up_none
import com.adnan.memoracloud.resources.settings_nothing_to_remove
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val telegramAuthRepository: TelegramAuthRepository,
    private val telegramClient: TelegramClient,
    private val cacheRepository: CacheRepository,
    private val fileRepository: FileRepository,
    private val keyBackupRepository: KeyBackupRepository,
    private val syncRepository: SyncRepository,
    private val localDataWiper: LocalDataWiper,
    private val channelRepository: ChannelRepository,
    private val backupRepository: BackupRepository,
    private val appLockManager: AppLockManager,
    private val maintenanceScheduler: MaintenanceScheduler,
    private val transferScheduler: TransferScheduler,
    val permissionChecker: PermissionChecker,
    private val updateChecker: UpdateChecker,
    private val restoreFiles: RestoreFilesUseCase,
    private val debugLogRecorder: DebugLogRecorder,
    private val sidecarCleanup: com.adnan.memoracloud.core.transfer.SidecarCleanup,
) : ViewModel() {

    private val user = MutableStateFlow<TelegramUser?>(null)

    private val _updateState = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val updateState: StateFlow<UpdateState> = _updateState.asStateFlow()

    private val _messages = MutableSharedFlow<UiText>(extraBufferCapacity = 8)
    val messages = _messages.asSharedFlow()

    private val _shareRequests = MutableSharedFlow<ShareRequest>(extraBufferCapacity = 1)
    val shareRequests = _shareRequests.asSharedFlow()

    private val _restorePlan = MutableStateFlow<RestorePlan?>(null)
    /** Null while the counts are being read from the index. */
    val restorePlan: StateFlow<RestorePlan?> = _restorePlan.asStateFlow()

    private val _sidecarCount = MutableStateFlow<Int?>(null)
    /** How many Takeout sidecars are in the index; null until scanned. */
    val sidecarCount: StateFlow<Int?> = _sidecarCount.asStateFlow()

    private val _sidecarWorking = MutableStateFlow(false)
    val sidecarWorking: StateFlow<Boolean> = _sidecarWorking.asStateFlow()

    fun scanSidecars() {
        if (_sidecarWorking.value) return
        viewModelScope.launch {
            _sidecarWorking.value = true
            _sidecarCount.value = runCatching { sidecarCleanup.find().size }.getOrNull()
            _sidecarWorking.value = false
        }
    }

    /** Deletes every Takeout sidecar from the index and from Telegram. Not reversible. */
    fun deleteSidecars() {
        if (_sidecarWorking.value) return
        viewModelScope.launch {
            _sidecarWorking.value = true
            val removed = runCatching {
                sidecarCleanup.delete(sidecarCleanup.find().map { it.id })
            }.getOrDefault(0)
            _sidecarCount.value = 0
            _sidecarWorking.value = false
            _messages.tryEmit(UiText.Resource(Res.string.backup_cleanup_done, removed))
        }
    }

    private val _keyBackupWorking = MutableStateFlow(false)
    val keyBackupWorking: StateFlow<Boolean> = _keyBackupWorking.asStateFlow()

    val uiState: StateFlow<SettingsUiState> = combine(
        settingsRepository.preferences,
        user,
        telegramAuthRepository.connectionState,
        cacheRepository.observeStats(),
        syncRepository.syncing
    ) { prefs, currentUser, connection, cacheStats, syncing ->
        SettingsUiState(
            preferences = prefs,
            user = currentUser,
            connection = connection,
            cacheStats = cacheStats,
            syncing = syncing,
            loading = false
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    init {
        viewModelScope.launch {
            cacheRepository.refreshStats()
            when (val result = telegramAuthRepository.getCurrentUser()) {
                is AppResult.Success -> user.value = result.value
                is AppResult.Failure -> Unit
            }
        }
    }

    fun update(transform: (UserPreferences) -> UserPreferences) {
        viewModelScope.launch {
            val previous = settingsRepository.preferences.first()
            settingsRepository.update(transform)
            val current = settingsRepository.preferences.first()
            if (current.linkPreviews != previous.linkPreviews) {
                cacheRepository.clearLinkThumbnails()
            }
            if (current.allowMeteredTransfers != previous.allowMeteredTransfers ||
                current.backupChargingOnly != previous.backupChargingOnly ||
                current.backupWifiOnly != previous.backupWifiOnly
            ) {
                transferScheduler.rekick(current.allowMeteredTransfers)
            }
            if (current.updateCheckEnabled != previous.updateCheckEnabled) {
                maintenanceScheduler.scheduleUpdateCheck(current.updateCheckEnabled)
            }
            backupRepository.syncActiveSessionWithSelection()
            rescheduleWork()
        }
    }

    private suspend fun scanForNewBackupWork() {
        when (val result = backupRepository.startBackup(BackupTrigger.MANUAL)) {
            is AppResult.Success -> result.value?.let {
                _messages.tryEmit(UiText.Resource(Res.string.message_backing_up_new_folder))
            }

            is AppResult.Failure -> Unit
        }
    }

    private suspend fun rescheduleWork() {
        val prefs = settingsRepository.preferences.first()
        maintenanceScheduler.scheduleAll(
            backupEnabled = prefs.autoBackupEnabled && prefs.backupIntervalHours > 0,
            backupIntervalHours = prefs.backupIntervalHours,
            wifiOnly = prefs.backupWifiOnly,
            chargingOnly = prefs.backupChargingOnly,
            instantBackup = prefs.instantBackupEnabled,
            updateChecks = prefs.updateCheckEnabled
        )
    }

    val backupFolders: StateFlow<Set<String>> = settingsRepository.preferences
        .map { it.storageChatId }
        .distinctUntilChanged()
        .flatMapLatest { chatId ->
            channelRepository.observeChannels().map { channels ->
                channels.firstOrNull { it.chatId == chatId }?.backupFolders.orEmpty()
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    fun addBackupFolder(path: String) {
        editBackupFolders { it + path }
    }

    fun removeBackupFolder(path: String) {
        editBackupFolders { it - path }
    }

    private fun editBackupFolders(transform: (Set<String>) -> Set<String>) {
        viewModelScope.launch {
            val chatId = settingsRepository.preferences.first().storageChatId ?: return@launch
            val before = channelRepository.backupFolders(chatId)
            val after = transform(before)
            if (after == before) return@launch
            channelRepository.setBackupFolders(chatId, after)
            backupRepository.syncActiveSessionWithSelection()
            rescheduleWork()
            if ((after - before).isNotEmpty()) scanForNewBackupWork()
        }
    }

    fun notify(message: UiText) {
        _messages.tryEmit(message)
    }

    /**
     * Turns encryption on only after the key backup is saved, so nothing is sealed with an
     * unrecoverable key.
     */
    fun backUpEncryptionKey(passphrase: String, hint: String, enableEncryption: Boolean) {
        _keyBackupWorking.value = true
        viewModelScope.launch {
            val result = keyBackupRepository.createBackup(
                passphrase.toCharArray(),
                hint.takeIf { it.isNotBlank() }
            )
            _keyBackupWorking.value = false
            when (result) {
                is AppResult.Success -> {
                    settingsRepository.update {
                        it.copy(
                            keyBackupCreated = true,
                            encryptFiles = if (enableEncryption) true else it.encryptFiles
                        )
                    }
                    _messages.tryEmit(UiText.Resource(Res.string.message_key_backup_saved))
                }

                is AppResult.Failure -> _messages.tryEmit(result.error.toUiText())
            }
        }
    }

    /** Only callable once a key backup exists. */
    fun enableEncryption() {
        update { it.copy(encryptFiles = true) }
    }

    fun disableEncryption() {
        update { it.copy(encryptFiles = false) }
    }

    private val _keyHint = MutableStateFlow<KeyHint>(KeyHint.Unknown)
    val keyHint: StateFlow<KeyHint> = _keyHint.asStateFlow()

    fun loadKeyHint() {
        _keyHint.value = KeyHint.Loading
        viewModelScope.launch {
            _keyHint.value = when (val result = keyBackupRepository.backupHint()) {
                is AppResult.Success -> KeyHint.Loaded(result.value)
                is AppResult.Failure -> KeyHint.Missing
            }
        }
    }

    fun restoreEncryptionKey(passphrase: String) {
        _keyBackupWorking.value = true
        viewModelScope.launch {
            val result = keyBackupRepository.restore(passphrase.toCharArray())
            _keyBackupWorking.value = false
            when (result) {
                is AppResult.Success -> {
                    if (result.value) {
                        settingsRepository.update { it.copy(keyBackupCreated = true) }
                        _messages.tryEmit(UiText.Resource(Res.string.message_key_restored))
                        syncRepository.fullResync()
                    } else {
                        _messages.tryEmit(UiText.Resource(Res.string.message_wrong_passphrase))
                    }
                }

                is AppResult.Failure -> _messages.tryEmit(result.error.toUiText())
            }
        }
    }

    val reclaimableBytes: StateFlow<Long> = fileRepository.observeReclaimableBytes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0L)

    private val _deleteConsentRequests = MutableSharedFlow<DeleteConsentRequest>(extraBufferCapacity = 1)
    val deleteConsentRequests = _deleteConsentRequests.asSharedFlow()

    val indexedSoFar: StateFlow<Int> = syncRepository.indexedSoFar
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    fun reportNothingToFree() {
        _messages.tryEmit(UiText.Resource(Res.string.settings_free_up_none))
    }

    fun freeUpSpace() {
        viewModelScope.launch {
            when (val result = fileRepository.freeUpSpace()) {
                is AppResult.Success -> {
                    val consent = result.value.consentRequest
                    if (consent != null) {
                        _deleteConsentRequests.tryEmit(consent)
                    } else {
                        _messages.tryEmit(
                            if (result.value.deletedCount == 0) {
                                UiText.Resource(Res.string.settings_nothing_to_remove)
                            } else {
                                UiText.Resource(
                                    Res.string.files_removed_local_copies,
                                    result.value.deletedCount
                                )
                            }
                        )
                    }
                }

                is AppResult.Failure -> _messages.tryEmit(result.error.toUiText())
            }
        }
    }

    fun clearCache() {
        viewModelScope.launch {
            cacheRepository.clearAll()
            _messages.tryEmit(UiText.Resource(Res.string.message_cache_cleared))
        }
    }

    fun clearThumbnails() {
        viewModelScope.launch {
            cacheRepository.clearThumbnails()
            _messages.tryEmit(UiText.Resource(Res.string.message_thumbnails_cleared))
        }
    }

    fun checkForUpdates() {
        if (_updateState.value is UpdateState.Checking) return
        viewModelScope.launch {
            _updateState.value = UpdateState.Checking
            val result = updateChecker.check()
            settingsRepository.update { it.copy(lastUpdateCheckAt = System.currentTimeMillis()) }
            _updateState.value = when (result) {
                is UpdateCheck.Available -> {
                    settingsRepository.update {
                        it.copy(notifiedUpdateVersion = result.release.version)
                    }
                    UpdateState.Available(result.release)
                }

                is UpdateCheck.UpToDate -> {
                    _messages.tryEmit(UiText.Resource(Res.string.about_no_update))
                    UpdateState.Message(
                        "Installed ${result.current} · latest release ${result.latest}"
                    )
                }

                is UpdateCheck.Failed -> UpdateState.Message("Check failed: ${result.reason}")
            }
        }
    }

    /** Reads how much a restore would involve. Nothing is downloaded to answer this. */
    fun loadRestorePlan(scope: RestoreScope) {
        viewModelScope.launch {
            _restorePlan.value = null
            _restorePlan.value = withContext(Dispatchers.IO) { restoreFiles.plan(scope) }
        }
    }

    fun startRestore(scope: RestoreScope, onlyMissing: Boolean) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) { restoreFiles.restore(scope, onlyMissing) }
            when (result) {
                is AppResult.Success -> _messages.tryEmit(
                    if (result.value == 0) {
                        UiText.Resource(Res.string.restore_nothing_to_do)
                    } else {
                        UiText.Resource(Res.string.restore_queued, result.value)
                    }
                )

                is AppResult.Failure -> _messages.tryEmit(result.error.toUiText())
            }
            _restorePlan.value = withContext(Dispatchers.IO) { restoreFiles.plan(scope) }
        }
    }

    fun dismissUpdate() {
        _updateState.value = UpdateState.Idle
    }

    fun shareDebugLog() {
        viewModelScope.launch {
            val log = withContext(Dispatchers.IO) { debugLogRecorder.export() }
            if (log == null) {
                _messages.tryEmit(UiText.Resource(Res.string.settings_debug_log_empty))
            } else {
                _shareRequests.tryEmit(ShareRequest(listOf(log.absolutePath), "text/plain"))
            }
        }
    }

    fun resync() {
        viewModelScope.launch {
            when (val result = syncRepository.fullResync()) {
                is AppResult.Success -> _messages.tryEmit(
                    if (result.value.lockedFiles > 0) {
                        UiText.PluralResource(Res.plurals.rebuild_locked_files, result.value.lockedFiles,
                            result.value.lockedFiles
                        )
                    } else {
                        UiText.Resource(Res.string.message_rebuild_done,
                            result.value.inserted,
                            result.value.updated,
                            result.value.detachedFromRemote
                        )
                    }
                )

                is AppResult.Failure -> _messages.tryEmit(result.error.toUiText())
            }
        }
    }

    fun logout(onLoggedOut: () -> Unit) {
        viewModelScope.launch {
            when (val result = telegramAuthRepository.logout()) {
                is AppResult.Success -> {
                    withContext(NonCancellable) {
                        localDataWiper.wipe()
                        settingsRepository.clearTelegramCredentials()
                        settingsRepository.update {
                            it.copy(
                                onboardingComplete = false,
                                storageChatId = null,
                                backupFolders = emptySet()
                            )
                        }
                    }
                    onLoggedOut()
                }

                is AppResult.Failure -> _messages.tryEmit(result.error.toUiText())
            }
        }
    }
}
