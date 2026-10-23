@file:OptIn(ExperimentalMaterial3Api::class)

package com.lihan.localvault.feature.vault.presentation.create.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.lihan.localvault.R
import com.lihan.localvault.core.presentation.ui.theme.IconsEnum
import com.lihan.localvault.core.presentation.ui.theme.LocalVaultTheme
import com.lihan.localvault.core.presentation.ui.theme.OnBackground
import com.lihan.localvault.core.presentation.ui.theme.Placeholder
import com.lihan.localvault.core.presentation.ui.theme.SurfaceContainer
import com.lihan.localvault.core.presentation.ui.theme.VaultCoralRed

@Composable
fun IconPickerBottomSheet(
    selectedIconIndex: Int,
    color: Color,
    onSelectIcon: (Int) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = modifier,
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
                    val isSelected = selectedIconIndex == index
                    Box(
                        modifier = Modifier
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                if (isSelected) {
                                    color.copy(alpha = 0.25f)
                                } else {
                                    color.copy(alpha = 0.08f)
                                }
                            )
                            .border(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) color else Placeholder.copy(alpha = 0.3f),
                                shape = RoundedCornerShape(16.dp)
                            )
                            .clickable {
                                onSelectIcon(index)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            modifier = Modifier.size(28.dp),
                            imageVector = ImageVector.vectorResource(iconEnum.resId),
                            contentDescription = null,
                            tint = if (isSelected) color else OnBackground
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun IconPickerBottomSheetPreview() {
    LocalVaultTheme {
        IconPickerBottomSheet(
            selectedIconIndex = 0,
            color = VaultCoralRed,
            onSelectIcon = {},
            onDismiss = {}
        )
    }
}
