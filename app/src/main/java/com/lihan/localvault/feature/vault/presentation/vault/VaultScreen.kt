@file:OptIn(ExperimentalMaterial3Api::class)

package com.lihan.localvault.feature.vault.presentation.vault

import android.annotation.SuppressLint
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.toColorLong
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
import com.lihan.localvault.core.presentation.ui.theme.DarkBackground
import com.lihan.localvault.core.presentation.ui.theme.IconsEnum
import com.lihan.localvault.core.presentation.ui.theme.LocalVaultTheme
import com.lihan.localvault.core.presentation.ui.theme.Primary
import com.lihan.localvault.core.presentation.ui.theme.VaultColors
import com.lihan.localvault.core.presentation.ui.theme.VaultCoralRed
import com.lihan.localvault.core.presentation.util.ObserveAsEvents
import com.lihan.localvault.feature.vault.presentation.model.VaultContentUi
import com.lihan.localvault.feature.vault.presentation.model.VaultFieldUi
import com.lihan.localvault.feature.vault.presentation.model.VaultUi
import com.lihan.localvault.feature.vault.presentation.vault.components.BackupPasswordDialog
import com.lihan.localvault.feature.vault.presentation.vault.components.BackupPasswordMode
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

    var pendingExportPayload by remember { mutableStateOf<String?>(null) }

    val exportFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri ->
        val content = pendingExportPayload
        if (uri != null && content != null) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { os ->
                    os.write(content.toByteArray(Charsets.UTF_8))
                    os.flush()
                }
                Toast.makeText(context, context.getString(R.string.export_success), Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, context.getString(R.string.error_backup_failed), Toast.LENGTH_SHORT).show()
            }
        }
        pendingExportPayload = null
    }

    val importFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            try {
                val content = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    inputStream.bufferedReader(Charsets.UTF_8).readText()
                }
                if (!content.isNullOrBlank()) {
                    viewModel.onAction(VaultAction.OnImportFileSelected(content))
                } else {
                    Toast.makeText(context, context.getString(R.string.error_corrupted_backup), Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, context.getString(R.string.error_backup_failed), Toast.LENGTH_SHORT).show()
            }
        }
    }

    LaunchedEffect(state.isLocked) {
        if (state.isLocked) {
            viewModel.onAction(VaultAction.OnRequestBiometric)
        }
    }

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
                        viewModel.onAction(VaultAction.OnAuthenticatedSession)
                    }
                }
            }
            is VaultEvent.LaunchExportFilePicker -> {
                pendingExportPayload = event.content
                exportFileLauncher.launch(event.suggestedFileName)
            }
            is VaultEvent.LaunchImportFilePicker -> {
                importFileLauncher.launch(arrayOf("*/*"))
            }
            is VaultEvent.ShowToast -> {
                val message = if (event.formatArgs.isNotEmpty()) {
                    context.getString(event.messageRes, *event.formatArgs.toTypedArray())
                } else {
                    context.getString(event.messageRes)
                }
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    DisposableEffect(currentActivity) {
        val activity = currentActivity ?: return@DisposableEffect onDispose {}
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> {
                    viewModel.onAction(VaultAction.OnAppForeground)
                }
                else -> Unit
            }
        }
        activity.lifecycle.addObserver(observer)
        onDispose {
            activity.lifecycle.removeObserver(observer)
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
    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (state.isLocked) {
                        Modifier.blur(24.dp)
                    } else Modifier
                ),
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                VaultTopAppBar(
                    isSearchActive = state.isSearchActive,
                    searchQuery = state.searchQuery,
                    isDarkTheme = state.isDarkTheme,
                    onSearchQueryChange = { onAction(VaultAction.OnSearchQueryChange(it)) },
                    onToggleSearch = { onAction(VaultAction.OnToggleSearch) },
                    onClearSearch = { onAction(VaultAction.OnClearSearch) },
                    onToggleTheme = { onAction(VaultAction.OnToggleTheme) },
                    onExportClick = { onAction(VaultAction.OnExportBackupClick) },
                    onImportClick = { onAction(VaultAction.OnImportBackupClick) }
                )
            },
            floatingActionButton = {
                if (!state.isLocked) {
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
                            tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                        )
                        Text(
                            text = stringResource(R.string.no_vaults_found),
                            style = MaterialTheme.typography.bodyLarge.copy(
                                color = MaterialTheme.colorScheme.outline,
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
                    if (!state.isSearchActive && state.isLimitEnabled) {
                        VaultLimitCard(
                            currentCount = state.totalCount,
                            maxCount = state.maxVaultCount,
                            isCollapsed = state.isLimitCardCollapsed,
                            onToggleCollapse = { onAction(VaultAction.OnToggleLimitCardCollapse) }
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

        AnimatedVisibility(
            visible = state.isLocked,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background.copy(alpha = 0.88f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { onAction(VaultAction.OnRequestBiometric) }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.padding(horizontal = 32.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .background(
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                shape = CircleShape
                            )
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = ImageVector.vectorResource(R.drawable.fingerprint),
                            contentDescription = stringResource(R.string.biometric_prompt_title),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(40.dp)
                        )
                    }

                    Text(
                        text = stringResource(R.string.vault_locked),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    )

                    Text(
                        text = stringResource(R.string.biometric_prompt_subtitle),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.outline,
                            textAlign = TextAlign.Center
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = { onAction(VaultAction.OnRequestBiometric) },
                        shape = RoundedCornerShape(24.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
                    ) {
                        Icon(
                            imageVector = ImageVector.vectorResource(R.drawable.fingerprint),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.tap_to_unlock),
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                        )
                    }
                }
            }
        }
    }

    if (!state.isLocked && state.deleteTargetId != null) {
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
                    color = MaterialTheme.colorScheme.onBackground
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.delete_dialog_message),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground
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
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            },
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            shape = RoundedCornerShape(16.dp)
        )
    }

    if (state.isExportPasswordDialogVisible) {
        BackupPasswordDialog(
            mode = BackupPasswordMode.EXPORT,
            errorMessage = state.backupErrorRes?.let { stringResource(it) },
            onConfirm = { password ->
                onAction(VaultAction.OnExportPasswordConfirm(password))
            },
            onDismiss = {
                onAction(VaultAction.OnDismissBackupDialog)
            }
        )
    }

    if (state.isImportPasswordDialogVisible) {
        BackupPasswordDialog(
            mode = BackupPasswordMode.IMPORT,
            errorMessage = state.backupErrorRes?.let { stringResource(it) },
            onConfirm = { password ->
                onAction(VaultAction.OnImportPasswordConfirm(password))
            },
            onDismiss = {
                onAction(VaultAction.OnDismissBackupDialog)
            }
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
                isAuthenticatedSession = true,
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
