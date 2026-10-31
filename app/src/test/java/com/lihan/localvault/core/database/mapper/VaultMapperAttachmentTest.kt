package com.lihan.localvault.core.database.mapper

import com.lihan.localvault.core.domain.FieldType
import com.lihan.localvault.core.domain.VaultAttachment
import com.lihan.localvault.core.domain.VaultContent
import com.lihan.localvault.core.domain.VaultField
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VaultMapperAttachmentTest {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    @Test
    fun vaultContent_withAttachments_serializesAndDeserializesCorrectly() {
        val originalContent = VaultContent(
            fields = listOf(
                VaultField(id = "f1", type = FieldType.SINGLE_LINE, label = "Account", value = "user123")
            ),
            attachments = listOf(
                VaultAttachment(
                    id = "att-1",
                    fileName = "passport.jpg",
                    mimeType = "image/jpeg",
                    fileSize = 1048576L,
                    encryptedFileName = "att-1.enc",
                    createdAt = 1700000000L
                ),
                VaultAttachment(
                    id = "att-2",
                    fileName = "contract.pdf",
                    mimeType = "application/pdf",
                    fileSize = 2097152L,
                    encryptedFileName = "att-2.enc",
                    createdAt = 1700000010L
                )
            )
        )

        val jsonString = originalContent.toJsonString(json)
        val restoredContent = jsonString.toVaultContent(json = json)

        assertEquals(1, restoredContent.fields.size)
        assertEquals("Account", restoredContent.fields[0].label)
        assertEquals(2, restoredContent.attachments.size)

        val firstAttachment = restoredContent.attachments[0]
        assertEquals("att-1", firstAttachment.id)
        assertEquals("passport.jpg", firstAttachment.fileName)
        assertEquals("image/jpeg", firstAttachment.mimeType)
        assertEquals(1048576L, firstAttachment.fileSize)
        assertEquals("att-1.enc", firstAttachment.encryptedFileName)
        assertTrue(firstAttachment.isImage)

        val secondAttachment = restoredContent.attachments[1]
        assertEquals("att-2", secondAttachment.id)
        assertEquals("contract.pdf", secondAttachment.fileName)
        assertEquals("application/pdf", secondAttachment.mimeType)
        assertEquals(2097152L, secondAttachment.fileSize)
        assertEquals("att-2.enc", secondAttachment.encryptedFileName)
        assertTrue(!secondAttachment.isImage)
    }

    @Test
    fun legacyJson_withoutAttachments_deserializesWithEmptyAttachmentsList() {
        val legacyJson = """{"fields":[{"id":"f1","type":"SINGLE_LINE","label":"User","value":"encore"}]}"""
        val content = legacyJson.toVaultContent(json = json)

        assertEquals(1, content.fields.size)
        assertEquals(0, content.attachments.size)
    }
}
