@file:OptIn(ExperimentalMaterial3Api::class)

package com.lihan.localvault.feature.vault.presentation.create.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.lihan.localvault.R
import com.lihan.localvault.core.domain.FieldType
import com.lihan.localvault.core.presentation.ui.theme.LocalVaultIcons
import com.lihan.localvault.core.presentation.ui.theme.LocalVaultTheme
import com.lihan.localvault.feature.vault.presentation.create.CreateVaultAction

private enum class AddMenuStep {
    ROOT,
    FIELD,
    ATTACHMENT
}

@Composable
fun AddEntryBottomSheet(
    onDismiss: () -> Unit,
    onAddField: (FieldType) -> Unit,
    onAction: (CreateVaultAction) -> Unit,
    modifier: Modifier = Modifier
) {
    var menuStep by remember { mutableStateOf(AddMenuStep.ROOT) }

    BackHandler(enabled = menuStep != AddMenuStep.ROOT) {
        menuStep = AddMenuStep.ROOT
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        contentColor = MaterialTheme.colorScheme.onBackground,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = {
            BottomSheetDefaults.DragHandle(
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
            )
        }
    ) {
        AnimatedContent(
            targetState = menuStep,
            transitionSpec = {
                if (initialState == AddMenuStep.ROOT) {
                    (slideInHorizontally { it } + fadeIn()).togetherWith(
                        slideOutHorizontally { -it } + fadeOut()
                    )
                } else {
                    (slideInHorizontally { -it } + fadeIn()).togetherWith(
                        slideOutHorizontally { it } + fadeOut()
                    )
                }
            },
            label = "AddMenuStepTransition"
        ) { targetStep ->
            when (targetStep) {
                AddMenuStep.ROOT -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                            .padding(bottom = 36.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.add),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        )

                        AddMenuOptionRow(
                            imageVector = LocalVaultIcons.Edit,
                            title = stringResource(R.string.add_field),
                            description = stringResource(R.string.add_field_desc),
                            hasChevron = true,
                            onClick = { menuStep = AddMenuStep.FIELD }
                        )

                        AddMenuOptionRow(
                            imageVector = LocalVaultIcons.Image,
                            title = stringResource(R.string.add_attachment),
                            description = stringResource(R.string.add_attachment_desc),
                            hasChevron = true,
                            onClick = { menuStep = AddMenuStep.ATTACHMENT }
                        )
                    }
                }

                AddMenuStep.FIELD -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                            .padding(bottom = 36.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            IconButton(
                                onClick = { menuStep = AddMenuStep.ROOT },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = LocalVaultIcons.Back,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onBackground
                                )
                            }
                            Text(
                                text = stringResource(R.string.add_field),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                            )
                        }

                        AddMenuOptionRow(
                            imageVector = LocalVaultIcons.User,
                            title = stringResource(R.string.single_line_input),
                            description = stringResource(R.string.add_field_text_desc),
                            onClick = { onAddField(FieldType.SINGLE_LINE) }
                        )

                        AddMenuOptionRow(
                            imageVector = LocalVaultIcons.LockKeyhole,
                            title = stringResource(R.string.password_input),
                            description = stringResource(R.string.add_field_password_desc),
                            onClick = { onAddField(FieldType.PASSWORD) }
                        )

                        AddMenuOptionRow(
                            imageVector = LocalVaultIcons.Edit,
                            title = stringResource(R.string.multiline_input),
                            description = stringResource(R.string.add_field_note_desc),
                            onClick = { onAddField(FieldType.MULTI_LINE) }
                        )
                    }
                }

                AddMenuStep.ATTACHMENT -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                            .padding(bottom = 36.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            IconButton(
                                onClick = { menuStep = AddMenuStep.ROOT },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = LocalVaultIcons.Back,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onBackground
                                )
                            }
                            Text(
                                text = stringResource(R.string.add_attachment),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                            )
                        }

                        AddMenuOptionRow(
                            imageVector = LocalVaultIcons.Camera,
                            title = stringResource(R.string.take_photo),
                            description = stringResource(R.string.take_photo_desc),
                            onClick = { onAction(CreateVaultAction.OnTakePhotoClick) }
                        )

                        AddMenuOptionRow(
                            imageVector = LocalVaultIcons.Image,
                            title = stringResource(R.string.choose_image),
                            description = stringResource(R.string.choose_image_desc),
                            onClick = { onAction(CreateVaultAction.OnChoosePhotoClick) }
                        )

                        AddMenuOptionRow(
                            imageVector = LocalVaultIcons.Document,
                            title = stringResource(R.string.choose_document),
                            description = stringResource(R.string.choose_document_desc),
                            onClick = { onAction(CreateVaultAction.OnChooseDocumentClick) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AddMenuOptionRow(
    imageVector: ImageVector,
    title: String,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    hasChevron: Boolean = false
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
        modifier = modifier.fillMaxWidth()
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
                    .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = imageVector,
                    contentDescription = null,
                    modifier = Modifier.size(22.dp),
                    tint = MaterialTheme.colorScheme.onBackground
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
                        color = MaterialTheme.colorScheme.onBackground
                    )
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.outline
                    )
                )
            }

            if (hasChevron) {
                Icon(
                    imageVector = LocalVaultIcons.ChevronRight,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AddEntryBottomSheetPreview() {
    LocalVaultTheme {
        AddEntryBottomSheet(
            onDismiss = {},
            onAddField = {},
            onAction = {}
        )
    }
}
