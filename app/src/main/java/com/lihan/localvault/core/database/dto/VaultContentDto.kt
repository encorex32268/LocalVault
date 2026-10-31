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
data class VaultAttachmentDto(
    val id: String = "",
    val fileName: String = "",
    val mimeType: String = "",
    val fileSize: Long = 0L,
    val encryptedFileName: String = "",
    val createdAt: Long = 0L
)

@Serializable
data class DynamicVaultContentDto(
    val fields: List<VaultFieldDto> = emptyList(),
    val attachments: List<VaultAttachmentDto> = emptyList()
)
