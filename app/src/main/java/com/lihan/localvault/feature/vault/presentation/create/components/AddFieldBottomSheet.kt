@file:OptIn(ExperimentalMaterial3Api::class)

package com.lihan.localvault.feature.vault.presentation.create.components

import androidx.annotation.DrawableRes
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.lihan.localvault.R
import com.lihan.localvault.core.domain.FieldType
import com.lihan.localvault.core.presentation.ui.theme.LocalVaultTheme
import com.lihan.localvault.core.presentation.ui.theme.OnBackground
import com.lihan.localvault.core.presentation.ui.theme.Placeholder
import com.lihan.localvault.core.presentation.ui.theme.SurfaceContainer

@Composable
fun AddFieldBottomSheet(
    onDismiss: () -> Unit,
    onAddField: (FieldType) -> Unit,
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
                onClick = { onAddField(FieldType.SINGLE_LINE) }
            )

            AddFieldOptionRow(
                iconRes = R.drawable.lock_keyhole,
                title = stringResource(R.string.password_input),
                description = stringResource(R.string.add_field_password_desc),
                onClick = { onAddField(FieldType.PASSWORD) }
            )

            AddFieldOptionRow(
                iconRes = R.drawable.edit,
                title = stringResource(R.string.multiline_input),
                description = stringResource(R.string.add_field_note_desc),
                onClick = { onAddField(FieldType.MULTI_LINE) }
            )
        }
    }
}

@Composable
fun AddFieldOptionRow(
    @DrawableRes iconRes: Int,
    title: String,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = SurfaceContainer,
        border = BorderStroke(1.dp, Placeholder.copy(alpha = 0.25f)),
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
                    .background(Placeholder.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    modifier = Modifier.size(22.dp),
                    imageVector = ImageVector.vectorResource(iconRes),
                    contentDescription = null,
                    tint = OnBackground
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
private fun AddFieldOptionRowPreview() {
    LocalVaultTheme {
        AddFieldOptionRow(
            iconRes = R.drawable.user,
            title = "Text",
            description = "Single line text field",
            onClick = {}
        )
    }
}
