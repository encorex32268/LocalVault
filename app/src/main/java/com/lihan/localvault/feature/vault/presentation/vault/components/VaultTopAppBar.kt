@file:OptIn(ExperimentalMaterial3Api::class)

package com.lihan.localvault.feature.vault.presentation.vault.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lihan.localvault.R
import com.lihan.localvault.core.presentation.ui.theme.LocalVaultIcons
import com.lihan.localvault.core.presentation.ui.theme.LocalVaultTheme
import com.lihan.localvault.core.presentation.ui.theme.Primary

@Composable
fun VaultTopAppBar(
    isSearchActive: Boolean,
    searchQuery: String,
    isDarkTheme: Boolean,
    onSearchQueryChange: (String) -> Unit,
    onToggleSearch: () -> Unit,
    onClearSearch: () -> Unit,
    onToggleTheme: () -> Unit,
    onExportClick: () -> Unit = {},
    onImportClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    AnimatedContent(
        targetState = isSearchActive,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "VaultTopAppBarSearchTransition"
    ) { searchActive ->
        if (searchActive) {
            val focusRequester = remember { FocusRequester() }
            val focusManager = LocalFocusManager.current

            LaunchedEffect(Unit) {
                focusRequester.requestFocus()
            }

            TopAppBar(
                modifier = modifier,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                ),
                navigationIcon = {
                    IconButton(
                        shape = RoundedCornerShape(16.dp),
                        onClick = onToggleSearch
                    ) {
                        Icon(
                            imageVector = LocalVaultIcons.Back,
                            contentDescription = stringResource(R.string.close_search),
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                title = {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .background(
                                color = MaterialTheme.colorScheme.surfaceContainer,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .padding(horizontal = 12.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        if (searchQuery.isEmpty()) {
                            Text(
                                text = stringResource(R.string.search_placeholder),
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = MaterialTheme.colorScheme.outline,
                                    fontSize = 16.sp
                                )
                            )
                        }

                        BasicTextField(
                            value = searchQuery,
                            onValueChange = onSearchQueryChange,
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(focusRequester),
                            textStyle = MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.onBackground,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Normal
                            ),
                            cursorBrush = SolidColor(Primary),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(
                                onSearch = {
                                    focusManager.clearFocus()
                                }
                            )
                        )
                    }
                },
                actions = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(
                            shape = RoundedCornerShape(16.dp),
                            onClick = onClearSearch
                        ) {
                            Icon(
                                modifier = Modifier.size(20.dp),
                                imageVector = LocalVaultIcons.Close,
                                contentDescription = stringResource(R.string.clear_search),
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }
                }
            )
        } else {
            TopAppBar(
                modifier = modifier,
                title = {
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground,
                            fontSize = 28.sp
                        )
                    )
                },
                actions = {
                    IconButton(
                        shape = RoundedCornerShape(16.dp),
                        onClick = onToggleSearch
                    ) {
                        Icon(
                            modifier = Modifier.size(24.dp),
                            imageVector = LocalVaultIcons.Search,
                            contentDescription = stringResource(R.string.search),
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    IconButton(
                        shape = RoundedCornerShape(16.dp),
                        onClick = onToggleTheme
                    ) {
                        Icon(
                            modifier = Modifier.size(24.dp),
                            imageVector = if (isDarkTheme) LocalVaultIcons.Sun else LocalVaultIcons.Moon,
                            contentDescription = stringResource(
                                if (isDarkTheme) R.string.switch_to_light_mode else R.string.switch_to_dark_mode
                            ),
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }

                    var isMenuExpanded by remember { mutableStateOf(false) }
                    Box {
                        IconButton(
                            shape = RoundedCornerShape(16.dp),
                            onClick = { isMenuExpanded = true }
                        ) {
                            Icon(
                                modifier = Modifier.size(24.dp),
                                imageVector = LocalVaultIcons.MoreVert,
                                contentDescription = stringResource(R.string.more_options),
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                        }

                        DropdownMenu(
                            expanded = isMenuExpanded,
                            onDismissRequest = { isMenuExpanded = false },
                            shape = RoundedCornerShape(16.dp),
                            containerColor = MaterialTheme.colorScheme.surface
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = stringResource(R.string.export_backup),
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    )
                                },
                                onClick = {
                                    isMenuExpanded = false
                                    onExportClick()
                                }
                            )
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = stringResource(R.string.import_backup),
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    )
                                },
                                onClick = {
                                    isMenuExpanded = false
                                    onImportClick()
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun VaultTopAppBarNormalPreview() {
    LocalVaultTheme {
        VaultTopAppBar(
            isSearchActive = false,
            searchQuery = "",
            isDarkTheme = true,
            onSearchQueryChange = {},
            onToggleSearch = {},
            onClearSearch = {},
            onToggleTheme = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun VaultTopAppBarSearchPreview() {
    LocalVaultTheme {
        VaultTopAppBar(
            isSearchActive = true,
            searchQuery = "GitHub",
            isDarkTheme = true,
            onSearchQueryChange = {},
            onToggleSearch = {},
            onClearSearch = {},
            onToggleTheme = {}
        )
    }
}
