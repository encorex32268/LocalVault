package com.lihan.localvault.feature.vault.presentation.model

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import com.lihan.localvault.core.domain.VaultCategory
import com.lihan.localvault.core.presentation.ui.theme.IconsEnum

data class VaultUi(
    val id: Long = 0L,
    val iconIndex: Int,
    val color: Long,
    val title: String,
    val updatedAt: Long,
    val formattedUpdatedAt: String = "",
    val category: VaultCategory,
    val isExpand: Boolean = false,
    val isLoadingContent: Boolean = false,
    val content: VaultContentUi? = null
){
    val imageVector: ImageVector
        @Composable
        get(){
            val icons = IconsEnum.entries
            val resId =  icons.getOrNull(iconIndex)?.resId?:icons.first().resId
            return ImageVector.vectorResource(resId)
        }
}
