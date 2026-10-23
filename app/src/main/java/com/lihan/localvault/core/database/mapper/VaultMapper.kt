package com.lihan.localvault.core.database.mapper

import com.lihan.localvault.core.database.dto.DynamicVaultContentDto
import com.lihan.localvault.core.database.dto.VaultFieldDto
import com.lihan.localvault.core.database.entity.VaultEntity
import com.lihan.localvault.core.database.entity.VaultSummaryTuple
import com.lihan.localvault.core.domain.FieldType
import com.lihan.localvault.core.domain.Vault
import com.lihan.localvault.core.domain.VaultCategory
import com.lihan.localvault.core.domain.VaultContent
import com.lihan.localvault.core.domain.VaultField
import kotlinx.serialization.json.Json

fun VaultSummaryTuple.toVault(): Vault {
    return Vault(
        id = id,
        iconIndex = iconIndex,
        color = color,
        title = title,
        updatedAt = updatedAt,
        category = try {
            VaultCategory.valueOf(category)
        } catch (e: Exception) {
            VaultCategory.CUSTOM
        }
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
    val dto = DynamicVaultContentDto(
        fields = fields.map {
            VaultFieldDto(
                id = it.id,
                type = it.type.name,
                label = it.label,
                value = it.value
            )
        }
    )
    return json.encodeToString(dto)
}

fun String.toVaultContent(
    category: VaultCategory = VaultCategory.CUSTOM,
    json: Json
): VaultContent {
    return try {
        val dynamicDto = json.decodeFromString<DynamicVaultContentDto>(this)
        VaultContent(
            fields = dynamicDto.fields.map { dto ->
                VaultField(
                    id = dto.id.ifEmpty { java.util.UUID.randomUUID().toString() },
                    type = try {
                        FieldType.valueOf(dto.type)
                    } catch (e: Exception) {
                        when (dto.type) {
                            "TEXT" -> FieldType.SINGLE_LINE
                            "NOTE" -> FieldType.MULTI_LINE
                            else -> FieldType.SINGLE_LINE
                        }
                    },
                    label = dto.label,
                    value = dto.value
                )
            }
        )
    } catch (e: Exception) {
        VaultContent()
    }
}
