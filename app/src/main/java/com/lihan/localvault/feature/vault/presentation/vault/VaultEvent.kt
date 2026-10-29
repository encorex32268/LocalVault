package com.lihan.localvault.feature.vault.presentation.vault

sealed interface VaultEvent {
    data object RequestBiometric : VaultEvent
    data class LaunchExportFilePicker(val suggestedFileName: String, val content: String) : VaultEvent
    data object LaunchImportFilePicker : VaultEvent
    data class ShowToast(val messageRes: Int, val formatArgs: List<Any> = emptyList()) : VaultEvent
}
