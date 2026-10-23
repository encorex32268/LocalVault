package com.lihan.localvault.core.domain

enum class FieldType {
    TEXT,
    PASSWORD,
    NOTE
}

data class VaultField(
    val id: String = java.util.UUID.randomUUID().toString(),
    val type: FieldType,
    val label: String,
    val value: String
)

data class VaultContent(
    val fields: List<VaultField> = emptyList()
)
