package com.lihan.localvault.feature.vault.presentation.model

import androidx.compose.foundation.text.input.TextFieldState

sealed interface VaultContentUi {
    data class Account(
        val accountState: TextFieldState,
        val passwordState: TextFieldState
    ) : VaultContentUi
    data class Address(
        val zipCodeState: TextFieldState,
        val addressState: TextFieldState
    ) : VaultContentUi
    data class Note(
        val noteState: TextFieldState
    ) : VaultContentUi
}