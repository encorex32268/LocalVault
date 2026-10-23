package com.lihan.localvault.feature.vault.presentation.vault.components.textfields.create

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
fun AccountVaultCreateTextField(
    accountTextFieldState: TextFieldState,
    passwordTextFieldState: TextFieldState,
    onAccountPasteClick: (VaultFieldType) -> Unit,
    onPasswordPasteClick: (VaultFieldType) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = stringResource(R.string.account),
            fontWeight = FontWeight.SemiBold,
            color = OnBackground
        )
        VaultTextField(
            textFieldState = accountTextFieldState,
            placeholder = stringResource(R.string.account_placeholder),
            trailingIcon = {
                IconButton(
                    modifier = Modifier.size(32.dp),
                    onClick = {
                        onAccountPasteClick(VaultFieldType.ACCOUNT)
                    },
                    shape = CircleShape,
                ) {
                    Icon(
                        modifier = Modifier.size(24.dp),
                        imageVector = ImageVector.vectorResource(R.drawable.paste),
                        contentDescription = null,
                        tint = OnBackground
                    )
                }
            }
        )

        Text(
            text = stringResource(R.string.password),
            fontWeight = FontWeight.SemiBold,
            color = OnBackground
        )
        VaultTextField(
            textFieldState = passwordTextFieldState,
            placeholder = stringResource(R.string.password_placeholder),
            trailingIcon = {
                IconButton(
                    modifier = Modifier.size(32.dp),
                    onClick = {
                        onPasswordPasteClick(VaultFieldType.PASSWORD)
                    },
                    shape = CircleShape,
                ) {
                    Icon(
                        modifier = Modifier.size(24.dp),
                        imageVector = ImageVector.vectorResource(R.drawable.paste),
                        contentDescription = null,
                        tint = OnBackground
                    )
                }
            }
        )
    }

}

@PreviewLightDark
@Composable
private fun AccountVaultTextFieldPreview() {
    LocalVaultTheme {
        AccountVaultCreateTextField(
            modifier = Modifier.fillMaxWidth(),
            accountTextFieldState = TextFieldState("encore123123"),
            passwordTextFieldState = TextFieldState("12345667"),
            onAccountPasteClick = {},
            onPasswordPasteClick = {},
        )
    }
}