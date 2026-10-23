package com.lihan.localvault.feature.vault.presentation.mapper

import com.lihan.localvault.core.domain.Vault
import com.lihan.localvault.feature.vault.presentation.model.VaultUi

fun Vault.toUi(): VaultUi {
    return VaultUi(
        id = id,
        iconIndex = iconIndex,
        color = color,
        title = title,
        updatedAt = updatedAt,
        category = category
    )
}

