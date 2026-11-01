package com.lihan.localvault.feature.vault.presentation.create

import androidx.compose.ui.graphics.Color
import com.lihan.localvault.core.domain.FieldType
import com.lihan.localvault.core.domain.VaultCategory

sealed interface CreateVaultAction {
    data class OnInit(val vaultId: Long? = null) : CreateVaultAction
    data object OnSaveClick : CreateVaultAction
    data object OnBackClick : CreateVaultAction
    data object OnOpenIconPicker : CreateVaultAction
    data object OnDismissIconPicker : CreateVaultAction
    data class OnSelectIcon(val iconIndex: Int) : CreateVaultAction
    data class OnSelectColor(val color: Color) : CreateVaultAction
    data class OnSelectCategory(val category: VaultCategory) : CreateVaultAction
    data object OnPasteTitle : CreateVaultAction
    data class OnPasteField(val fieldId: String) : CreateVaultAction
    data object OnOpenAddMenu : CreateVaultAction
    data object OnDismissAddMenu : CreateVaultAction
    data class OnAddField(val type: FieldType, val label: String = "") : CreateVaultAction
    data class OnRemoveField(val fieldId: String) : CreateVaultAction
    data class OnTogglePasswordVisibility(val fieldId: String) : CreateVaultAction
    data class OnMoveField(val fromIndex: Int, val toIndex: Int) : CreateVaultAction
    data object OnTakePhotoClick : CreateVaultAction
    data object OnChoosePhotoClick : CreateVaultAction
    data object OnChooseDocumentClick : CreateVaultAction
    data class OnFileSelected(val uri: android.net.Uri, val tempFile: java.io.File? = null) : CreateVaultAction
    data class OnRemoveAttachment(val attachmentId: String) : CreateVaultAction
}
