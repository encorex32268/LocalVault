package com.lihan.localvault.feature.vault.presentation.create.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.lihan.localvault.core.presentation.ui.theme.LocalVaultTheme
import com.lihan.localvault.core.presentation.ui.theme.VaultColors
import com.lihan.localvault.core.presentation.ui.theme.VaultCoralRed

@Composable
fun VaultColorPickerRow(
    selectedColor: Color,
    onColorSelect: (Color) -> Unit,
    modifier: Modifier = Modifier,
    colors: List<Color> = VaultColors
) {
    val itemShape = RoundedCornerShape(10.dp)
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(colors) { color ->
            val isSelected = selectedColor == color
            Box(
                modifier = Modifier
                    .clip(itemShape)
                    .background(
                        color = color,
                        shape = itemShape
                    )
                    .size(36.dp)
                    .border(
                        width = 2.5.dp,
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.onBackground
                        } else {
                            Color.Transparent
                        },
                        shape = itemShape
                    )
                    .clickable {
                        onColorSelect(color)
                    }
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun VaultColorPickerRowPreview() {
    LocalVaultTheme {
        VaultColorPickerRow(
            selectedColor = VaultCoralRed,
            onColorSelect = {}
        )
    }
}
