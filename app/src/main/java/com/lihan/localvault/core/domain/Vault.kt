package com.lihan.localvault.core.domain

data class Vault(
    val id: Long = 0L,
    val iconIndex: Int,
    val color: Long,
    val title: String,
    val updatedAt: Long,
    val category: VaultCategory,
)
