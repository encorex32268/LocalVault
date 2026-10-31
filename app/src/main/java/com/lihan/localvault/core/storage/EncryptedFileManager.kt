package com.lihan.localvault.core.storage

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.provider.OpenableColumns
import androidx.core.content.FileProvider
import androidx.security.crypto.EncryptedFile
import androidx.security.crypto.MasterKey
import com.lihan.localvault.core.domain.VaultAttachment
import com.lihan.localvault.core.domain.util.DataError
import com.lihan.localvault.core.domain.util.Result
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import java.util.UUID
import kotlin.coroutines.coroutineContext

interface EncryptedFileManager {
    suspend fun saveEncryptedFile(sourceUri: Uri): Result<VaultAttachment, DataError.Local>
    suspend fun saveEncryptedBytes(fileName: String, mimeType: String, bytes: ByteArray): Result<VaultAttachment, DataError.Local>
    suspend fun openDecryptedStream(encryptedFileName: String): Result<InputStream, DataError.Local>
    suspend fun readDecryptedBytes(encryptedFileName: String): Result<ByteArray, DataError.Local>
    suspend fun deleteEncryptedFile(encryptedFileName: String): Boolean
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

    override suspend fun saveEncryptedFile(sourceUri: Uri): Result<VaultAttachment, DataError.Local> = withContext(Dispatchers.IO) {
        val contentResolver = context.contentResolver
        val (originalFileName, fileSize) = queryFileInfo(sourceUri)
        val mimeType = contentResolver.getType(sourceUri) ?: "application/octet-stream"

        val attachmentId = UUID.randomUUID().toString()
        val encryptedFileName = "$attachmentId.enc"
        val targetFile = File(storageDir, encryptedFileName)
        var isSuccess = false

        try {
            coroutineContext.ensureActive()

            val encryptedFile = EncryptedFile.Builder(
                context,
                targetFile,
                masterKey,
                EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
            ).build()

            var isCompressed = false
            var actualSize = 0L

            if (mimeType.startsWith("image/") && mimeType != "image/gif" && mimeType != "image/svg+xml") {
                val compressedBytes = tryCompressImage(sourceUri, encryptedFile)
                if (compressedBytes != null && compressedBytes > 0) {
                    isCompressed = true
                    actualSize = compressedBytes
                } else {
                    targetFile.delete()
                }
            }

            coroutineContext.ensureActive()

            if (!isCompressed) {
                val fallbackEncryptedFile = EncryptedFile.Builder(
                    context,
                    targetFile,
                    masterKey,
                    EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
                ).build()
                actualSize = contentResolver.openInputStream(sourceUri)?.use { input ->
                    fallbackEncryptedFile.openFileOutput().use { output ->
                        input.copyTo(output)
                    }
                } ?: return@withContext Result.Error(DataError.Local.NOT_FOUND)
            }

            coroutineContext.ensureActive()

            val finalMimeType = if (isCompressed) "image/jpeg" else mimeType
            val finalFileName = if (isCompressed && !originalFileName.endsWith(".jpg", ignoreCase = true) && !originalFileName.endsWith(".jpeg", ignoreCase = true)) {
                "$originalFileName.jpg"
            } else {
                originalFileName
            }

            isSuccess = true
            Result.Success(
                VaultAttachment(
                    id = attachmentId,
                    fileName = finalFileName,
                    mimeType = finalMimeType,
                    fileSize = if (isCompressed) actualSize else if (fileSize > 0) fileSize else actualSize,
                    encryptedFileName = encryptedFileName,
                    createdAt = System.currentTimeMillis()
                )
            )
        } catch (e: CancellationException) {
            targetFile.delete()
            throw e
        } catch (e: Exception) {
            targetFile.delete()
            Result.Error(DataError.Local.CRYPTO_ERROR)
        } finally {
            if (!isSuccess && targetFile.exists()) {
                targetFile.delete()
            }
        }
    }

    private suspend fun tryCompressImage(sourceUri: Uri, encryptedFile: EncryptedFile): Long? {
        val contentResolver = context.contentResolver
        var decodedBitmap: Bitmap? = null
        var rotatedBitmap: Bitmap? = null
        var scaledBitmap: Bitmap? = null

        return try {
            currentCoroutineContext().ensureActive()

            val boundsOptions = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            contentResolver.openInputStream(sourceUri)?.use { input ->
                BitmapFactory.decodeStream(input, null, boundsOptions)
            } ?: return null

            val origWidth = boundsOptions.outWidth
            val origHeight = boundsOptions.outHeight
            if (origWidth <= 0 || origHeight <= 0) return null

            coroutineContext.ensureActive()

            var sampleSize = 1
            val maxDim = maxOf(origWidth, origHeight)
            while (maxDim / sampleSize > 1920 * 1.5) {
                sampleSize *= 2
            }

            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }

            decodedBitmap = contentResolver.openInputStream(sourceUri)?.use { input ->
                BitmapFactory.decodeStream(input, null, decodeOptions)
            } ?: return null

            coroutineContext.ensureActive()

            var currentBitmap = decodedBitmap
            try {
                contentResolver.openInputStream(sourceUri)?.use { input ->
                    val exif = ExifInterface(input)
                    val orientation = exif.getAttributeInt(
                        ExifInterface.TAG_ORIENTATION,
                        ExifInterface.ORIENTATION_NORMAL
                    )
                    val matrix = Matrix()
                    when (orientation) {
                        ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                        ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                        ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                        ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
                        ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
                    }
                    if (!matrix.isIdentity) {
                        rotatedBitmap = Bitmap.createBitmap(currentBitmap, 0, 0, currentBitmap.width, currentBitmap.height, matrix, true)
                        if (rotatedBitmap != currentBitmap) {
                            currentBitmap = rotatedBitmap
                        }
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {}

            coroutineContext.ensureActive()

            val currentMax = maxOf(currentBitmap.width, currentBitmap.height)
            if (currentMax > 1920) {
                val scale = 1920f / currentMax
                val targetW = (currentBitmap.width * scale).toInt().coerceAtLeast(1)
                val targetH = (currentBitmap.height * scale).toInt().coerceAtLeast(1)
                scaledBitmap = Bitmap.createScaledBitmap(currentBitmap, targetW, targetH, true)
                if (scaledBitmap != currentBitmap) {
                    currentBitmap = scaledBitmap
                }
            }

            currentCoroutineContext().ensureActive()

            var bytesWritten = 0L
            encryptedFile.openFileOutput().use { output ->
                val countingOut = object : java.io.FilterOutputStream(output) {
                    override fun write(b: Int) {
                        super.write(b)
                        bytesWritten++
                    }
                    override fun write(b: ByteArray, off: Int, len: Int) {
                        out.write(b, off, len)
                        bytesWritten += len
                    }
                }
                currentBitmap.compress(Bitmap.CompressFormat.JPEG, 85, countingOut)
                countingOut.flush()
            }
            bytesWritten
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            null
        } finally {
            decodedBitmap?.recycle()
            if (rotatedBitmap != null && rotatedBitmap != decodedBitmap) {
                rotatedBitmap.recycle()
            }
            if (scaledBitmap != null && scaledBitmap != rotatedBitmap && scaledBitmap != decodedBitmap) {
                scaledBitmap.recycle()
            }
        }
    }

    override suspend fun saveEncryptedBytes(
        fileName: String,
        mimeType: String,
        bytes: ByteArray
    ): Result<VaultAttachment, DataError.Local> = withContext(Dispatchers.IO) {
        val attachmentId = UUID.randomUUID().toString()
        val encryptedFileName = "$attachmentId.enc"
        val targetFile = File(storageDir, encryptedFileName)
        var isSuccess = false

        try {
            coroutineContext.ensureActive()

            val encryptedFile = EncryptedFile.Builder(
                context,
                targetFile,
                masterKey,
                EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
            ).build()

            encryptedFile.openFileOutput().use { output ->
                output.write(bytes)
            }

            isSuccess = true
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
        } catch (e: CancellationException) {
            targetFile.delete()
            throw e
        } catch (e: Exception) {
            targetFile.delete()
            Result.Error(DataError.Local.CRYPTO_ERROR)
        } finally {
            if (!isSuccess && targetFile.exists()) {
                targetFile.delete()
            }
        }
    }

    override suspend fun openDecryptedStream(encryptedFileName: String): Result<InputStream, DataError.Local> = withContext(Dispatchers.IO) {
        try {
            val targetFile = File(storageDir, encryptedFileName)
            if (!targetFile.exists()) {
                return@withContext Result.Error(DataError.Local.NOT_FOUND)
            }

            val encryptedFile = EncryptedFile.Builder(
                context,
                targetFile,
                masterKey,
                EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
            ).build()

            Result.Success(encryptedFile.openFileInput())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.Error(DataError.Local.CRYPTO_ERROR)
        }
    }

    override suspend fun readDecryptedBytes(encryptedFileName: String): Result<ByteArray, DataError.Local> = withContext(Dispatchers.IO) {
        when (val streamResult = openDecryptedStream(encryptedFileName)) {
            is Result.Error -> Result.Error(streamResult.error)
            is Result.Success -> {
                try {
                    streamResult.data.use { input ->
                        Result.Success(input.readBytes())
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Result.Error(DataError.Local.CORRUPTED_DATA)
                }
            }
        }
    }

    override suspend fun deleteEncryptedFile(encryptedFileName: String): Boolean = withContext(Dispatchers.IO) {
        val targetFile = File(storageDir, encryptedFileName)
        targetFile.exists() && targetFile.delete()
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
