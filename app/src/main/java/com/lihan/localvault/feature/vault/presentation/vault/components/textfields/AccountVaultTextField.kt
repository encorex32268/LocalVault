package com.lihan.localvault.feature.vault.presentation.vault.components.textfields

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.lihan.localvault.R
import com.lihan.localvault.core.presentation.ui.theme.LocalVaultTheme
import com.lihan.localvault.core.presentation.ui.theme.OnBackground
import com.lihan.localvault.core.presentation.ui.theme.Primary
import com.lihan.localvault.feature.vault.presentation.vault.components.PasswordOutputTransformation
import com.lihan.localvault.feature.vault.presentation.vault.components.VaultTextField

@Composable
fun AccountVaultTextField(
    accountTextFieldState: TextFieldState,
    passwordTextFieldState: TextFieldState,
    onAccountCopyClick: (String) -> Unit,
    onPasswordCopyClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var isPasswordVisible by remember { mutableStateOf(false) }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = stringResource(R.string.account),
            fontWeight = FontWeight.SemiBold
        )
        VaultTextField(
            textFieldState = accountTextFieldState,
            placeholder = stringResource(R.string.account_placeholder),
            trailingIcon = {
                IconButton(
                    modifier = Modifier.size(32.dp),
                    onClick = {
                        onAccountCopyClick(accountTextFieldState.text.trim().toString())
                    },
                    shape = CircleShape,
                ) {
                    Icon(
                        modifier = Modifier.size(24.dp),
                        imageVector = ImageVector.vectorResource(R.drawable.copy),
                        contentDescription = null,
                        tint = OnBackground
                    )
                }
            }
        )

        Text(
            text = stringResource(R.string.password),
            fontWeight = FontWeight.SemiBold
        )
        VaultTextField(
            textFieldState = passwordTextFieldState,
            outputTransformation = if (isPasswordVisible){
                null
            }else PasswordOutputTransformation(),
            lineLimits = TextFieldLineLimits.SingleLine,
            placeholder = stringResource(R.string.password_placeholder),
            trailingIcon = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        modifier = Modifier.size(32.dp),
                        onClick = {
                            isPasswordVisible = !isPasswordVisible
                        },
                        shape = CircleShape,
                    ) {
                        Icon(
                            modifier = Modifier.size(24.dp),
                            imageVector = if (isPasswordVisible){
                                ImageVector.vectorResource(R.drawable.eye)
                            }else{
                                ImageVector.vectorResource(R.drawable.eye_off)
                            },
                            contentDescription = null,
                            tint = OnBackground
                        )
                    }
                    IconButton(
                        modifier = Modifier.size(32.dp),
                        onClick = {
                            onPasswordCopyClick(passwordTextFieldState.text.trim().toString())
                        },
                        shape = CircleShape,
                    ) {
                        Icon(
                            modifier = Modifier.size(24.dp),
                            imageVector = ImageVector.vectorResource(R.drawable.copy),
                            contentDescription = null,
                            tint = OnBackground
                        )
                    }
                }

            }
        )
    }

}

@PreviewLightDark
@Composable
private fun AccountVaultTextFieldPreview() {
    LocalVaultTheme {
        AccountVaultTextField(
            modifier = Modifier.fillMaxWidth(),
            accountTextFieldState = TextFieldState("encore123123"),
            passwordTextFieldState = TextFieldState("12345667"),
            onAccountCopyClick = {},
            onPasswordCopyClick = {},
        )
    }
}