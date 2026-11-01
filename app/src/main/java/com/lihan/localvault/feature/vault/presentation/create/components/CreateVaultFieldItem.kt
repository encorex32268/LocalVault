package com.lihan.localvault.feature.vault.presentation.create.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.maxLength
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.lihan.localvault.R
import com.lihan.localvault.core.domain.FieldType
import com.lihan.localvault.core.presentation.ui.theme.LocalVaultIcons
import com.lihan.localvault.core.presentation.ui.theme.LocalVaultTheme
import com.lihan.localvault.core.presentation.ui.theme.Primary
import com.lihan.localvault.feature.vault.presentation.model.VaultFieldUi
import com.lihan.localvault.feature.vault.presentation.vault.components.PasswordOutputTransformation
import com.lihan.localvault.feature.vault.presentation.vault.components.VaultTextField

@Composable
fun CreateVaultFieldItem(
    field: VaultFieldUi,
    onTogglePasswordVisibility: () -> Unit,
    onPasteField: () -> Unit,
    onRemoveField: () -> Unit,
    modifier: Modifier = Modifier,
    onMoveUp: (() -> Unit)? = null
) {
    val defaultPlaceholder = when (field.type) {
        FieldType.SINGLE_LINE -> stringResource(R.string.account)
        FieldType.PASSWORD -> stringResource(R.string.password)
        FieldType.MULTI_LINE -> stringResource(R.string.note)
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.CenterStart
            ) {
                BasicTextField(
                    state = field.labelState,
                    inputTransformation = InputTransformation.maxLength(30),
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground
                    ),
                    cursorBrush = SolidColor(Primary),
                    lineLimits = TextFieldLineLimits.SingleLine,
                    decorator = { innerTextField ->
                        if (field.labelState.text.isBlank()) {
                            Text(
                                text = defaultPlaceholder,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            )
                        }
                        innerTextField()
                    }
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (onMoveUp != null) {
                    IconButton(
                        modifier = Modifier.size(28.dp),
                        onClick = onMoveUp,
                        shape = CircleShape
                    ) {
                        Icon(
                            modifier = Modifier.size(16.dp),
                            imageVector = LocalVaultIcons.ChevronUp,
                            contentDescription = stringResource(R.string.move_up),
                            tint = MaterialTheme.colorScheme.outline
                        )
                    }
                }

                IconButton(
                    modifier = Modifier.size(28.dp),
                    onClick = onRemoveField,
                    shape = CircleShape
                ) {
                    Icon(
                        modifier = Modifier.size(16.dp),
                        imageVector = LocalVaultIcons.Close,
                        contentDescription = stringResource(R.string.delete),
                        tint = MaterialTheme.colorScheme.outline
                    )
                }
            }
        }

        VaultTextField(
            textFieldState = field.state,
            placeholder = when (field.type) {
                FieldType.PASSWORD -> stringResource(R.string.password_placeholder)
                FieldType.MULTI_LINE -> stringResource(R.string.note_placeholder)
                FieldType.SINGLE_LINE -> stringResource(R.string.field_value)
            },
            outputTransformation = if (field.isPasswordVisible || field.type != FieldType.PASSWORD) null else PasswordOutputTransformation(),
            lineLimits = if (field.type == FieldType.MULTI_LINE) TextFieldLineLimits.Default else TextFieldLineLimits.SingleLine,
            trailingIcon = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (field.type == FieldType.PASSWORD) {
                        IconButton(
                            modifier = Modifier.size(32.dp),
                            onClick = onTogglePasswordVisibility,
                            shape = CircleShape
                        ) {
                            Icon(
                                modifier = Modifier.size(22.dp),
                                imageVector = if (field.isPasswordVisible) {
                                    LocalVaultIcons.Eye
                                } else {
                                    LocalVaultIcons.EyeOff
                                },
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }
                    IconButton(
                        modifier = Modifier.size(32.dp),
                        onClick = onPasteField,
                        shape = CircleShape
                    ) {
                        Icon(
                            modifier = Modifier.size(22.dp),
                            imageVector = LocalVaultIcons.Paste,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
            }
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun CreateVaultFieldItemPreview() {
    LocalVaultTheme {
        CreateVaultFieldItem(
            field = VaultFieldUi(
                id = "1",
                label = "帳號",
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
