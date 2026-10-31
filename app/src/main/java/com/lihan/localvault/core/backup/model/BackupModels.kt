package com.lihan.localvault.core.backup.model

import kotlinx.serialization.Serializable

@Serializable
data class BackupEnvelope(
    val version: Int = 1,
    val createdAt: Long = System.currentTimeMillis(),
    val kdf: String = "PBKDF2WithHmacSHA256",
    val iterations: Int = 100_000,
    val salt: String,
    val iv: String,
    val ciphertext: String
)

@Serializable
data class VaultBackupPayload(
    val version: Int = 1,
    val exportedAt: Long = System.currentTimeMillis(),
    val items: List<VaultBackupItem>
)

@Serializable
data class VaultBackupItem(
    val title: String,
    val color: Long,
    val iconIndex: Int,
    val category: String,
    val updatedAt: Long,
    val fields: List<VaultBackupField>,
    val attachments: List<VaultBackupAttachment> = emptyList()
)

@Serializable
data class VaultBackupAttachment(
    val fileName: String,
    val mimeType: String,
    val dataBase64: String
)

@Serializable
data class VaultBackupField(
    val id: String,
    val label: String,
    val value: String,
    val type: String
)
