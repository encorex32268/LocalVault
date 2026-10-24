package com.lihan.localvault.feature.vault.presentation.vault

sealed interface VaultEvent {
    data class RequestBiometric(val vaultId: Long) : VaultEvent
}
