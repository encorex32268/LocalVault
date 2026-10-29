package com.lihan.localvault.feature.vault.presentation.vault

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lihan.localvault.R
import com.lihan.localvault.core.biometric.BiometricAvailability
import com.lihan.localvault.core.biometric.BiometricPromptManager
import com.lihan.localvault.core.domain.datastore.AppDataStore
import com.lihan.localvault.core.domain.util.DataError
import com.lihan.localvault.core.domain.util.Result
import com.lihan.localvault.core.session.SessionManager
import com.lihan.localvault.feature.vault.domain.VaultRepository
import com.lihan.localvault.feature.vault.presentation.mapper.toUi
import com.lihan.localvault.feature.vault.presentation.model.VaultUi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class VaultViewModel(
    private val vaultRepository: VaultRepository,
    private val appDataStore: AppDataStore,
    private val biometricPromptManager: BiometricPromptManager,
    private val sessionManager: SessionManager
): ViewModel() {

    private val _events = Channel<VaultEvent>()
    val events = _events.receiveAsFlow()

    private var hasLoadedInitialData = false

    private val _state = MutableStateFlow(VaultState())
    val state = _state.onStart {
        if (!hasLoadedInitialData) {
            observeVaultItems()
            observeTheme()
            observeLimitCardCollapsed()
            observeSession()
            hasLoadedInitialData = true
        }
    }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = VaultState()
        )

    init {
        val availability = biometricPromptManager.checkBiometricAvailability()
        val isAvailable = availability == BiometricAvailability.READY
        _state.update {
            it.copy(
                isBiometricAvailable = isAvailable,
                isAuthenticatedSession = !isAvailable || sessionManager.isSessionUnlocked.value
            )
        }
    }

    fun onAction(action: VaultAction) {
        when (action) {
            is VaultAction.OnToggleExpand -> handleToggleExpand(action.id)
            is VaultAction.OnBiometricSuccess,
            is VaultAction.OnAuthenticatedSession -> {
                sessionManager.unlock()
            }
            is VaultAction.OnRequestBiometric -> {
                if (_state.value.isLocked) {
                    viewModelScope.launch {
                        _events.send(VaultEvent.RequestBiometric)
                    }
                }
            }
            is VaultAction.OnAppForeground -> {
                sessionManager.onAppForeground()
                if (!sessionManager.isSessionUnlocked.value && _state.value.isBiometricAvailable && _state.value.items.isNotEmpty()) {
                    _state.update { current ->
                        current.copy(
                            isAuthenticatedSession = false,
                            items = current.items.map { if (it.isExpand) it.copy(isExpand = false) else it }
                        )
                    }
                    viewModelScope.launch {
                        _events.send(VaultEvent.RequestBiometric)
                    }
                }
            }
            is VaultAction.OnDeleteClick -> {
                _state.update { it.copy(deleteTargetId = action.id) }
            }
            is VaultAction.OnConfirmDelete -> {
                val targetId = _state.value.deleteTargetId
                if (targetId != null) {
                    deleteVault(targetId)
                }
                _state.update { it.copy(deleteTargetId = null) }
            }
            is VaultAction.OnDismissDeleteDialog -> {
                _state.update { it.copy(deleteTargetId = null) }
            }
            is VaultAction.OnToggleTheme -> {
                viewModelScope.launch {
                    appDataStore.toggleTheme()
                }
            }
            is VaultAction.OnToggleLimitCardCollapse -> {
                viewModelScope.launch {
                    appDataStore.toggleLimitCardCollapsed()
                }
            }
            is VaultAction.OnDelete -> deleteVault(action.id)
            is VaultAction.OnSearchQueryChange -> {
                _state.update { it.copy(searchQuery = action.query) }
            }
            is VaultAction.OnToggleSearch -> {
                _state.update { current ->
                    val willBeActive = !current.isSearchActive
                    current.copy(
                        isSearchActive = willBeActive,
                        searchQuery = if (!willBeActive) "" else current.searchQuery
                    )
                }
            }
            is VaultAction.OnClearSearch -> {
                _state.update { it.copy(searchQuery = "") }
            }
            is VaultAction.OnEndSession -> {
                sessionManager.lock()
            }
            is VaultAction.OnCollapseAll -> collapseAll()
            is VaultAction.OnExportBackupClick -> {
                _state.update {
                    it.copy(
                        isExportPasswordDialogVisible = true,
                        backupErrorRes = null
                    )
                }
            }
            is VaultAction.OnImportBackupClick -> {
                viewModelScope.launch {
                    _events.send(VaultEvent.LaunchImportFilePicker)
                }
            }
            is VaultAction.OnDismissBackupDialog -> {
                _state.update {
                    it.copy(
                        isExportPasswordDialogVisible = false,
                        isImportPasswordDialogVisible = false,
                        pendingImportContent = null,
                        backupErrorRes = null
                    )
                }
            }
            is VaultAction.OnExportPasswordConfirm -> {
                handleExportBackup(action.password)
            }
            is VaultAction.OnImportFileSelected -> {
                _state.update {
                    it.copy(
                        isImportPasswordDialogVisible = true,
                        pendingImportContent = action.content,
                        backupErrorRes = null
                    )
                }
            }
            is VaultAction.OnImportPasswordConfirm -> {
                handleImportBackup(action.password)
            }
            is VaultAction.OnCopyClick,
            is VaultAction.OnCreateClick,
            is VaultAction.OnEditClick -> Unit
        }
    }

    private fun handleToggleExpand(id: Long) {
        if (!_state.value.isLocked) {
            toggleExpand(id)
        }
    }

    private fun observeTheme() {
        appDataStore.isDarkTheme
            .onEach { isDark ->
                _state.update { it.copy(isDarkTheme = isDark) }
            }
            .launchIn(viewModelScope)
    }

    private fun observeLimitCardCollapsed() {
        appDataStore.isLimitCardCollapsed
            .onEach { isCollapsed ->
                _state.update { it.copy(isLimitCardCollapsed = isCollapsed) }
            }
            .launchIn(viewModelScope)
    }

    private fun observeSession() {
        sessionManager.isSessionUnlocked
            .onEach { isUnlocked ->
                _state.update { current ->
                    val authenticated = !current.isBiometricAvailable || isUnlocked
                    current.copy(
                        isAuthenticatedSession = authenticated,
                        items = if (!authenticated) {
                            current.items.map { if (it.isExpand) it.copy(isExpand = false) else it }
                        } else {
                            current.items
                        }
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    private fun toggleExpand(id: Long) {
        val currentItem = _state.value.items.find { it.id == id } ?: return
        val willExpand = !currentItem.isExpand

        if (!willExpand) {
            updateItem(id) { it.copy(isExpand = false) }
            return
        }

        if (currentItem.content != null) {
            updateItem(id) { it.copy(isExpand = true) }
            return
        }

        updateItem(id) { it.copy(isExpand = true, isLoadingContent = true) }
        loadVaultContent(id)
    }

    private fun collapseAll() {
        _state.update { current ->
            current.copy(
                items = current.items.map { if (it.isExpand) it.copy(isExpand = false) else it }
            )
        }
    }

    private fun loadVaultContent(id: Long) {
        viewModelScope.launch {
            when (val result = vaultRepository.getVaultContent(id)) {
                is Result.Success -> {
                    updateItem(id) {
                        it.copy(
                            isLoadingContent = false,
                            content = result.data.toUi()
                        )
                    }
                }
                is Result.Error -> {
                    updateItem(id) {
                        it.copy(isLoadingContent = false)
                    }
                }
            }
        }
    }

    private fun deleteVault(id: Long) {
        viewModelScope.launch {
            vaultRepository.deleteVault(id)
        }
    }

    private fun updateItem(id: Long, transform: (VaultUi) -> VaultUi) {
        _state.update { current ->
            current.copy(
                items = current.items.map { if (it.id == id) transform(it) else it }
            )
        }
    }

    private fun observeVaultItems() {
        vaultRepository
            .vaultItems
            .onEach { items ->
                val itemsToReload = mutableListOf<Long>()

                _state.update { current ->
                    val existingMap = current.items.associateBy { it.id }
                    current.copy(
                        items = items.map { vault ->
                            val existing = existingMap[vault.id]
                            val isStale = existing != null && existing.updatedAt != vault.updatedAt
                            val shouldReload = existing != null && existing.isExpand && isStale

                            if (shouldReload) {
                                itemsToReload.add(vault.id)
                            }

                            vault.toUi().copy(
                                isExpand = existing?.isExpand ?: false,
                                isLoadingContent = if (shouldReload) true else (existing?.isLoadingContent ?: false),
                                content = if (isStale) null else existing?.content
                            )
                        }
                    )
                }

                itemsToReload.forEach { id ->
                    loadVaultContent(id)
                }
            }
            .launchIn(viewModelScope)
    }

    private fun handleExportBackup(passphrase: String) {
        viewModelScope.launch {
            when (val result = vaultRepository.exportBackup(passphrase)) {
                is Result.Success -> {
                    _state.update { it.copy(isExportPasswordDialogVisible = false, backupErrorRes = null) }
                    val fileName = "localvault_backup_${System.currentTimeMillis()}.vault"
                    _events.send(VaultEvent.LaunchExportFilePicker(suggestedFileName = fileName, content = result.data))
                }
                is Result.Error -> {
                    _events.send(VaultEvent.ShowToast(R.string.error_backup_failed))
                }
            }
        }
    }

    private fun handleImportBackup(passphrase: String) {
        val content = _state.value.pendingImportContent ?: return
        viewModelScope.launch {
            when (val result = vaultRepository.importBackup(passphrase, content)) {
                is Result.Success -> {
                    _state.update {
                        it.copy(
                            isImportPasswordDialogVisible = false,
                            pendingImportContent = null,
                            backupErrorRes = null
                        )
                    }
                    _events.send(VaultEvent.ShowToast(R.string.import_success, listOf(result.data)))
                }
                is Result.Error -> {
                    val errorRes = when (result.error) {
                        DataError.Local.INVALID_PASSPHRASE -> R.string.error_invalid_passphrase
                        DataError.Local.CORRUPTED_DATA -> R.string.error_corrupted_backup
                        else -> R.string.error_backup_failed
                    }
                    _state.update { it.copy(backupErrorRes = errorRes) }
                }
            }
        }
    }
}
