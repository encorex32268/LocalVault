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
    data object OnToggleLimitCardCollapse: VaultAction
    data class OnSearchQueryChange(val query: String): VaultAction
    data object OnToggleSearch: VaultAction
    data object OnClearSearch: VaultAction
    data object OnCollapseAll: VaultAction
    data object OnAuthenticatedSession: VaultAction
    data object OnEndSession: VaultAction
    data object OnRequestBiometric: VaultAction
    data object OnAppForeground: VaultAction
    data class OnBiometricSuccess(val id: Long): VaultAction
    data object OnExportBackupClick: VaultAction
    data object OnImportBackupClick: VaultAction
    data class OnExportPasswordConfirm(val password: String, val includeAttachments: Boolean = true): VaultAction
    data class OnImportFileSelected(val content: String): VaultAction
    data class OnImportPasswordConfirm(val password: String): VaultAction
    data object OnDismissBackupDialog: VaultAction
}
