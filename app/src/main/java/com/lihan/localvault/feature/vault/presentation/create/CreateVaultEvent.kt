package com.lihan.localvault.feature.vault.presentation.create

sealed interface CreateVaultEvent {
    data object SaveSuccess : CreateVaultEvent
}
