package com.lihan.localvault.feature.vault.presentation.vault

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lihan.localvault.core.domain.util.Result
import com.lihan.localvault.feature.vault.domain.VaultRepository
import com.lihan.localvault.feature.vault.presentation.mapper.toUi
import com.lihan.localvault.feature.vault.presentation.model.VaultUi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class VaultViewModel(
    private val vaultRepository: VaultRepository
): ViewModel() {

    private var hasLoadedInitialData = false

    private val _state = MutableStateFlow(VaultState())
    val state = _state.onStart {
        if (!hasLoadedInitialData) {
            observeVaultItems()
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
            is VaultAction.OnToggleExpand -> toggleExpand(action.id)
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
            is VaultAction.OnDelete -> deleteVault(action.id)
            is VaultAction.OnCopyClick,
            is VaultAction.OnCreateClick,
            is VaultAction.OnEditClick -> Unit
        }
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
