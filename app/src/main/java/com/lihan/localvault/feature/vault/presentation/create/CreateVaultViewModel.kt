package com.lihan.localvault.feature.vault.presentation.create

import android.net.Uri
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.fromColorLong
import androidx.compose.ui.graphics.toColorLong
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lihan.localvault.core.domain.FieldType
import com.lihan.localvault.core.domain.Vault
import com.lihan.localvault.core.domain.VaultAttachment
import com.lihan.localvault.core.domain.VaultCategory
import com.lihan.localvault.core.domain.VaultContent
import com.lihan.localvault.core.domain.VaultField
import com.lihan.localvault.core.domain.datastore.AppDataStore
import com.lihan.localvault.core.domain.util.Result
import com.lihan.localvault.core.presentation.ui.theme.VaultColors
import com.lihan.localvault.core.domain.security.storage.EncryptedFileManager
import com.lihan.localvault.feature.vault.domain.VaultRepository
import com.lihan.localvault.feature.vault.presentation.mapper.toUi
import com.lihan.localvault.feature.vault.presentation.model.VaultAttachmentUi
import com.lihan.localvault.feature.vault.presentation.model.VaultFieldUi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

class CreateVaultViewModel(
    private val vaultRepository: VaultRepository,
    private val appDataStore: AppDataStore,
    private val encryptedFileManager: EncryptedFileManager
) : ViewModel() {

    private val _events = Channel<CreateVaultEvent>()
    val events = _events.receiveAsFlow()

    private val _state = MutableStateFlow(
        CreateVaultState()
    )
    val state = _state.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = _state.value
    )

    init {
        loadLastUsedPreferences()
    }

    fun onAction(action: CreateVaultAction) {
        when (action) {
            is CreateVaultAction.OnInit -> {
                if (action.vaultId != null) {
                    loadExistingVault(action.vaultId)
                } else {
                    loadLastUsedPreferences()
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
            is CreateVaultAction.OnOpenAddMenu -> {
                _state.update { it.copy(isAddMenuOpen = true) }
            }
            is CreateVaultAction.OnDismissAddMenu -> {
                _state.update { it.copy(isAddMenuOpen = false) }
            }
            is CreateVaultAction.OnAddField -> {
                val newField = VaultFieldUi(
                    type = action.type,
                    label = action.label
                )
                _state.update {
                    it.copy(
                        fields = it.fields + newField,
                        isAddMenuOpen = false
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
            is CreateVaultAction.OnMoveField -> {
                _state.update { state ->
                    if (action.fromIndex == action.toIndex ||
                        action.fromIndex !in state.fields.indices ||
                        action.toIndex !in state.fields.indices
                    ) {
                        state
                    } else {
                        val updated = state.fields.toMutableList().apply {
                            val item = removeAt(action.fromIndex)
                            add(action.toIndex, item)
                        }
                        state.copy(fields = updated)
                    }
                }
            }
            is CreateVaultAction.OnTakePhotoClick -> {
                _state.update { it.copy(isAddMenuOpen = false) }
                val (uri, tempFile) = encryptedFileManager.createCameraTempUri()
                viewModelScope.launch {
                    _events.send(CreateVaultEvent.LaunchCamera(uri, tempFile))
                }
            }
            is CreateVaultAction.OnChoosePhotoClick -> {
                _state.update { it.copy(isAddMenuOpen = false) }
                viewModelScope.launch {
                    _events.send(CreateVaultEvent.LaunchPhotoPicker)
                }
            }
            is CreateVaultAction.OnChooseDocumentClick -> {
                _state.update { it.copy(isAddMenuOpen = false) }
                viewModelScope.launch {
                    _events.send(CreateVaultEvent.LaunchDocumentPicker)
                }
            }
            is CreateVaultAction.OnFileSelected -> {
                _state.update { it.copy(isProcessingAttachment = true) }
                viewModelScope.launch{
                    try {
                        when (val result = encryptedFileManager.saveEncryptedFile(action.uri)) {
                            is Result.Success -> {
                                val attachment = result.data
                                val ui = VaultAttachmentUi(
                                    id = attachment.id,
                                    fileName = attachment.fileName,
                                    mimeType = attachment.mimeType,
                                    fileSize = attachment.fileSize,
                                    encryptedFileName = attachment.encryptedFileName
                                )
                                _state.update {
                                    it.copy(
                                        attachments = it.attachments + ui,
                                        isProcessingAttachment = false
                                    )
                                }
                            }
                            is Result.Error -> {
                                _state.update { it.copy(isProcessingAttachment = false) }
                            }
                        }
                    } finally {
                        action.tempFile?.let { encryptedFileManager.deleteTempFile(it) }
                    }
                }
            }
            is CreateVaultAction.OnRemoveAttachment -> {
                val removed = _state.value.attachments.find { it.id == action.attachmentId }
                if (removed != null) {
                    viewModelScope.launch(Dispatchers.IO) {
                        encryptedFileManager.deleteEncryptedFile(removed.encryptedFileName)
                    }
                }
                _state.update {
                    it.copy(attachments = it.attachments.filterNot { att -> att.id == action.attachmentId })
                }
            }
            is CreateVaultAction.OnSaveClick -> saveVault()
            else -> Unit
        }
    }

    private fun loadLastUsedPreferences() {
        viewModelScope.launch {
            val colorLong = appDataStore.lastUsedColor.first()
            val iconIndex = appDataStore.lastUsedIconIndex.first()
            _state.update { current ->
                current.copy(
                    color = colorLong?.let { Color.fromColorLong(it) } ?: current.color,
                    iconIndex = iconIndex ?: current.iconIndex
                )
            }
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
                val contentUi = content.toUi()
                _state.update { current ->
                    current.copy(
                        isLoading = false,
                        color = Color.fromColorLong(vault.color),
                        category = vault.category,
                        iconIndex = vault.iconIndex,
                        vaultTitle = TextFieldState(vault.title),
                        fields = contentUi.fields,
                        attachments = contentUi.attachments
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
            },
            attachments = currentState.attachments.map { attUi ->
                VaultAttachment(
                    id = attUi.id,
                    fileName = attUi.fileName,
                    mimeType = attUi.mimeType,
                    fileSize = attUi.fileSize,
                    encryptedFileName = attUi.encryptedFileName
                )
            }
        )

        val vault = Vault(
            id = currentState.vaultId ?: 0L,
            iconIndex = currentState.iconIndex,
            color = currentState.color.toColorLong(),
            title = titleText,
            updatedAt = System.currentTimeMillis(),
            category = currentState.category,
            hasAttachments = currentState.attachments.isNotEmpty()
        )

        _state.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            when (val result = vaultRepository.upsertVault(vault, domainContent)) {
                is Result.Success -> {
                    if (currentState.vaultId == null) {
                        appDataStore.saveLastUsedVaultStyle(
                            color = currentState.color.toColorLong(),
                            iconIndex = currentState.iconIndex
                        )
                    }
                    _events.send(CreateVaultEvent.SaveSuccess)
                }
                is Result.Error -> {
                    _state.update { it.copy(isLoading = false) }
                }
            }
        }
    }
}
