package com.lihan.localvault.feature.vault.presentation.vault.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.KeyboardActionHandler
import androidx.compose.foundation.text.input.OutputTransformation
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.lihan.localvault.R
import com.lihan.localvault.core.domain.VaultCategory
import com.lihan.localvault.core.domain.VaultContent
import com.lihan.localvault.core.presentation.ui.theme.LocalVaultTheme
import com.lihan.localvault.core.presentation.ui.theme.OnBackground
import com.lihan.localvault.core.presentation.ui.theme.Placeholder
import com.lihan.localvault.core.presentation.ui.theme.Primary

@Composable
fun VaultTextField(
    textFieldState: TextFieldState,
    placeholder: String,
    modifier: Modifier = Modifier,
    lineLimits: TextFieldLineLimits = TextFieldLineLimits.Default,
    onKeyboardAction: KeyboardActionHandler? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    trailingIcon: @Composable (() -> Unit)?=null,
    outputTransformation: OutputTransformation? = null,
) {

    BasicTextField(
        state = textFieldState,
        keyboardOptions = keyboardOptions,
        onKeyboardAction = onKeyboardAction,
        outputTransformation = outputTransformation,
        lineLimits = lineLimits,
        cursorBrush = SolidColor(Primary),
        decorator = { innerField ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.CenterStart
                ){
                    if (textFieldState.text.isBlank()){
                        Text(
                            text = placeholder,
                            color = Placeholder
                        )
                    }
                    innerField()
                }
                trailingIcon?.invoke()

            }

        },
        textStyle = MaterialTheme.typography.bodyMedium.copy(
            fontWeight = FontWeight.Medium,
            color = OnBackground
        ),
        modifier = modifier
            .border(
                width = 1.dp,
                shape = RoundedCornerShape(24.dp),
                color = Placeholder
            )
            .padding(vertical = 12.dp, horizontal = 16.dp),
        scrollState = rememberScrollState(),


    )

}

@PreviewLightDark
@Composable
private fun VaultTextFieldPreview() {
    LocalVaultTheme {


    }
}