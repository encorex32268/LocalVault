package com.lihan.localvault.feature.vault.presentation.model

import androidx.compose.foundation.text.input.TextFieldState
import com.lihan.localvault.core.domain.FieldType

data class VaultFieldUi(
    val id: String = java.util.UUID.randomUUID().toString(),
    val type: FieldType,
    val label: String,
    val state: TextFieldState = TextFieldState(),
    val isPasswordVisible: Boolean = false
)

data class VaultContentUi(
    val fields: List<VaultFieldUi> = emptyList()
)
