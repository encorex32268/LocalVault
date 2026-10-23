@file:OptIn(ExperimentalMaterial3Api::class)

package com.lihan.localvault.feature.vault.presentation.create

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lihan.localvault.R
import com.lihan.localvault.core.domain.FieldType
import com.lihan.localvault.core.presentation.ui.theme.IconsEnum
import com.lihan.localvault.core.presentation.ui.theme.LocalVaultTheme
import com.lihan.localvault.core.presentation.ui.theme.OnBackground
import com.lihan.localvault.core.presentation.ui.theme.Placeholder
import com.lihan.localvault.core.presentation.ui.theme.Primary
import com.lihan.localvault.core.presentation.ui.theme.SurfaceContainer
import com.lihan.localvault.core.presentation.ui.theme.VaultColors
import com.lihan.localvault.core.presentation.util.ObserveAsEvents
import com.lihan.localvault.feature.vault.presentation.model.VaultFieldUi
import com.lihan.localvault.feature.vault.presentation.vault.components.PasswordOutputTransformation
import com.lihan.localvault.feature.vault.presentation.vault.components.VaultTextField
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
        .compositeOver(SurfaceContainer)

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                ),
                title = {
                    Text(
                        text = if (state.vaultId == null) {
                            stringResource(R.string.create)
                        } else {
                            stringResource(R.string.edit)
                        },
                        style = MaterialTheme.typography.titleLarge.copy(
                            color = OnBackground,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 32.sp
                        )
                    )
                },
                navigationIcon = {
                    IconButton(
                        shape = RoundedCornerShape(16.dp),
                        onClick = {
                            onAction(CreateVaultAction.OnBackClick)
                        }
                    ) {
                        Icon(
                            imageVector = ImageVector.vectorResource(R.drawable.back),
                            contentDescription = null,
                            tint = OnBackground
                        )
                    }
                },
                actions = {
                    if (state.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .padding(end = 12.dp)
                                .size(24.dp),
                            color = state.color,
                            strokeWidth = 2.dp
                        )
                    } else {
                        IconButton(
                            shape = RoundedCornerShape(16.dp),
                            onClick = {
                                onAction(CreateVaultAction.OnSaveClick)
                            }
                        ) {
                            Icon(
                                imageVector = ImageVector.vectorResource(R.drawable.save),
                                contentDescription = null,
                                tint = OnBackground
                            )
                        }
                    }
                }
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
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(VaultColors) { color ->
                    val isSelected = state.color == color
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(
                                color = color,
                                shape = CircleShape
                            )
                            .size(36.dp)
                            .border(
                                width = 3.dp,
                                color = if (isSelected) {
                                    OnBackground
                                } else {
                                    Color.Transparent
                                },
                                shape = CircleShape
                            )
                            .clickable {
                                onAction(CreateVaultAction.OnSelectColor(color))
                            }
                    )
                }
            }

            Text(
                text = stringResource(R.string.title),
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = OnBackground
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
                        .background(state.color.copy(alpha = 0.16f))
                        .border(
                            width = 1.dp,
                            color = state.color.copy(alpha = 0.45f),
                            shape = RoundedCornerShape(14.dp)
                        )
                        .clickable {
                            onAction(CreateVaultAction.OnOpenIconPicker)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        modifier = Modifier.size(24.dp),
                        imageVector = ImageVector.vectorResource(state.iconResId),
                        contentDescription = null,
                        tint = state.color
                    )
                }

                VaultTextField(
                    modifier = Modifier.weight(1f),
                    textFieldState = state.vaultTitle,
                    placeholder = stringResource(R.string.title_placeholder),
                    lineLimits = TextFieldLineLimits.SingleLine,
                    trailingIcon = {
                        IconButton(
                            modifier = Modifier.size(32.dp),
                            onClick = {
                                onAction(CreateVaultAction.OnPasteTitle)
                            }
                        ) {
                            Icon(
                                modifier = Modifier.size(24.dp),
                                imageVector = ImageVector.vectorResource(R.drawable.paste),
                                contentDescription = null,
                                tint = OnBackground
                            )
                        }
                    }
                )
            }

            state.fields.forEach { field ->
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        BasicTextField(
                            value = field.label,
                            onValueChange = { newLabel ->
                                onAction(CreateVaultAction.OnFieldLabelChange(field.id, newLabel))
                            },
                            textStyle = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = OnBackground
                            ),
                            cursorBrush = SolidColor(Primary),
                            singleLine = true,
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                        Icon(
                            modifier = Modifier.size(13.dp),
                            imageVector = ImageVector.vectorResource(R.drawable.edit),
                            contentDescription = null,
                            tint = Placeholder.copy(alpha = 0.5f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
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
                                                    onClick = {
                                                        onAction(CreateVaultAction.OnTogglePasswordVisibility(field.id))
                                                    },
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
                                                    onClick = {
                                                        onAction(CreateVaultAction.OnPasteField(field.id))
                                                    },
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
                                FieldType.NOTE -> {
                                    VaultTextField(
                                        textFieldState = field.state,
                                        placeholder = stringResource(R.string.note_placeholder),
                                        lineLimits = TextFieldLineLimits.Default,
                                        trailingIcon = {
                                            IconButton(
                                                modifier = Modifier.size(32.dp),
                                                onClick = {
                                                    onAction(CreateVaultAction.OnPasteField(field.id))
                                                },
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
                                FieldType.TEXT -> {
                                    VaultTextField(
                                        textFieldState = field.state,
                                        placeholder = field.label.lowercase(),
                                        lineLimits = TextFieldLineLimits.SingleLine,
                                        trailingIcon = {
                                            IconButton(
                                                modifier = Modifier.size(32.dp),
                                                onClick = {
                                                    onAction(CreateVaultAction.OnPasteField(field.id))
                                                },
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
                            onClick = {
                                onAction(CreateVaultAction.OnRemoveField(field.id))
                            },
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
            }

            Spacer(Modifier.height(4.dp))

            OutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    onAction(CreateVaultAction.OnOpenAddFieldMenu)
                },
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = state.color
                ),
                border = BorderStroke(1.dp, state.color.copy(alpha = 0.5f))
            ) {
                Icon(
                    imageVector = ImageVector.vectorResource(R.drawable.add),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.add_field),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold
                    )
                )
            }
        }
    }

    if (state.isAddFieldMenuOpen) {
        ModalBottomSheet(
            onDismissRequest = {
                onAction(CreateVaultAction.OnDismissAddFieldMenu)
            },
            containerColor = SurfaceContainer,
            contentColor = OnBackground,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            dragHandle = {
                BottomSheetDefaults.DragHandle(
                    color = Placeholder.copy(alpha = 0.6f)
                )
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 36.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.add_field),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = OnBackground
                    )
                )

                AddFieldOptionRow(
                    iconRes = R.drawable.user,
                    title = stringResource(R.string.single_line_input),
                    description = stringResource(R.string.add_field_text_desc),
                    tintColor = state.color,
                    onClick = {
                        onAction(CreateVaultAction.OnAddField(FieldType.TEXT))
                    }
                )

                AddFieldOptionRow(
                    iconRes = R.drawable.lock_keyhole,
                    title = stringResource(R.string.password_input),
                    description = stringResource(R.string.add_field_password_desc),
                    tintColor = state.color,
                    onClick = {
                        onAction(CreateVaultAction.OnAddField(FieldType.PASSWORD))
                    }
                )

                AddFieldOptionRow(
                    iconRes = R.drawable.edit,
                    title = stringResource(R.string.multiline_input),
                    description = stringResource(R.string.add_field_note_desc),
                    tintColor = state.color,
                    onClick = {
                        onAction(CreateVaultAction.OnAddField(FieldType.NOTE))
                    }
                )
            }
        }
    }

    if (state.isIconPickerOpen) {
        ModalBottomSheet(
            onDismissRequest = {
                onAction(CreateVaultAction.OnDismissIconPicker)
            },
            containerColor = SurfaceContainer,
            contentColor = OnBackground,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            dragHandle = {
                BottomSheetDefaults.DragHandle(
                    color = Placeholder.copy(alpha = 0.6f)
                )
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = stringResource(R.string.select_icon),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = OnBackground
                    )
                )

                LazyVerticalGrid(
                    columns = GridCells.Fixed(5),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 360.dp),
                ) {
                    itemsIndexed(IconsEnum.entries) { index, iconEnum ->
                        val isSelected = state.iconIndex == index
                        Box(
                            modifier = Modifier
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    if (isSelected) {
                                        state.color.copy(alpha = 0.25f)
                                    } else {
                                        state.color.copy(alpha = 0.08f)
                                    }
                                )
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) state.color else Placeholder.copy(alpha = 0.3f),
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .clickable {
                                    onAction(CreateVaultAction.OnSelectIcon(index))
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                modifier = Modifier.size(28.dp),
                                imageVector = ImageVector.vectorResource(iconEnum.resId),
                                contentDescription = null,
                                tint = if (isSelected) state.color else OnBackground
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AddFieldOptionRow(
    iconRes: Int,
    title: String,
    description: String,
    tintColor: Color,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = tintColor.copy(alpha = 0.08f).compositeOver(SurfaceContainer),
        border = BorderStroke(1.dp, tintColor.copy(alpha = 0.25f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(tintColor.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    modifier = Modifier.size(22.dp),
                    imageVector = ImageVector.vectorResource(iconRes),
                    contentDescription = null,
                    tint = tintColor
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = OnBackground
                    )
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Placeholder
                    )
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun CreateVaultScreenPreview() {
    LocalVaultTheme {
        CreateVaultScreen(
            state = CreateVaultState(
                fields = listOf(
                    VaultFieldUi(type = FieldType.TEXT, label = "Account"),
                    VaultFieldUi(type = FieldType.PASSWORD, label = "Password")
                )
            ),
            onAction = {}
        )
    }
}
