package com.lihan.localvault.feature.vault.presentation.mapper

import androidx.compose.foundation.text.input.TextFieldState
import com.lihan.localvault.core.domain.VaultContent
import com.lihan.localvault.core.domain.VaultField
import com.lihan.localvault.feature.vault.presentation.model.VaultContentUi
import com.lihan.localvault.feature.vault.presentation.model.VaultFieldUi

fun VaultContent.toUi(): VaultContentUi {
    return VaultContentUi(
        fields = fields.map { field ->
            VaultFieldUi(
                id = field.id,
                type = field.type,
                label = field.label,
                state = TextFieldState(field.value),
                isPasswordVisible = false
            )
        }
    )
}

fun VaultContentUi.toDomain(): VaultContent {
    return VaultContent(
        fields = fields.map { fieldUi ->
            VaultField(
                id = fieldUi.id,
                type = fieldUi.type,
                label = fieldUi.label,
                value = fieldUi.state.text.toString()
            )
        }
    )
}
