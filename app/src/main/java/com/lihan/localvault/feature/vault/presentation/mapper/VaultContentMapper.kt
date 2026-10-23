package com.lihan.localvault.feature.vault.presentation.mapper

import androidx.compose.foundation.text.input.TextFieldState
import com.lihan.localvault.core.domain.VaultContent
import com.lihan.localvault.feature.vault.presentation.model.VaultContentUi

fun VaultContent.toUi(): VaultContentUi {
    return when (this) {
        is VaultContent.Account -> VaultContentUi.Account(
            accountState = TextFieldState(account),
            passwordState = TextFieldState(password)
        )
        is VaultContent.Address -> VaultContentUi.Address(
            zipCodeState = TextFieldState(zipCode),
            addressState = TextFieldState(address)
        )
        is VaultContent.Note -> VaultContentUi.Note(
            noteState = TextFieldState(content)
        )
    }
}

fun VaultContentUi.toDomain(): VaultContent {
    return when (this) {
        is VaultContentUi.Account -> VaultContent.Account(
            account = accountState.text.toString(),
            password = passwordState.text.toString()
        )
        is VaultContentUi.Address -> VaultContent.Address(
            zipCode = zipCodeState.text.toString(),
            address = addressState.text.toString()
        )
        is VaultContentUi.Note -> VaultContent.Note(
            content = noteState.text.toString()
        )
    }
}