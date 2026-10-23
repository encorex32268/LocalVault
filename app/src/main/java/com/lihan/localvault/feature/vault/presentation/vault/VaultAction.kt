package com.lihan.localvault.feature.vault.presentation.vault

sealed interface VaultAction {
    data class OnToggleExpand(val id: Long): VaultAction
    data class OnDeleteClick(val id: Long): VaultAction
    data object OnConfirmDelete: VaultAction
    data object OnDismissDeleteDialog: VaultAction
    data class OnDelete(val id: Long): VaultAction
    data class OnCopyClick(val text: String): VaultAction
    data object OnCreateClick: VaultAction
    data class OnEditClick(val id: Long): VaultAction
    data object OnToggleTheme: VaultAction
}
