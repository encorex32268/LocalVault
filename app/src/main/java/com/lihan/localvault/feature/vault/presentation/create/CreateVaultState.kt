package com.lihan.localvault.feature.vault.presentation.create

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.ui.graphics.Color
import com.lihan.localvault.core.domain.VaultCategory
import com.lihan.localvault.core.presentation.ui.theme.IconsEnum
import com.lihan.localvault.core.presentation.ui.theme.VaultColors
import com.lihan.localvault.feature.vault.presentation.model.VaultAttachmentUi
import com.lihan.localvault.feature.vault.presentation.model.VaultFieldUi

data class CreateVaultState(
    val vaultId: Long? = null,
    val isLoading: Boolean = false,
    val vaultTitle: TextFieldState = TextFieldState(),
    val category: VaultCategory = VaultCategory.CUSTOM,
    val fields: List<VaultFieldUi> = emptyList(),
    val attachments: List<VaultAttachmentUi> = emptyList(),
    val color: Color = VaultColors.first(),
    val iconIndex: Int = 0,
    val isIconPickerOpen: Boolean = false,
    val isAddMenuOpen: Boolean = false,
    val isProcessingAttachment: Boolean = false
) {
    val iconResId: Int
        get() {
            val icons = IconsEnum.entries
            return icons.getOrNull(iconIndex)?.resId ?: icons.first().resId
        }
}
