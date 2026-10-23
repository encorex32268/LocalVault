package com.lihan.localvault.core.domain

sealed interface VaultContent {
    data class Account(
        val account: String,
        val password: String
    ): VaultContent

    data class Address(
        val zipCode: String,
        val address: String
    ): VaultContent

    data class Note(
        val content: String
    ): VaultContent
}