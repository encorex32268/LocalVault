package com.lihan.localvault.core.domain

data class VaultAttachment(
    val id: String = java.util.UUID.randomUUID().toString(),
    val fileName: String,
    val mimeType: String,
    val fileSize: Long,
    val encryptedFileName: String,
    val createdAt: Long = System.currentTimeMillis()
) {
    val isImage: Boolean
        get() = mimeType.startsWith("image/")
}
