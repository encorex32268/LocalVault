package com.lihan.localvault.feature.vault.presentation.vault.components

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.fromColorLong
import androidx.compose.ui.graphics.toColorLong
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.lihan.localvault.R
import com.lihan.localvault.core.domain.FieldType
import com.lihan.localvault.core.domain.VaultCategory
import com.lihan.localvault.core.presentation.ui.theme.LocalVaultTheme
import com.lihan.localvault.core.presentation.ui.theme.VaultCoralRed
import com.lihan.localvault.feature.vault.presentation.model.VaultContentUi
import com.lihan.localvault.feature.vault.presentation.model.VaultFieldUi
import com.lihan.localvault.feature.vault.presentation.model.VaultUi
import com.lihan.localvault.feature.vault.presentation.vault.VaultAction
import java.time.Instant

@Composable
fun VaultItemCard(
    vault: VaultUi,
    modifier: Modifier = Modifier,
    onAction: (VaultAction) -> Unit,
) {
    val accentColor = Color.fromColorLong(vault.color)
    val cardBackgroundColor = accentColor
        .copy(alpha = if (vault.isExpand) 0.14f else 0.08f)
        .compositeOver(MaterialTheme.colorScheme.surfaceContainer)

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        onClick = {
            onAction(VaultAction.OnToggleExpand(vault.id))
        },
        color = cardBackgroundColor,
        contentColor = MaterialTheme.colorScheme.onBackground,
        border = BorderStroke(
            width = 1.dp,
            brush = Brush.linearGradient(
                colors = listOf(
                    accentColor.copy(alpha = if (vault.isExpand) 0.65f else 0.45f),
                    accentColor.copy(alpha = 0.12f)
                )
            )
        )
    ) {
        Column(
            modifier = Modifier
                .then(
                    if (vault.isExpand) {
                        Modifier.fillMaxWidth()
                    } else {
                        Modifier
                    }
                )
                .padding(horizontal = 14.dp, vertical = 12.dp)
                .animateContentSize()
        ) {
            Row(
                modifier = if (vault.isExpand) Modifier.fillMaxWidth() else Modifier,
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(accentColor.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        modifier = Modifier.size(18.dp),
                        imageVector = vault.imageVector,
                        contentDescription = null,
                        tint = accentColor
                    )
                }
                if (vault.isExpand) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            modifier = Modifier.basicMarquee(),
                            text = vault.title,
                            color = MaterialTheme.colorScheme.onBackground,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.SemiBold
                            ),
                            maxLines = 1
                        )
                        if (vault.formattedUpdatedAt.isNotBlank()) {
                            Text(
                                text = vault.formattedUpdatedAt,
                                color = MaterialTheme.colorScheme.outline,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                } else {
                    Text(
                        text = vault.title,
                        color = MaterialTheme.colorScheme.onBackground,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        maxLines = 1
                    )
                }
                if (vault.isExpand) {
                    IconButton(
                        modifier = Modifier.size(32.dp),
                        onClick = {
                            onAction(VaultAction.OnEditClick(vault.id))
                        },
                        shape = CircleShape
                    ) {
                        Icon(
                            modifier = Modifier.size(18.dp),
                            imageVector = ImageVector.vectorResource(R.drawable.edit),
                            contentDescription = null,
                            tint = accentColor
                        )
                    }
                    IconButton(
                        modifier = Modifier.size(32.dp),
                        onClick = {
                            onAction(VaultAction.OnDeleteClick(vault.id))
                        },
                        shape = CircleShape
                    ) {
                        Icon(
                            modifier = Modifier.size(18.dp),
                            imageVector = ImageVector.vectorResource(R.drawable.trash),
                            contentDescription = null,
                            tint = accentColor
                        )
                    }
                }
            }
            if (vault.isExpand) {
                Spacer(Modifier.height(12.dp))
                if (vault.isLoadingContent) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = accentColor,
                            strokeWidth = 2.dp
                        )
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        vault.content?.fields?.forEach { field ->
                            when (field.type) {
                                FieldType.PASSWORD -> {
                                    PasswordFieldCardItem(
                                        label = field.label,
                                        state = field.state,
                                        onCopyClick = { onAction(VaultAction.OnCopyClick(it)) }
                                    )
                                }
                                FieldType.MULTI_LINE -> {
                                    MultiLineFieldCardItem(
                                        label = field.label,
                                        state = field.state,
                                        onCopyClick = { onAction(VaultAction.OnCopyClick(it)) }
                                    )
                                }
                                FieldType.SINGLE_LINE -> {
                                    SingleLineFieldCardItem(
                                        label = field.label,
                                        state = field.state,
                                        onCopyClick = { onAction(VaultAction.OnCopyClick(it)) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Preview(backgroundColor = 0xFF0D1117, showBackground = true)
@Composable
private fun VaultItemCardPreview() {
    LocalVaultTheme {
        VaultItemCard(
            vault = VaultUi(
                iconIndex = 1,
                color = VaultCoralRed.toColorLong(),
                title = "Account",
                updatedAt = Instant.now().toEpochMilli(),
                formattedUpdatedAt = "2026年10月23日 下午4:30",
                category = VaultCategory.CUSTOM,
                isExpand = true,
                content = VaultContentUi(
                    fields = listOf(
                        VaultFieldUi(type = FieldType.SINGLE_LINE, label = "Account", state = TextFieldState("encore")),
                        VaultFieldUi(type = FieldType.PASSWORD, label = "Password", state = TextFieldState("password123"))
                    )
                )
            ),
            onAction = {}
        )
    }
}
