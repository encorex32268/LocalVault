package com.lihan.localvault.feature.vault.presentation.vault.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lihan.localvault.R
import com.lihan.localvault.core.presentation.ui.theme.Primary

enum class BackupPasswordMode {
    EXPORT,
    IMPORT
}

@Composable
fun BackupPasswordDialog(
    mode: BackupPasswordMode,
    errorMessage: String? = null,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var isConfirmPasswordVisible by remember { mutableStateOf(false) }

    val focusManager = LocalFocusManager.current

    val isLengthValid = password.length in 4..6
    val isConfirmValid = if (mode == BackupPasswordMode.EXPORT) {
        confirmPassword == password
    } else true

    val canSubmit = isLengthValid && isConfirmValid

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        icon = {
            Icon(
                modifier = Modifier.size(32.dp),
                imageVector = ImageVector.vectorResource(R.drawable.lock_keyhole),
                contentDescription = null,
                tint = Primary
            )
        },
        title = {
            Text(
                text = if (mode == BackupPasswordMode.EXPORT) {
                    stringResource(R.string.export_backup_title)
                } else {
                    stringResource(R.string.import_backup_title)
                },
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = if (mode == BackupPasswordMode.EXPORT) {
                        stringResource(R.string.export_backup_desc)
                    } else {
                        stringResource(R.string.import_backup_desc)
                    },
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )

                // Password field
                OutlinedTextField(
                    value = password,
                    onValueChange = { input ->
                        if (input.length <= 6) {
                            password = input.filter { it.isLetterOrDigit() }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = {
                        Text(stringResource(R.string.backup_password_label))
                    },
                    placeholder = {
                        Text(stringResource(R.string.backup_password_placeholder))
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Primary,
                        focusedLabelColor = Primary,
                        cursorColor = Primary
                    ),
                    visualTransformation = if (isPasswordVisible) {
                        VisualTransformation.None
                    } else {
                        PasswordVisualTransformation()
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.NumberPassword,
                        imeAction = if (mode == BackupPasswordMode.EXPORT) ImeAction.Next else ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            focusManager.clearFocus()
                            if (canSubmit) onConfirm(password)
                        }
                    ),
                    trailingIcon = {
                        IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                            Icon(
                                modifier = Modifier.size(20.dp),
                                imageVector = ImageVector.vectorResource(
                                    if (isPasswordVisible) R.drawable.eye else R.drawable.eye_off
                                ),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                )

                // Confirm password field for export mode
                if (mode == BackupPasswordMode.EXPORT) {
                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = { input ->
                            if (input.length <= 6) {
                                confirmPassword = input.filter { it.isLetterOrDigit() }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = {
                            Text(stringResource(R.string.confirm_backup_password_label))
                        },
                        placeholder = {
                            Text(stringResource(R.string.confirm_backup_password_placeholder))
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Primary,
                            focusedLabelColor = Primary,
                            cursorColor = Primary
                        ),
                        visualTransformation = if (isConfirmPasswordVisible) {
                            VisualTransformation.None
                        } else {
                            PasswordVisualTransformation()
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.NumberPassword,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                focusManager.clearFocus()
                                if (canSubmit) onConfirm(password)
                            }
                        ),
                        trailingIcon = {
                            IconButton(onClick = { isConfirmPasswordVisible = !isConfirmPasswordVisible }) {
                                Icon(
                                    modifier = Modifier.size(20.dp),
                                    imageVector = ImageVector.vectorResource(
                                        if (isConfirmPasswordVisible) R.drawable.eye else R.drawable.eye_off
                                    ),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    )

                    // Error text if passwords don't match
                    if (confirmPassword.isNotEmpty() && !isConfirmValid) {
                        Text(
                            text = stringResource(R.string.error_passwords_do_not_match),
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.error
                            )
                        )
                    }
                }

                // General error message (e.g. wrong password during import)
                AnimatedVisibility(
                    visible = !errorMessage.isNullOrBlank(),
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    if (errorMessage != null) {
                        Text(
                            text = errorMessage,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.Medium
                            )
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(password) },
                enabled = canSubmit,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text(
                    text = if (mode == BackupPasswordMode.EXPORT) {
                        stringResource(R.string.confirm_export)
                    } else {
                        stringResource(R.string.confirm_import)
                    },
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.cancel),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    )
}
