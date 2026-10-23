package com.lihan.localvault.feature.vault.presentation.create

import androidx.compose.ui.graphics.Color
import com.lihan.localvault.core.domain.VaultCategory
import com.lihan.localvault.feature.vault.presentation.vault.components.textfields.create.VaultFieldType

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
    data class OnPasteField(val fieldType: VaultFieldType) : CreateVaultAction
}

