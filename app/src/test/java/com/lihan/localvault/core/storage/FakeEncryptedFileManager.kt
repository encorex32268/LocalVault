package com.lihan.localvault.core.storage

import android.net.Uri
import com.lihan.localvault.core.domain.VaultAttachment
import com.lihan.localvault.core.domain.util.DataError
import com.lihan.localvault.core.domain.util.Result
import java.io.ByteArrayInputStream
import java.io.File
import java.io.InputStream
import java.util.UUID

class FakeEncryptedFileManager : EncryptedFileManager {
    val savedFiles = mutableMapOf<String, ByteArray>()
    val deletedFiles = mutableListOf<String>()

    override fun saveEncryptedFile(sourceUri: Uri): Result<VaultAttachment, DataError.Local> {
        val id = UUID.randomUUID().toString()
        val encName = "$id.enc"
        savedFiles[encName] = byteArrayOf(1, 2, 3)
        return Result.Success(
            VaultAttachment(
                id = id,
                fileName = "test.jpg",
                mimeType = "image/jpeg",
                fileSize = 3L,
                encryptedFileName = encName
            )
        )
    }

    override fun saveEncryptedBytes(
        fileName: String,
        mimeType: String,
        bytes: ByteArray
    ): Result<VaultAttachment, DataError.Local> {
        val id = UUID.randomUUID().toString()
        val encName = "$id.enc"
        savedFiles[encName] = bytes
        return Result.Success(
            VaultAttachment(
                id = id,
                fileName = fileName,
                mimeType = mimeType,
                fileSize = bytes.size.toLong(),
                encryptedFileName = encName
            )
        )
    }

    override fun openDecryptedStream(encryptedFileName: String): Result<InputStream, DataError.Local> {
        val bytes = savedFiles[encryptedFileName] ?: return Result.Error(DataError.Local.NOT_FOUND)
        return Result.Success(ByteArrayInputStream(bytes))
    }

    override fun readDecryptedBytes(encryptedFileName: String): Result<ByteArray, DataError.Local> {
        val bytes = savedFiles[encryptedFileName] ?: return Result.Error(DataError.Local.NOT_FOUND)
        return Result.Success(bytes)
    }

    override fun deleteEncryptedFile(encryptedFileName: String): Boolean {
        deletedFiles.add(encryptedFileName)
        return savedFiles.remove(encryptedFileName) != null
    }

    override fun createCameraTempUri(): Pair<Uri?, File> {
        val dummyFile = File.createTempFile("fake_cam", ".jpg")
        return Pair(null, dummyFile)
    }

    override fun deleteTempFile(file: File): Boolean {
        return file.delete()
    }
}
