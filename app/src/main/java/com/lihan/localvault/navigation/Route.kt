package com.lihan.localvault.navigation

import kotlinx.serialization.Serializable

@Serializable
sealed interface Route {

    @Serializable
    data object VaultList : Route

    @Serializable
    data class VaultEdit(val vaultId: Long? = null) : Route
}
