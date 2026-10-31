package com.lihan.localvault.core.storage

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.core.content.FileProvider
import androidx.security.crypto.EncryptedFile
import androidx.security.crypto.MasterKey
import com.lihan.localvault.core.domain.VaultAttachment
import com.lihan.localvault.core.domain.util.DataError
import com.lihan.localvault.core.domain.util.Result
import java.io.File
import java.io.InputStream
import java.util.UUID

interface EncryptedFileManager {
    fun saveEncryptedFile(sourceUri: Uri): Result<VaultAttachment, DataError.Local>
    fun saveEncryptedBytes(fileName: String, mimeType: String, bytes: ByteArray): Result<VaultAttachment, DataError.Local>
    fun openDecryptedStream(encryptedFileName: String): Result<InputStream, DataError.Local>
    fun readDecryptedBytes(encryptedFileName: String): Result<ByteArray, DataError.Local>
    fun deleteEncryptedFile(encryptedFileName: String): Boolean
    fun createCameraTempUri(): Pair<Uri?, File>
    fun deleteTempFile(file: File): Boolean
}

class AndroidEncryptedFileManager(
    private val context: Context
) : EncryptedFileManager {

    private val masterKey: MasterKey by lazy {
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
    }

    private val storageDir: File by lazy {
        File(context.filesDir, ATTACHMENTS_DIR).apply {
            if (!exists()) {
                mkdirs()
            }
        }
    }

    private val cameraTempDir: File by lazy {
        File(context.cacheDir, CAMERA_TEMP_DIR).apply {
            if (!exists()) {
                mkdirs()
            }
        }
    }

    override fun saveEncryptedFile(sourceUri: Uri): Result<VaultAttachment, DataError.Local> {
        return try {
            val contentResolver = context.contentResolver
            val (originalFileName, fileSize) = queryFileInfo(sourceUri)
            val mimeType = contentResolver.getType(sourceUri) ?: "application/octet-stream"

            val attachmentId = UUID.randomUUID().toString()
            val encryptedFileName = "$attachmentId.enc"
            val targetFile = File(storageDir, encryptedFileName)

            val encryptedFile = EncryptedFile.Builder(
                context,
                targetFile,
                masterKey,
                EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
            ).build()

            val actualSize = contentResolver.openInputStream(sourceUri)?.use { input ->
                encryptedFile.openFileOutput().use { output ->
                    input.copyTo(output)
                }
            } ?: return Result.Error(DataError.Local.NOT_FOUND)

            Result.Success(
                VaultAttachment(
                    id = attachmentId,
                    fileName = originalFileName,
                    mimeType = mimeType,
                    fileSize = if (fileSize > 0) fileSize else actualSize,
                    encryptedFileName = encryptedFileName,
                    createdAt = System.currentTimeMillis()
                )
            )
        } catch (e: Exception) {
            Result.Error(DataError.Local.CRYPTO_ERROR)
        }
    }

    override fun saveEncryptedBytes(
        fileName: String,
        mimeType: String,
        bytes: ByteArray
    ): Result<VaultAttachment, DataError.Local> {
        return try {
            val attachmentId = UUID.randomUUID().toString()
            val encryptedFileName = "$attachmentId.enc"
            val targetFile = File(storageDir, encryptedFileName)

            val encryptedFile = EncryptedFile.Builder(
                context,
                targetFile,
                masterKey,
                EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
            ).build()

            encryptedFile.openFileOutput().use { output ->
                output.write(bytes)
            }

            Result.Success(
                VaultAttachment(
                    id = attachmentId,
                    fileName = fileName,
                    mimeType = mimeType,
                    fileSize = bytes.size.toLong(),
                    encryptedFileName = encryptedFileName,
                    createdAt = System.currentTimeMillis()
                )
            )
        } catch (e: Exception) {
            Result.Error(DataError.Local.CRYPTO_ERROR)
        }
    }

    override fun openDecryptedStream(encryptedFileName: String): Result<InputStream, DataError.Local> {
        return try {
            val targetFile = File(storageDir, encryptedFileName)
            if (!targetFile.exists()) {
                return Result.Error(DataError.Local.NOT_FOUND)
            }

            val encryptedFile = EncryptedFile.Builder(
                context,
                targetFile,
                masterKey,
                EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
            ).build()

            Result.Success(encryptedFile.openFileInput())
        } catch (e: Exception) {
            Result.Error(DataError.Local.CRYPTO_ERROR)
        }
    }

    override fun readDecryptedBytes(encryptedFileName: String): Result<ByteArray, DataError.Local> {
        return when (val streamResult = openDecryptedStream(encryptedFileName)) {
            is Result.Error -> Result.Error(streamResult.error)
            is Result.Success -> {
                try {
                    streamResult.data.use { input ->
                        Result.Success(input.readBytes())
                    }
                } catch (e: Exception) {
                    Result.Error(DataError.Local.CORRUPTED_DATA)
                }
            }
        }
    }

    override fun deleteEncryptedFile(encryptedFileName: String): Boolean {
        val targetFile = File(storageDir, encryptedFileName)
        return if (targetFile.exists()) {
            targetFile.delete()
        } else {
            false
        }
    }

    override fun createCameraTempUri(): Pair<Uri?, File> {
        val tempFile = File.createTempFile("camera_", ".jpg", cameraTempDir)
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            tempFile
        )
        return Pair(uri, tempFile)
    }

    override fun deleteTempFile(file: File): Boolean {
        return if (file.exists()) file.delete() else false
    }

    private fun queryFileInfo(uri: Uri): Pair<String, Long> {
        var name = "attachment_${System.currentTimeMillis()}"
        var size = 0L

        try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (cursor.moveToFirst()) {
                    if (nameIndex != -1 && !cursor.isNull(nameIndex)) {
                        name = cursor.getString(nameIndex)
                    }
                    if (sizeIndex != -1 && !cursor.isNull(sizeIndex)) {
                        size = cursor.getLong(sizeIndex)
                    }
                }
            }
        } catch (_: Exception) {
        }

        return Pair(name, size)
    }

    companion object {
        private const val ATTACHMENTS_DIR = "vault_attachments"
        private const val CAMERA_TEMP_DIR = "camera"
    }
}
