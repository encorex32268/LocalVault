package com.lihan.localvault.feature.vault.presentation.vault

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lihan.localvault.core.biometric.BiometricAvailability
import com.lihan.localvault.core.biometric.BiometricPromptManager
import com.lihan.localvault.core.domain.datastore.AppDataStore
import com.lihan.localvault.core.domain.util.Result
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
    private val biometricPromptManager: BiometricPromptManager
): ViewModel() {

    private val _events = Channel<VaultEvent>()
    val events = _events.receiveAsFlow()

    private var hasLoadedInitialData = false

    private val _state = MutableStateFlow(VaultState())
    val state = _state.onStart {
        if (!hasLoadedInitialData) {
            observeVaultItems()
            observeTheme()
            hasLoadedInitialData = true
        }
    }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = VaultState()
        )

    fun onAction(action: VaultAction) {
        when (action) {
            is VaultAction.OnToggleExpand -> handleToggleExpand(action.id)
            is VaultAction.OnBiometricSuccess -> {
                _state.update { it.copy(isAuthenticatedSession = true) }
                toggleExpand(action.id)
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
            is VaultAction.OnAuthenticatedSession -> {
                _state.update { it.copy(isAuthenticatedSession = true) }
            }
            is VaultAction.OnEndSession -> {
                _state.update { current ->
                    current.copy(
                        isAuthenticatedSession = false,
                        items = current.items.map { if (it.isExpand) it.copy(isExpand = false) else it }
                    )
                }
            }
            is VaultAction.OnCollapseAll -> collapseAll()
            is VaultAction.OnCopyClick,
            is VaultAction.OnCreateClick,
            is VaultAction.OnEditClick -> Unit
        }
    }

    private fun handleToggleExpand(id: Long) {
        val currentItem = _state.value.items.find { it.id == id } ?: return

        if (currentItem.isExpand) {
            toggleExpand(id)
            return
        }

        if (_state.value.isAuthenticatedSession) {
            toggleExpand(id)
            return
        }

        val availability = biometricPromptManager.checkBiometricAvailability()
        if (availability == BiometricAvailability.READY) {
            viewModelScope.launch {
                _events.send(VaultEvent.RequestBiometric(id))
            }
        } else {
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
                isAuthenticatedSession = false,
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
}
