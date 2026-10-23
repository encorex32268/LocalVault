package com.lihan.localvault.feature.vault.presentation.create.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.lihan.localvault.R
import com.lihan.localvault.core.domain.FieldType
import com.lihan.localvault.core.presentation.ui.theme.LocalVaultTheme
import com.lihan.localvault.core.presentation.ui.theme.OnBackground
import com.lihan.localvault.core.presentation.ui.theme.Placeholder
import com.lihan.localvault.feature.vault.presentation.model.VaultFieldUi
import com.lihan.localvault.feature.vault.presentation.vault.components.PasswordOutputTransformation
import com.lihan.localvault.feature.vault.presentation.vault.components.VaultTextField

@Composable
fun CreateVaultFieldItem(
    field: VaultFieldUi,
    onTogglePasswordVisibility: () -> Unit,
    onPasteField: () -> Unit,
    onRemoveField: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(modifier = Modifier.weight(1f)) {
            when (field.type) {
                FieldType.PASSWORD -> {
                    VaultTextField(
                        textFieldState = field.state,
                        placeholder = stringResource(R.string.password_placeholder),
                        outputTransformation = if (field.isPasswordVisible) null else PasswordOutputTransformation(),
                        lineLimits = TextFieldLineLimits.SingleLine,
                        trailingIcon = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                IconButton(
                                    modifier = Modifier.size(32.dp),
                                    onClick = onTogglePasswordVisibility,
                                    shape = CircleShape
                                ) {
                                    Icon(
                                        modifier = Modifier.size(22.dp),
                                        imageVector = if (field.isPasswordVisible) {
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
                                    onClick = onPasteField,
                                    shape = CircleShape
                                ) {
                                    Icon(
                                        modifier = Modifier.size(22.dp),
                                        imageVector = ImageVector.vectorResource(R.drawable.paste),
                                        contentDescription = null,
                                        tint = OnBackground
                                    )
                                }
                            }
                        }
                    )
                }
                FieldType.MULTI_LINE -> {
                    VaultTextField(
                        textFieldState = field.state,
                        placeholder = stringResource(R.string.note_placeholder),
                        lineLimits = TextFieldLineLimits.Default,
                        trailingIcon = {
                            IconButton(
                                modifier = Modifier.size(32.dp),
                                onClick = onPasteField,
                                shape = CircleShape
                            ) {
                                Icon(
                                    modifier = Modifier.size(22.dp),
                                    imageVector = ImageVector.vectorResource(R.drawable.paste),
                                    contentDescription = null,
                                    tint = OnBackground
                                )
                            }
                        }
                    )
                }
                FieldType.SINGLE_LINE -> {
                    VaultTextField(
                        textFieldState = field.state,
                        placeholder = stringResource(R.string.single_line_input),
                        lineLimits = TextFieldLineLimits.SingleLine,
                        trailingIcon = {
                            IconButton(
                                modifier = Modifier.size(32.dp),
                                onClick = onPasteField,
                                shape = CircleShape
                            ) {
                                Icon(
                                    modifier = Modifier.size(22.dp),
                                    imageVector = ImageVector.vectorResource(R.drawable.paste),
                                    contentDescription = null,
                                    tint = OnBackground
                                )
                            }
                        }
                    )
                }
            }
        }

        IconButton(
            modifier = Modifier.size(36.dp),
            onClick = onRemoveField,
            shape = CircleShape
        ) {
            Icon(
                modifier = Modifier.size(20.dp),
                imageVector = ImageVector.vectorResource(R.drawable.trash),
                contentDescription = stringResource(R.string.delete),
                tint = Placeholder
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun CreateVaultFieldItemPreview() {
    LocalVaultTheme {
        CreateVaultFieldItem(
            field = VaultFieldUi(
                id = "1",
                label = "",
                state = rememberTextFieldState("secret123"),
                type = FieldType.PASSWORD,
                isPasswordVisible = false
            ),
            onTogglePasswordVisibility = {},
            onPasteField = {},
            onRemoveField = {}
        )
    }
}
