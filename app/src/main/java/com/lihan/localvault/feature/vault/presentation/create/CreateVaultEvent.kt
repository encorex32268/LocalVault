package com.lihan.localvault.feature.vault.presentation.create

import android.net.Uri
import java.io.File

sealed interface CreateVaultEvent {
    data object SaveSuccess : CreateVaultEvent
    data class LaunchCamera(val uri: Uri?, val tempFile: File) : CreateVaultEvent
    data object LaunchPhotoPicker : CreateVaultEvent
    data object LaunchDocumentPicker : CreateVaultEvent
}
