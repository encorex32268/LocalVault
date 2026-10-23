package com.lihan.localvault.feature.vault.presentation.vault.components

import androidx.compose.foundation.text.input.OutputTransformation
import androidx.compose.foundation.text.input.TextFieldBuffer

class PasswordOutputTransformation: OutputTransformation {
    override fun TextFieldBuffer.transformOutput() {
        replace(0,length,"*".repeat(length))
    }
}