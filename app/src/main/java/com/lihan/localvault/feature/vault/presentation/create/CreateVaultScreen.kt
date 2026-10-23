package com.lihan.localvault.feature.vault.presentation.create

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun CreateVaultRoot(
    viewModel: CreateVaultViewModel = viewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    CreateVaultScreen(
        state = state,
        onAction = viewModel::onAction
    )
}

@Composable
private fun CreateVaultScreen(
    state: CreateVaultState,
    onAction: (CreateVaultAction) -> Unit,
    modifier: Modifier = Modifier
) {

}

@Preview(showBackground = true)
@Composable
private fun CreateVaultScreenPreview() {
    CreateVaultScreen(
        state = CreateVaultState(),
        onAction = {}
    )
}