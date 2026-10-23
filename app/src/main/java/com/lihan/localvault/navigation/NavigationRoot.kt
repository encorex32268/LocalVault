package com.lihan.localvault.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.lihan.localvault.feature.vault.presentation.create.CreateVaultRoot
import com.lihan.localvault.feature.vault.presentation.vault.VaultRoot

@Composable
fun NavigationRoot(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = Route.VaultList,
        modifier = modifier
    ) {
        composable<Route.VaultList> {
            VaultRoot(
                onNavigateToCreate = {
                    navController.navigate(Route.VaultEdit(vaultId = null))
                },
                onNavigateToEdit = { vaultId ->
                    navController.navigate(Route.VaultEdit(vaultId = vaultId))
                }
            )
        }

        composable<Route.VaultEdit> { backStackEntry ->
            val route = backStackEntry.toRoute<Route.VaultEdit>()
            CreateVaultRoot(
                vaultId = route.vaultId,
                onBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
