package com.lihan.localvault.feature.vault.presentation.vault

import com.lihan.localvault.feature.vault.presentation.model.VaultUi

data class VaultState(
    val isLoading: Boolean = false,
    val items: List<VaultUi> = emptyList()
)