package com.lihan.localvault.feature.vault.presentation.vault.components


import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lihan.localvault.R
import com.lihan.localvault.core.presentation.ui.theme.OnBackground


@Composable
fun SingleLineFieldCardItem(
    label: String,
    state: TextFieldState,
    onCopyClick: (String) -> Unit
) {
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
            placeholder = if (label.isNotBlank()) label.lowercase() else stringResource(R.string.single_line_input),
            lineLimits = TextFieldLineLimits.SingleLine,
            trailingIcon = {
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
        )
    }
}