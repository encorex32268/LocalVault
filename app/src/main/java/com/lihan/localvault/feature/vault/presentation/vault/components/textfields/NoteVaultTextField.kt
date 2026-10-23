package com.lihan.localvault.feature.vault.presentation.vault.components.textfields

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import com.lihan.localvault.feature.vault.presentation.vault.components.VaultTextField

@Composable
fun NoteVaultTextField(
    noteTextFieldState: TextFieldState,
    onNoteCopyClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    maxHeightInLines: Int = 5
) {

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = stringResource(R.string.note),
            fontWeight = FontWeight.SemiBold
        )
        VaultTextField(
            textFieldState = noteTextFieldState,
            lineLimits = TextFieldLineLimits.MultiLine(maxHeightInLines = maxHeightInLines),
            placeholder = stringResource(R.string.note_placeholder),
            trailingIcon = {
                IconButton(
                    modifier = Modifier.size(32.dp),
                    onClick = {
                        onNoteCopyClick(noteTextFieldState.text.trim().toString())
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
private fun NoteVaultTextFieldPreview() {
    LocalVaultTheme {
        NoteVaultTextField(
            noteTextFieldState = TextFieldState(
                initialText = "1 \n 2 \n 3\n4 \n 5 \n 6 \n 7",
            ),
            onNoteCopyClick = {}
        )
    }
}