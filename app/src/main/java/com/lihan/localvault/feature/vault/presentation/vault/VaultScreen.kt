package com.lihan.localvault.feature.vault.presentation.vault

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toColorLong
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lihan.localvault.R
import com.lihan.localvault.core.domain.VaultCategory
import com.lihan.localvault.core.presentation.ui.theme.Background
import com.lihan.localvault.core.presentation.ui.theme.IconsEnum
import com.lihan.localvault.core.presentation.ui.theme.LocalVaultTheme
import com.lihan.localvault.core.presentation.ui.theme.OnBackground
import com.lihan.localvault.core.presentation.ui.theme.Placeholder
import com.lihan.localvault.core.presentation.ui.theme.Primary
import com.lihan.localvault.core.presentation.ui.theme.SurfaceContainer
import com.lihan.localvault.core.presentation.ui.theme.VaultColors
import com.lihan.localvault.core.presentation.ui.theme.VaultCoralRed
import com.lihan.localvault.feature.vault.presentation.model.VaultContentUi
import com.lihan.localvault.feature.vault.presentation.model.VaultUi
import com.lihan.localvault.feature.vault.presentation.vault.components.VaultItemCard
import org.koin.androidx.compose.koinViewModel
import java.time.Instant

@Composable
fun VaultRoot(
    viewModel: VaultViewModel = koinViewModel(),
    onNavigateToCreate: () -> Unit = {},
    onNavigateToEdit: (Long) -> Unit = {}
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    @Suppress("DEPRECATION")
    val clipboardManager = LocalClipboardManager.current

    VaultScreen(
        state = state,
        onAction = { action ->
            when (action) {
                is VaultAction.OnCreateClick -> onNavigateToCreate()
                is VaultAction.OnEditClick -> onNavigateToEdit(action.id)
                is VaultAction.OnCopyClick -> {
                    clipboardManager.setText(AnnotatedString(action.text))
                    viewModel.onAction(action)
                }
                else -> viewModel.onAction(action)
            }
        }
    )
}

@Composable
private fun VaultScreen(
    state: VaultState,
    onAction: (VaultAction) -> Unit
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Background,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onAction(VaultAction.OnCreateClick) },
                containerColor = Primary,
                contentColor = Background
            ) {
                Icon(
                    imageVector = ImageVector.vectorResource(R.drawable.add),
                    contentDescription = null
                )
            }
        }
    ) { paddingValues ->

        FlowRow(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            state.items.forEach { item ->
                VaultItemCard(
                    vault = item,
                    onAction = onAction
                )
            }
        }
    }

    if (state.deleteTargetId != null) {
        AlertDialog(
            onDismissRequest = {
                onAction(VaultAction.OnDismissDeleteDialog)
            },
            title = {
                Text(
                    text = stringResource(R.string.delete_dialog_title),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = OnBackground
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.delete_dialog_message),
                    style = MaterialTheme.typography.bodyMedium,
                    color = OnBackground
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onAction(VaultAction.OnConfirmDelete)
                    }
                ) {
                    Text(
                        text = stringResource(R.string.confirm),
                        color = VaultCoralRed
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        onAction(VaultAction.OnDismissDeleteDialog)
                    }
                ) {
                    Text(
                        text = stringResource(R.string.cancel),
                        color = Placeholder
                    )
                }
            },
            containerColor = SurfaceContainer,
            shape = RoundedCornerShape(16.dp)
        )
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Preview(showBackground = true)
@Composable
private fun VaultScreenPreview() {
    LocalVaultTheme {
        VaultScreen(
            state = VaultState(
                items = (0..20).map {
                    val title = if (it % 2 == 0) {
                        "VaultVaultVaultVaultVaultVaultVault - $it"
                    } else {
                        "Vault - $it"
                    }
                    VaultUi(
                        id = it.toLong(),
                        iconIndex = IconsEnum.entries.random().ordinal,
                        color = VaultColors.random().toColorLong(),
                        title = title,
                        updatedAt = Instant.now().toEpochMilli(),
                        category = VaultCategory.entries.random(),
                        isExpand = it == 4,
                        content = if (it == 4) VaultContentUi.Note(
                            noteState = TextFieldState("Sample note content")
                        ) else null
                    )
                }
            ),
            onAction = {}
        )
    }
}
