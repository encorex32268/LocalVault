package com.lihan.localvault.feature.vault.presentation.vault.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lihan.localvault.R
import com.lihan.localvault.core.presentation.ui.theme.LocalVaultTheme
import com.lihan.localvault.core.presentation.ui.theme.Primary
import com.lihan.localvault.core.presentation.ui.theme.VaultCoralRed

@Composable
fun VaultLimitCard(
    currentCount: Int,
    maxCount: Int,
    isCollapsed: Boolean,
    onToggleCollapse: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isLimitReached = currentCount >= maxCount
    val progress = (currentCount.toFloat() / maxCount.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)

    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.CenterStart
    ) {
        Surface(
            modifier = Modifier.animateContentSize(),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceContainer,
            border = BorderStroke(
                width = 1.dp,
                color = if (isLimitReached) VaultCoralRed.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surfaceContainer
            ),
            onClick = onToggleCollapse
        ) {
            AnimatedContent(
                targetState = isCollapsed,
                transitionSpec = {
                    fadeIn(animationSpec = tween(200)) togetherWith fadeOut(animationSpec = tween(150))
                },
                label = "VaultLimitCardAnimation"
            ) { collapsed ->
                if (collapsed) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            modifier = Modifier.size(18.dp),
                            imageVector = ImageVector.vectorResource(R.drawable.lock_keyhole),
                            contentDescription = stringResource(R.string.free_plan),
                            tint = if (isLimitReached) VaultCoralRed else Primary
                        )
                        Text(
                            text = stringResource(R.string.vault_limit_count, currentCount, maxCount),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (isLimitReached) VaultCoralRed else Primary,
                                fontSize = 14.sp
                            )
                        )
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    modifier = Modifier.size(18.dp),
                                    imageVector = ImageVector.vectorResource(R.drawable.lock_keyhole),
                                    contentDescription = null,
                                    tint = if (isLimitReached) VaultCoralRed else Primary
                                )
                                Text(
                                    text = stringResource(R.string.free_plan),
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onBackground,
                                        fontSize = 14.sp
                                    )
                                )
                            }

                            Text(
                                text = stringResource(R.string.vault_limit_count, currentCount, maxCount),
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (isLimitReached) VaultCoralRed else Primary,
                                    fontSize = 14.sp
                                )
                            )
                        }

                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp),
                            color = if (isLimitReached) VaultCoralRed else Primary,
                            trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                            strokeCap = StrokeCap.Round
                        )
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun VaultLimitCardNormalPreview() {
    LocalVaultTheme(darkTheme = true) {
        VaultLimitCard(
            currentCount = 5,
            maxCount = 15,
            isCollapsed = false,
            onToggleCollapse = {}
        )
    }
}

@Preview
@Composable
private fun VaultLimitCardCollapsedPreview() {
    LocalVaultTheme(darkTheme = true) {
        VaultLimitCard(
            currentCount = 5,
            maxCount = 15,
            isCollapsed = true,
            onToggleCollapse = {}
        )
    }
}

@Preview
@Composable
private fun VaultLimitCardFullPreview() {
    LocalVaultTheme(darkTheme = true) {
        VaultLimitCard(
            currentCount = 15,
            maxCount = 15,
            isCollapsed = false,
            onToggleCollapse = {}
        )
    }
}
