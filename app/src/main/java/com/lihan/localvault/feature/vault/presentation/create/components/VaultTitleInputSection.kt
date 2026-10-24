package com.lihan.localvault.feature.vault.presentation.create.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.lihan.localvault.R
import com.lihan.localvault.core.presentation.ui.theme.LocalVaultTheme
import com.lihan.localvault.core.presentation.ui.theme.VaultCoralRed
import com.lihan.localvault.feature.vault.presentation.vault.components.VaultTextField

@Composable
fun VaultTitleInputSection(
    titleState: TextFieldState,
    iconResId: Int,
    color: Color,
    onIconClick: () -> Unit,
    onPasteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = stringResource(R.string.title),
            style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(color.copy(alpha = 0.16f))
                    .border(
                        width = 1.dp,
                        color = color.copy(alpha = 0.45f),
                        shape = RoundedCornerShape(14.dp)
                    )
                    .clickable(onClick = onIconClick),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    modifier = Modifier.size(24.dp),
                    imageVector = ImageVector.vectorResource(iconResId),
                    contentDescription = null,
                    tint = color
                )
            }

            VaultTextField(
                modifier = Modifier.weight(1f),
                textFieldState = titleState,
                placeholder = stringResource(R.string.title_placeholder),
                lineLimits = TextFieldLineLimits.SingleLine,
                trailingIcon = {
                    IconButton(
                        modifier = Modifier.size(32.dp),
                        onClick = onPasteClick
                    ) {
                        Icon(
                            modifier = Modifier.size(24.dp),
                            imageVector = ImageVector.vectorResource(R.drawable.paste),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun VaultTitleInputSectionPreview() {
    LocalVaultTheme {
        VaultTitleInputSection(
            titleState = rememberTextFieldState("My Vault"),
            iconResId = R.drawable.key,
            color = VaultCoralRed,
            onIconClick = {},
            onPasteClick = {}
        )
    }
}
