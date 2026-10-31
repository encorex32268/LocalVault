package com.lihan.localvault.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "vault_items")
data class VaultEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val iconIndex: Int,
    val color: Long,
    val title: String,
    val category: String,
    val ciphertext: ByteArray,
    val iv: ByteArray,
    val updatedAt: Long,
    val hasAttachments: Boolean = false
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as VaultEntity

        if (id != other.id) return false
        if (iconIndex != other.iconIndex) return false
        if (color != other.color) return false
        if (title != other.title) return false
        if (category != other.category) return false
        if (!ciphertext.contentEquals(other.ciphertext)) return false
        if (!iv.contentEquals(other.iv)) return false
        if (updatedAt != other.updatedAt) return false
        if (hasAttachments != other.hasAttachments) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + iconIndex
        result = 31 * result + color.hashCode()
        result = 31 * result + title.hashCode()
        result = 31 * result + category.hashCode()
        result = 31 * result + ciphertext.contentHashCode()
        result = 31 * result + iv.contentHashCode()
        result = 31 * result + updatedAt.hashCode()
        result = 31 * result + hasAttachments.hashCode()
        return result
    }
}

data class VaultSummaryTuple(
    val id: Long,
    val iconIndex: Int,
    val color: Long,
    val title: String,
    val category: String,
    val updatedAt: Long,
    val hasAttachments: Boolean = false
)

data class VaultEncryptedTuple(
    val category: String,
    val ciphertext: ByteArray,
    val iv: ByteArray
)
