@file:OptIn(ExperimentalMaterial3Api::class)

package com.lihan.localvault.feature.vault.presentation.create.components

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lihan.localvault.R
import com.lihan.localvault.core.presentation.ui.theme.LocalVaultTheme
import com.lihan.localvault.core.presentation.ui.theme.VaultCoralRed

@Composable
fun CreateVaultTopAppBar(
    isEditing: Boolean,
    isLoading: Boolean,
    color: Color,
    onBackClick: () -> Unit,
    onSaveClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    TopAppBar(
        modifier = modifier,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent
        ),
        title = {
            Text(
                text = if (!isEditing) {
                    stringResource(R.string.create)
                } else {
                    stringResource(R.string.edit)
                },
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 28.sp
                )
            )
        },
        navigationIcon = {
            IconButton(
                shape = RoundedCornerShape(16.dp),
                onClick = onBackClick
            ) {
                Icon(
                    imageVector = ImageVector.vectorResource(R.drawable.back),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
        },
        actions = {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .padding(end = 12.dp)
                        .size(24.dp),
                    color = color,
                    strokeWidth = 2.dp
                )
            } else {
                IconButton(
                    shape = RoundedCornerShape(16.dp),
                    onClick = onSaveClick
                ) {
                    Icon(
                        imageVector = ImageVector.vectorResource(R.drawable.save),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
            }
        }
    )
}

@Preview(showBackground = true)
@Composable
private fun CreateVaultTopAppBarPreview() {
    LocalVaultTheme {
        CreateVaultTopAppBar(
            isEditing = false,
            isLoading = false,
            color = VaultCoralRed,
            onBackClick = {},
            onSaveClick = {}
        )
    }
}
