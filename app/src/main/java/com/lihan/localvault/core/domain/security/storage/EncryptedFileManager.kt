package com.lihan.localvault.core.domain.security.storage

import android.net.Uri
import com.lihan.localvault.core.domain.VaultAttachment
import com.lihan.localvault.core.domain.util.DataError
import com.lihan.localvault.core.domain.util.Result
import java.io.File
import java.io.InputStream

interface EncryptedFileManager {
    suspend fun saveEncryptedFile(sourceUri: Uri): Result<VaultAttachment, DataError.Local>
    suspend fun saveEncryptedBytes(fileName: String, mimeType: String, bytes: ByteArray): Result<VaultAttachment, DataError.Local>
    suspend fun openDecryptedStream(encryptedFileName: String): Result<InputStream, DataError.Local>
    suspend fun readDecryptedBytes(encryptedFileName: String): Result<ByteArray, DataError.Local>
    suspend fun deleteEncryptedFile(encryptedFileName: String): Boolean
    fun createCameraTempUri(): Pair<Uri?, File>
    fun deleteTempFile(file: File): Boolean
}