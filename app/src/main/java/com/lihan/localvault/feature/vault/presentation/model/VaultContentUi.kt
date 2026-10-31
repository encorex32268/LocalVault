package com.lihan.localvault.feature.vault.presentation.model

import androidx.compose.foundation.text.input.TextFieldState
import com.lihan.localvault.core.domain.FieldType

data class VaultFieldUi(
    val id: String = java.util.UUID.randomUUID().toString(),
    val type: FieldType,
    val labelState: TextFieldState = TextFieldState(),
    val state: TextFieldState = TextFieldState(),
    val isPasswordVisible: Boolean = false
) {
    constructor(
        id: String = java.util.UUID.randomUUID().toString(),
        type: FieldType,
        label: String = "",
        state: TextFieldState = TextFieldState(),
        isPasswordVisible: Boolean = false
    ) : this(
        id = id,
        type = type,
        labelState = TextFieldState(label),
        state = state,
        isPasswordVisible = isPasswordVisible
    )

    val label: String get() = labelState.text.toString()
}

data class VaultAttachmentUi(
    val id: String = java.util.UUID.randomUUID().toString(),
    val fileName: String,
    val mimeType: String,
    val fileSize: Long,
    val encryptedFileName: String,
    val isImage: Boolean = mimeType.startsWith("image/"),
    val formattedSize: String = formatFileSize(fileSize)
)

data class VaultContentUi(
    val fields: List<VaultFieldUi> = emptyList(),
    val attachments: List<VaultAttachmentUi> = emptyList()
)

private fun formatFileSize(size: Long): String {
    if (size <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB")
    val digitGroups = (Math.log10(size.toDouble()) / Math.log10(1024.0)).toInt().coerceIn(0, units.size - 1)
    val formatted = String.format(java.util.Locale.US, "%.1f", size / Math.pow(1024.0, digitGroups.toDouble()))
    return "$formatted ${units[digitGroups]}"
}
