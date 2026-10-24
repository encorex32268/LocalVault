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

data class VaultContentUi(
    val fields: List<VaultFieldUi> = emptyList()
)
