package com.lihan.localvault.feature.vault.presentation.vault

import com.lihan.localvault.feature.vault.presentation.model.VaultUi

data class VaultState(
    val isLoading: Boolean = false,
    val items: List<VaultUi> = emptyList(),
    val searchQuery: String = "",
    val isSearchActive: Boolean = false,
    val deleteTargetId: Long? = null,
    val isDarkTheme: Boolean = true,
    val isAuthenticatedSession: Boolean = false
) {
    val filteredItems: List<VaultUi>
        get() = if (searchQuery.isBlank()) {
            items
        } else {
            items.filter { it.title.contains(searchQuery.trim(), ignoreCase = true) }
        }
}
