package com.lihan.localvault.feature.vault.presentation.create

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.fromColorLong
import androidx.compose.ui.graphics.toColorLong
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lihan.localvault.core.domain.FieldType
import com.lihan.localvault.core.domain.Vault
import com.lihan.localvault.core.domain.VaultCategory
import com.lihan.localvault.core.domain.VaultContent
import com.lihan.localvault.core.domain.VaultField
import com.lihan.localvault.core.domain.util.Result
import com.lihan.localvault.core.presentation.ui.theme.VaultColors
import com.lihan.localvault.feature.vault.domain.VaultRepository
import com.lihan.localvault.feature.vault.presentation.mapper.toUi
import com.lihan.localvault.feature.vault.presentation.model.VaultFieldUi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CreateVaultViewModel(
    private val vaultRepository: VaultRepository
) : ViewModel() {

    private val _events = Channel<CreateVaultEvent>()
    val events = _events.receiveAsFlow()

    private val _state = MutableStateFlow(
        CreateVaultState(
            category = VaultCategory.CUSTOM,
            fields = listOf(
                VaultFieldUi(type = FieldType.TEXT, label = "Account"),
                VaultFieldUi(type = FieldType.PASSWORD, label = "Password")
            )
        )
    )
    val state = _state.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = _state.value
    )

    fun onAction(action: CreateVaultAction) {
        when (action) {
            is CreateVaultAction.OnInit -> {
                if (action.vaultId != null) {
                    loadExistingVault(action.vaultId)
                }
            }
            is CreateVaultAction.OnOpenIconPicker -> {
                _state.update { it.copy(isIconPickerOpen = true) }
            }
            is CreateVaultAction.OnDismissIconPicker -> {
                _state.update { it.copy(isIconPickerOpen = false) }
            }
            is CreateVaultAction.OnSelectIcon -> {
                _state.update { it.copy(iconIndex = action.iconIndex, isIconPickerOpen = false) }
            }
            is CreateVaultAction.OnSelectColor -> {
                _state.update { it.copy(color = action.color) }
            }
            is CreateVaultAction.OnSelectCategory -> {
                _state.update { it.copy(category = action.category) }
            }
            is CreateVaultAction.OnOpenAddFieldMenu -> {
                _state.update { it.copy(isAddFieldMenuOpen = true) }
            }
            is CreateVaultAction.OnDismissAddFieldMenu -> {
                _state.update { it.copy(isAddFieldMenuOpen = false) }
            }
            is CreateVaultAction.OnAddField -> {
                val defaultLabel = when (action.type) {
                    FieldType.TEXT -> "Account"
                    FieldType.PASSWORD -> "Password"
                    FieldType.NOTE -> "Note"
                }
                val count = _state.value.fields.count { it.type == action.type }
                val label = if (count == 0) defaultLabel else "$defaultLabel ${count + 1}"
                val newField = VaultFieldUi(
                    type = action.type,
                    label = label
                )
                _state.update {
                    it.copy(
                        fields = it.fields + newField,
                        isAddFieldMenuOpen = false
                    )
                }
            }
            is CreateVaultAction.OnRemoveField -> {
                _state.update {
                    it.copy(fields = it.fields.filterNot { field -> field.id == action.fieldId })
                }
            }
            is CreateVaultAction.OnTogglePasswordVisibility -> {
                _state.update {
                    it.copy(
                        fields = it.fields.map { field ->
                            if (field.id == action.fieldId) {
                                field.copy(isPasswordVisible = !field.isPasswordVisible)
                            } else field
                        }
                    )
                }
            }
            is CreateVaultAction.OnFieldLabelChange -> {
                _state.update {
                    it.copy(
                        fields = it.fields.map { field ->
                            if (field.id == action.fieldId) {
                                field.copy(label = action.newLabel)
                            } else field
                        }
                    )
                }
            }
            is CreateVaultAction.OnSaveClick -> saveVault()
            else -> Unit
        }
    }

    private fun loadExistingVault(vaultId: Long) {
        _state.update { it.copy(vaultId = vaultId, isLoading = true) }
        viewModelScope.launch {
            val vaultResult = vaultRepository.getVaultById(vaultId)
            val contentResult = vaultRepository.getVaultContent(vaultId)

            if (vaultResult is Result.Success && contentResult is Result.Success) {
                val vault = vaultResult.data
                val content = contentResult.data
                _state.update { current ->
                    current.copy(
                        isLoading = false,
                        color = Color.fromColorLong(vault.color),
                        category = vault.category,
                        iconIndex = vault.iconIndex,
                        vaultTitle = TextFieldState(vault.title),
                        fields = content.toUi().fields
                    )
                }
            } else {
                _state.update { it.copy(isLoading = false) }
            }
        }
    }

    private fun saveVault() {
        val currentState = _state.value
        val titleText = currentState.vaultTitle.text.trim().toString().ifBlank {
            "Vault"
        }

        val domainContent = VaultContent(
            fields = currentState.fields.map { fieldUi ->
                VaultField(
                    id = fieldUi.id,
                    type = fieldUi.type,
                    label = fieldUi.label,
                    value = fieldUi.state.text.toString()
                )
            }
        )

        val vault = Vault(
            id = currentState.vaultId ?: 0L,
            iconIndex = currentState.iconIndex,
            color = currentState.color.toColorLong(),
            title = titleText,
            updatedAt = System.currentTimeMillis(),
            category = currentState.category
        )

        _state.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            when (val result = vaultRepository.upsertVault(vault, domainContent)) {
                is Result.Success -> {
                    _events.send(CreateVaultEvent.SaveSuccess)
                }
                is Result.Error -> {
                    _state.update { it.copy(isLoading = false) }
                }
            }
        }
    }
}
