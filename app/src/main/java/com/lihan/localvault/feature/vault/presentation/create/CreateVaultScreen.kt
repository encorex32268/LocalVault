package com.lihan.localvault.feature.vault.presentation.create

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lihan.localvault.R
import com.lihan.localvault.core.domain.FieldType
import com.lihan.localvault.core.presentation.ui.theme.LocalVaultTheme
import com.lihan.localvault.core.presentation.util.ObserveAsEvents
import com.lihan.localvault.feature.vault.presentation.create.components.AddFieldBottomSheet
import com.lihan.localvault.feature.vault.presentation.create.components.AddFieldButton
import com.lihan.localvault.feature.vault.presentation.create.components.CreateVaultFieldItem
import com.lihan.localvault.feature.vault.presentation.create.components.CreateVaultTopAppBar
import com.lihan.localvault.feature.vault.presentation.create.components.IconPickerBottomSheet
import com.lihan.localvault.feature.vault.presentation.create.components.VaultColorPickerRow
import com.lihan.localvault.feature.vault.presentation.create.components.VaultTitleInputSection
import com.lihan.localvault.feature.vault.presentation.model.VaultFieldUi
import org.koin.androidx.compose.koinViewModel

@Composable
fun CreateVaultRoot(
    vaultId: Long? = null,
    viewModel: CreateVaultViewModel = koinViewModel(),
    onBack: () -> Unit = {}
) {
    LaunchedEffect(vaultId) {
        viewModel.onAction(CreateVaultAction.OnInit(vaultId))
    }

    val state by viewModel.state.collectAsStateWithLifecycle()
    @Suppress("DEPRECATION")
    val clipboardManager = LocalClipboardManager.current

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            is CreateVaultEvent.SaveSuccess -> onBack()
        }
    }

    CreateVaultScreen(
        state = state,
        onAction = { action ->
            when (action) {
                is CreateVaultAction.OnBackClick -> onBack()
                is CreateVaultAction.OnPasteTitle -> {
                    clipboardManager.getText()?.text?.let { clipText ->
                        state.vaultTitle.edit {
                            replace(0, length, clipText)
                        }
                    }
                }
                is CreateVaultAction.OnPasteField -> {
                    clipboardManager.getText()?.text?.let { clipText ->
                        state.fields.find { it.id == action.fieldId }?.state?.edit {
                            replace(0, length, clipText)
                        }
                    }
                }
                else -> viewModel.onAction(action)
            }
        }
    )
}

@Composable
private fun CreateVaultScreen(
    state: CreateVaultState,
    onAction: (CreateVaultAction) -> Unit
) {
    val cardBackgroundColor = state.color
        .copy(alpha = 0.14f)
        .compositeOver(MaterialTheme.colorScheme.surfaceContainer)

    val focusManager = LocalFocusManager.current

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
            onClick = {
                focusManager.clearFocus(true)
            }
        ),
        topBar = {
            CreateVaultTopAppBar(
                isEditing = state.vaultId != null,
                isLoading = state.isLoading,
                color = state.color,
                onBackClick = { onAction(CreateVaultAction.OnBackClick) },
                onSaveClick = { onAction(CreateVaultAction.OnSaveClick) }
            )
        },
        containerColor = cardBackgroundColor
    ) { paddingValues ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(vertical = 8.dp, horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            VaultColorPickerRow(
                selectedColor = state.color,
                onColorSelect = { color ->
                    onAction(CreateVaultAction.OnSelectColor(color))
                }
            )

            VaultTitleInputSection(
                titleState = state.vaultTitle,
                iconResId = state.iconResId,
                color = state.color,
                onIconClick = { onAction(CreateVaultAction.OnOpenIconPicker) },
                onPasteClick = { onAction(CreateVaultAction.OnPasteTitle) }
            )

            state.fields.forEach { field ->
                CreateVaultFieldItem(
                    field = field,
                    onTogglePasswordVisibility = {
                        onAction(CreateVaultAction.OnTogglePasswordVisibility(field.id))
                    },
                    onPasteField = {
                        onAction(CreateVaultAction.OnPasteField(field.id))
                    },
                    onRemoveField = {
                        onAction(CreateVaultAction.OnRemoveField(field.id))
                    }
                )
            }

            Spacer(Modifier.height(4.dp))

            AddFieldButton(
                onClick = {
                    focusManager.clearFocus(true)

                    onAction(CreateVaultAction.OnOpenAddFieldMenu)
                }
            )
        }
    }

    val context = LocalContext.current
    if (state.isAddFieldMenuOpen) {
        AddFieldBottomSheet(
            onDismiss = { onAction(CreateVaultAction.OnDismissAddFieldMenu) },
            onAddField = { fieldType ->
                val label = when (fieldType) {
                    FieldType.SINGLE_LINE -> if (state.fields.none { it.type == FieldType.SINGLE_LINE }) {
                        context.getString(R.string.account)
                    } else {
                        context.getString(R.string.single_line_input)
                    }
                    FieldType.PASSWORD -> context.getString(R.string.password)
                    FieldType.MULTI_LINE -> context.getString(R.string.note)
                }
                onAction(CreateVaultAction.OnAddField(fieldType, label))
            }
        )
    }

    if (state.isIconPickerOpen) {
        IconPickerBottomSheet(
            selectedIconIndex = state.iconIndex,
            color = state.color,
            onSelectIcon = { index ->
                onAction(CreateVaultAction.OnSelectIcon(index))
            },
            onDismiss = { onAction(CreateVaultAction.OnDismissIconPicker) }
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun CreateVaultScreenPreview() {
    LocalVaultTheme {
        CreateVaultScreen(
            state = CreateVaultState(
                fields = listOf(
                    VaultFieldUi(type = FieldType.SINGLE_LINE, label = "帳號"),
                    VaultFieldUi(type = FieldType.PASSWORD, label = "密碼")
                )
            ),
            onAction = {}
        )
    }
}
