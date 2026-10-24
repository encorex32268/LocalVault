@file:OptIn(ExperimentalMaterial3Api::class)

package com.lihan.localvault.feature.vault.presentation.vault

import android.annotation.SuppressLint
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.LocalActivity
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toColorLong
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lihan.localvault.R
import com.lihan.localvault.core.biometric.BiometricPromptManager
import com.lihan.localvault.core.biometric.BiometricResult
import com.lihan.localvault.core.domain.FieldType
import com.lihan.localvault.core.domain.VaultCategory
import com.lihan.localvault.core.presentation.ui.theme.Background
import com.lihan.localvault.core.presentation.ui.theme.DarkBackground
import com.lihan.localvault.core.presentation.ui.theme.IconsEnum
import com.lihan.localvault.core.presentation.ui.theme.LocalVaultTheme
import com.lihan.localvault.core.presentation.ui.theme.OnBackground
import com.lihan.localvault.core.presentation.ui.theme.Placeholder
import com.lihan.localvault.core.presentation.ui.theme.Primary
import com.lihan.localvault.core.presentation.ui.theme.SurfaceContainer
import com.lihan.localvault.core.presentation.ui.theme.VaultColors
import com.lihan.localvault.core.presentation.ui.theme.VaultCoralRed
import com.lihan.localvault.core.presentation.util.ObserveAsEvents
import com.lihan.localvault.feature.vault.presentation.model.VaultContentUi
import com.lihan.localvault.feature.vault.presentation.model.VaultFieldUi
import com.lihan.localvault.feature.vault.presentation.model.VaultUi
import com.lihan.localvault.feature.vault.presentation.vault.components.VaultItemCard
import com.lihan.localvault.feature.vault.presentation.vault.components.VaultLimitCard
import com.lihan.localvault.feature.vault.presentation.vault.components.VaultTopAppBar
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import java.time.Instant

@SuppressLint("LocalContextGetResourceValueCall")
@Composable
fun VaultRoot(
    viewModel: VaultViewModel = koinViewModel(),
    biometricPromptManager: BiometricPromptManager = koinInject(),
    onNavigateToCreate: () -> Unit = {},
    onNavigateToEdit: (Long) -> Unit = {}
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    @Suppress("DEPRECATION")
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    val currentActivity = LocalActivity.current as? FragmentActivity
    val lifecycleOwner = LocalLifecycleOwner.current

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            is VaultEvent.RequestBiometric -> {
                val activity = currentActivity ?: (context as? FragmentActivity)
                if (activity == null) {
                    Toast.makeText(
                        context,
                        context.getString(R.string.error_activity_not_found),
                        Toast.LENGTH_SHORT
                    ).show()
                    return@ObserveAsEvents
                }
                biometricPromptManager.promptBiometric(
                    activity = activity,
                    title = context.getString(R.string.biometric_prompt_title),
                    subtitle = context.getString(R.string.biometric_prompt_subtitle)
                ) { result ->
                    if (result is BiometricResult.Success) {
                        viewModel.onAction(VaultAction.OnBiometricSuccess(event.vaultId))
                    }
                }
            }
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                viewModel.onAction(VaultAction.OnCollapseAll)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    VaultScreen(
        state = state,
        onAction = { action ->
            when (action) {
                is VaultAction.OnCreateClick -> {
                    if (state.isLimitReached) {
                        Toast.makeText(
                            context,
                            context.getString(R.string.vault_limit_reached, state.totalCount, state.maxVaultCount),
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        onNavigateToCreate()
                    }
                }
                is VaultAction.OnEditClick -> onNavigateToEdit(action.id)
                is VaultAction.OnCopyClick -> {
                    clipboardManager.setText(AnnotatedString(action.text))
                    viewModel.onAction(action)
                }
                else -> viewModel.onAction(action)
            }
        }
    )
}

@Composable
private fun VaultScreen(
    state: VaultState,
    onAction: (VaultAction) -> Unit
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Background,
        topBar = {
            VaultTopAppBar(
                isSearchActive = state.isSearchActive,
                searchQuery = state.searchQuery,
                isDarkTheme = state.isDarkTheme,
                onSearchQueryChange = { onAction(VaultAction.OnSearchQueryChange(it)) },
                onToggleSearch = { onAction(VaultAction.OnToggleSearch) },
                onClearSearch = { onAction(VaultAction.OnClearSearch) },
                onToggleTheme = { onAction(VaultAction.OnToggleTheme) }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onAction(VaultAction.OnCreateClick) },
                containerColor = Primary,
                contentColor = DarkBackground
            ) {
                Icon(
                    imageVector = ImageVector.vectorResource(R.drawable.add),
                    contentDescription = null
                )
            }
        }
    ) { paddingValues ->

        if (state.isSearchActive && state.searchQuery.isNotBlank() && state.filteredItems.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        modifier = Modifier.size(48.dp),
                        imageVector = ImageVector.vectorResource(R.drawable.search),
                        contentDescription = null,
                        tint = Placeholder.copy(alpha = 0.5f)
                    )
                    Text(
                        text = stringResource(R.string.no_vaults_found),
                        style = MaterialTheme.typography.bodyLarge.copy(
                            color = Placeholder,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 12.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (!state.isSearchActive) {
                    VaultLimitCard(
                        currentCount = state.totalCount,
                        maxCount = state.maxVaultCount
                    )
                }

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    state.filteredItems.forEach { item ->
                        VaultItemCard(
                            vault = item,
                            onAction = onAction
                        )
                    }
                }
            }
        }
    }

    if (state.deleteTargetId != null) {
        AlertDialog(
            onDismissRequest = {
                onAction(VaultAction.OnDismissDeleteDialog)
            },
            title = {
                Text(
                    text = stringResource(R.string.delete_dialog_title),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = OnBackground
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.delete_dialog_message),
                    style = MaterialTheme.typography.bodyMedium,
                    color = OnBackground
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onAction(VaultAction.OnConfirmDelete)
                    }
                ) {
                    Text(
                        text = stringResource(R.string.confirm),
                        color = VaultCoralRed
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        onAction(VaultAction.OnDismissDeleteDialog)
                    }
                ) {
                    Text(
                        text = stringResource(R.string.cancel),
                        color = Placeholder
                    )
                }
            },
            containerColor = SurfaceContainer,
            shape = RoundedCornerShape(16.dp)
        )
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Preview(showBackground = true)
@Composable
private fun VaultScreenPreview() {
    LocalVaultTheme {
        VaultScreen(
            state = VaultState(
                isDarkTheme = true,
                items = (0..20).map {
                    val title = if (it % 2 == 0) {
                        "VaultVaultVaultVaultVaultVaultVault - $it"
                    } else {
                        "Vault - $it"
                    }
                    VaultUi(
                        id = it.toLong(),
                        iconIndex = IconsEnum.entries.random().ordinal,
                        color = VaultColors.random().toColorLong(),
                        title = title,
                        updatedAt = Instant.now().toEpochMilli(),
                        category = VaultCategory.entries.random(),
                        isExpand = it == 4,
                        content = if (it == 4) VaultContentUi(
                            fields = listOf(
                                VaultFieldUi(
                                    type = FieldType.MULTI_LINE,
                                    label = "Note",
                                    state = TextFieldState("Sample note content")
                                )
                            )
                        ) else null
                    )
                }
            ),
            onAction = {}
        )
    }
}
