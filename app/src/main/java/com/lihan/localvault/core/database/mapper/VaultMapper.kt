package com.lihan.localvault.core.database.mapper

import com.lihan.localvault.core.database.dto.AccountContentDto
import com.lihan.localvault.core.database.dto.AddressContentDto
import com.lihan.localvault.core.database.dto.NoteContentDto
import com.lihan.localvault.core.database.entity.VaultEntity
import com.lihan.localvault.core.database.entity.VaultSummaryTuple
import com.lihan.localvault.core.domain.Vault
import com.lihan.localvault.core.domain.VaultCategory
import com.lihan.localvault.core.domain.VaultContent
import kotlinx.serialization.json.Json

fun VaultSummaryTuple.toVault(): Vault {
    return Vault(
        id = id,
        iconIndex = iconIndex,
        color = color,
        title = title,
        updatedAt = updatedAt,
        category = VaultCategory.valueOf(category)
    )
}

fun Vault.toVaultEntity(
    ciphertext: ByteArray,
    iv: ByteArray
): VaultEntity {
    return VaultEntity(
        id = id,
        iconIndex = iconIndex,
        color = color,
        title = title,
        category = category.name,
        ciphertext = ciphertext,
        iv = iv,
        updatedAt = updatedAt
    )
}

fun VaultContent.toJsonString(json: Json): String {
    return when (this) {
        is VaultContent.Account -> json.encodeToString(
            AccountContentDto(account = account, password = password)
        )
        is VaultContent.Address -> json.encodeToString(
            AddressContentDto(zipCode = zipCode, address = address)
        )
        is VaultContent.Note -> json.encodeToString(
            NoteContentDto(content = content)
        )
    }
}

fun String.toVaultContent(
    category: VaultCategory,
    json: Json
): VaultContent {
    return when (category) {
        VaultCategory.ACCOUNT -> {
            val dto = json.decodeFromString<AccountContentDto>(this)
            VaultContent.Account(account = dto.account, password = dto.password)
        }
        VaultCategory.ADDRESS -> {
            val dto = json.decodeFromString<AddressContentDto>(this)
            VaultContent.Address(zipCode = dto.zipCode, address = dto.address)
        }
        VaultCategory.NOTE -> {
            val dto = json.decodeFromString<NoteContentDto>(this)
            VaultContent.Note(content = dto.content)
        }
    }
}
