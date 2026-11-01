package com.lihan.localvault.core.presentation.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.lihan.localvault.core.domain.util.Result
import com.lihan.localvault.core.domain.security.storage.EncryptedFileManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.compose.koinInject

@Composable
fun EncryptedImageView(
    encryptedFileName: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    encryptedFileManager: EncryptedFileManager = koinInject()
) {
    val imageBytesState = produceState<ByteArray?>(initialValue = null, key1 = encryptedFileName) {
        value = withContext(Dispatchers.IO) {
            when (val result = encryptedFileManager.readDecryptedBytes(encryptedFileName)) {
                is Result.Success -> result.data
                is Result.Error -> null
            }
        }
    }

    val imageBytes = imageBytesState.value
    if (imageBytes != null) {
        AsyncImage(
            model = imageBytes,
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = contentScale
        )
    } else {
        Box(
            modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                strokeWidth = 2.dp
            )
        }
    }
}
