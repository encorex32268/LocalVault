package com.lihan.localvault.feature.vault.presentation.vault.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import com.lihan.localvault.R
import com.lihan.localvault.core.presentation.ui.theme.OnBackground

@Composable
fun PasswordFieldCardItem(
    label: String,
    state: TextFieldState,
    onCopyClick: (String) -> Unit
) {
    var isPasswordVisible by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        if (label.isNotBlank()) {
            Text(
                text = label,
                fontWeight = FontWeight.SemiBold,
                color = OnBackground,
                style = MaterialTheme.typography.bodyMedium
            )
        }
        VaultTextField(
            enabled = false,
            textFieldState = state,
            placeholder = stringResource(R.string.password_placeholder),
            outputTransformation = if (isPasswordVisible) null else PasswordOutputTransformation(),
            lineLimits = TextFieldLineLimits.SingleLine,
            trailingIcon = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(
                        modifier = Modifier.size(32.dp),
                        onClick = { isPasswordVisible = !isPasswordVisible },
                        shape = CircleShape
                    ) {
                        Icon(
                            modifier = Modifier.size(22.dp),
                            imageVector = if (isPasswordVisible) {
                                ImageVector.vectorResource(R.drawable.eye)
                            } else {
                                ImageVector.vectorResource(R.drawable.eye_off)
                            },
                            contentDescription = null,
                            tint = OnBackground
                        )
                    }
                    IconButton(
                        modifier = Modifier.size(32.dp),
                        onClick = { onCopyClick(state.text.trim().toString()) },
                        shape = CircleShape
                    ) {
                        Icon(
                            modifier = Modifier.size(22.dp),
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