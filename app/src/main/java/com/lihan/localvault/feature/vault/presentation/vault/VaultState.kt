package com.lihan.localvault.feature.vault.presentation.vault

import com.lihan.localvault.feature.vault.presentation.model.VaultUi

const val MAX_FREE_VAULTS = 15

data class VaultState(
    val isLoading: Boolean = false,
    val items: List<VaultUi> = emptyList(),
    val searchQuery: String = "",
    val isSearchActive: Boolean = false,
    val deleteTargetId: Long? = null,
    val isDarkTheme: Boolean = true,
    val isAuthenticatedSession: Boolean = false,
    val isLimitEnabled: Boolean = false,
    val maxVaultCount: Int = MAX_FREE_VAULTS,
    val isLimitCardCollapsed: Boolean = false
) {
    val totalCount: Int get() = items.size
    val isLimitReached: Boolean get() = isLimitEnabled && items.size >= maxVaultCount

    val filteredItems: List<VaultUi>
        get() = if (searchQuery.isBlank()) {
            items
        } else {
            items.filter { it.title.contains(searchQuery.trim(), ignoreCase = true) }
        }
}
