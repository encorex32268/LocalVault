package com.lihan.localvault.core.database.dto

import kotlinx.serialization.Serializable

@Serializable
data class AccountContentDto(
    val account: String,
    val password: String
)

@Serializable
data class AddressContentDto(
    val zipCode: String,
    val address: String
)

@Serializable
data class NoteContentDto(
    val content: String
)
