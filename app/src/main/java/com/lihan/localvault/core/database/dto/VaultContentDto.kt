package com.lihan.localvault.core.database.dto

import kotlinx.serialization.Serializable

@Serializable
data class VaultFieldDto(
    val id: String = "",
    val type: String = "TEXT",
    val label: String = "",
    val value: String = ""
)

@Serializable
data class DynamicVaultContentDto(
    val fields: List<VaultFieldDto> = emptyList()
)
