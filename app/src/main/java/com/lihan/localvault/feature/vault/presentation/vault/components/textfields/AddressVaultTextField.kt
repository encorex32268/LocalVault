package com.lihan.localvault.feature.vault.presentation.vault.components.textfields

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.lihan.localvault.R
import com.lihan.localvault.core.presentation.ui.theme.LocalVaultTheme
import com.lihan.localvault.core.presentation.ui.theme.OnBackground
import com.lihan.localvault.core.presentation.ui.theme.Primary
import com.lihan.localvault.feature.vault.presentation.vault.VaultAction
import com.lihan.localvault.feature.vault.presentation.vault.components.VaultTextField

@Composable
fun AddressVaultTextField(
    zipTextFieldState: TextFieldState,
    addressTextFieldState: TextFieldState,
    onZipCopyClick: (String) -> Unit,
    onAddressCopyClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = stringResource(R.string.zip),
            fontWeight = FontWeight.SemiBold
        )
        VaultTextField(
            textFieldState = zipTextFieldState,
            placeholder = stringResource(R.string.zip_placeholder),
            trailingIcon = {
                IconButton(
                    modifier = Modifier.size(32.dp),
                    onClick = {
                        onZipCopyClick(zipTextFieldState.text.trim().toString())
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
            text = stringResource(R.string.address),
            fontWeight = FontWeight.SemiBold
        )
        VaultTextField(
            textFieldState = addressTextFieldState,
            placeholder = stringResource(R.string.address_placeholder),
            trailingIcon = {
                IconButton(
                    modifier = Modifier.size(32.dp),
                    onClick = {
                        onAddressCopyClick(addressTextFieldState.text.trim().toString())
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
    }

}

@PreviewLightDark
@Composable
private fun AddressVaultTextFieldPreview() {
    LocalVaultTheme {
        AddressVaultTextField(
            zipTextFieldState = TextFieldState(),
            addressTextFieldState = TextFieldState(),
            onAddressCopyClick = {},
            onZipCopyClick = {}
        )
    }
}