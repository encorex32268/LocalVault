#!/usr/bin/env python3
import sys
import os
import re
from pathlib import Path

ROUTE_TEMPLATE = """package __PACKAGE__.navigation

import kotlinx.serialization.Serializable

@Serializable
data object HomeRoute

@Serializable
data object SearchRoute

@Serializable
data object SettingsRoute

@Serializable
data class DetailRoute(
    val id: String,
    val title: String? = null
)
"""

TOP_LEVEL_TEMPLATE = """package __PACKAGE__.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import __PACKAGE__.core.presentation.util.UiText
import kotlin.reflect.KClass

enum class TopLevelDestination(
    val route: KClass<*>,
    val startRoute: Any,
    val icon: ImageVector,
    val label: UiText
) {
    HOME(
        route = HomeRoute::class,
        startRoute = HomeRoute,
        icon = Icons.Default.Home,
        label = UiText.DynamicString("Home")
    ),
    SEARCH(
        route = SearchRoute::class,
        startRoute = SearchRoute,
        icon = Icons.Default.Search,
        label = UiText.DynamicString("Search")
    ),
    SETTINGS(
        route = SettingsRoute::class,
        startRoute = SettingsRoute,
        icon = Icons.Default.Settings,
        label = UiText.DynamicString("Settings")
    )
}
"""

APP_STATE_TEMPLATE = """package __PACKAGE__.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navOptions

@Composable
fun rememberAppState(
    navController: NavHostController = rememberNavController()
): AppState {
    return remember(navController) {
        AppState(navController)
    }
}

@Stable
class AppState(
    val navController: NavHostController
) {
    val currentDestination: NavDestination?
        @Composable get() = navController
            .currentBackStackEntryAsState().value?.destination

    val currentTopLevelDestination: TopLevelDestination?
        @Composable get() = TopLevelDestination.entries.firstOrNull { topLevel ->
            currentDestination?.hasRoute(route = topLevel.route) == true
        }

    val shouldShowBottomBar: Boolean
        @Composable get() = currentTopLevelDestination != null

    val topLevelDestinations: List<TopLevelDestination> = TopLevelDestination.entries

    fun navigateToTopLevelDestination(topLevelDestination: TopLevelDestination) {
        val topLevelNavOptions = navOptions {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
        navController.navigate(topLevelDestination.startRoute, topLevelNavOptions)
    }
}
"""

BOTTOM_BAR_TEMPLATE = """package __PACKAGE__.navigation

import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute

@Composable
fun AppBottomBar(
    destinations: List<TopLevelDestination>,
    onNavigateToDestination: (TopLevelDestination) -> Unit,
    currentDestination: NavDestination?,
    modifier: Modifier = Modifier
) {
    NavigationBar(modifier = modifier) {
        destinations.forEach { destination ->
            val selected = currentDestination?.hasRoute(destination.route) == true
            NavigationBarItem(
                selected = selected,
                onClick = { onNavigateToDestination(destination) },
                icon = {
                    Icon(
                        imageVector = destination.icon,
                        contentDescription = destination.label.asString()
                    )
                },
                label = {
                    Text(text = destination.label.asString())
                }
            )
        }
    }
}
"""

def find_project_root():
    current = Path.cwd()
    while current != current.parent:
        if (current / "gradlew").exists() or (current / "settings.gradle.kts").exists():
            return current
        current = current.parent
    return Path.cwd()

def find_base_package(project_root):
    app_java = project_root / "app" / "src" / "main" / "java"
    if not app_java.exists():
        return "com.example.app"
    for path in app_java.rglob("*.kt"):
        content = path.read_text(encoding="utf-8")
        match = re.search(r"package\s+([a-zA-Z0-9_.]+)", content)
        if match:
            pkg = match.group(1)
            parts = pkg.split(".")
            if len(parts) >= 3:
                return ".".join(parts[:3])
            return pkg
    return "com.example.app"

def update_libs_toml(project_root):
    toml_path = project_root / "gradle" / "libs.versions.toml"
    if not toml_path.exists():
        return
    content = toml_path.read_text(encoding="utf-8")
    changed = False

    if 'navigationCompose = ' not in content:
        content = re.sub(r'(\[versions\][^\[]*)', r'\1navigationCompose = "2.8.5"\n', content)
        changed = True

    if 'androidx-navigation-compose = ' not in content:
        content = re.sub(
            r'(\[libraries\][^\[]*)',
            r'\1androidx-navigation-compose = { group = "androidx.navigation", name = "navigation-compose", version.ref = "navigationCompose" }\n',
            content
        )
        changed = True

    if 'androidx-compose-material-icons-core = ' not in content:
        content = re.sub(
            r'(\[libraries\][^\[]*)',
            r'\1androidx-compose-material-icons-core = { group = "androidx.compose.material", name = "material-icons-core" }\nandroidx-compose-material-icons-extended = { group = "androidx.compose.material", name = "material-icons-extended" }\n',
            content
        )
        changed = True

    if changed:
        toml_path.write_text(content, encoding="utf-8")
        print("Updated gradle/libs.versions.toml with navigation and icons dependencies.")

def update_build_gradle(project_root):
    build_gradle = project_root / "app" / "build.gradle.kts"
    if not build_gradle.exists():
        return
    content = build_gradle.read_text(encoding="utf-8")
    changed = False

    deps_to_add = [
        "implementation(libs.androidx.navigation.compose)",
        "implementation(libs.androidx.compose.material.icons.core)",
        "implementation(libs.androidx.compose.material.icons.extended)"
    ]

    for dep in deps_to_add:
        if dep not in content:
            content = re.sub(r'(dependencies\s*\{)', f'\\1\n    {dep}', content)
            changed = True

    if changed:
        build_gradle.write_text(content, encoding="utf-8")
        print("Updated app/build.gradle.kts with navigation dependencies.")

def scaffold_navigation_files(project_root, base_package):
    pkg_path = project_root / "app" / "src" / "main" / "java" / Path(*base_package.split(".")) / "navigation"
    pkg_path.mkdir(parents=True, exist_ok=True)

    route_file = pkg_path / "Route.kt"
    if not route_file.exists():
        route_file.write_text(ROUTE_TEMPLATE.replace("__PACKAGE__", base_package), encoding="utf-8")
        print(f"Created {route_file.relative_to(project_root)}")

    top_level_file = pkg_path / "TopLevelDestination.kt"
    if not top_level_file.exists():
        top_level_file.write_text(TOP_LEVEL_TEMPLATE.replace("__PACKAGE__", base_package), encoding="utf-8")
        print(f"Created {top_level_file.relative_to(project_root)}")

    app_state_file = pkg_path / "AppState.kt"
    if not app_state_file.exists():
        app_state_file.write_text(APP_STATE_TEMPLATE.replace("__PACKAGE__", base_package), encoding="utf-8")
        print(f"Created {app_state_file.relative_to(project_root)}")

    bottom_bar_file = pkg_path / "AppBottomBar.kt"
    if not bottom_bar_file.exists():
        bottom_bar_file.write_text(BOTTOM_BAR_TEMPLATE.replace("__PACKAGE__", base_package), encoding="utf-8")
        print(f"Created {bottom_bar_file.relative_to(project_root)}")

def main():
    root = find_project_root()
    base_pkg = find_base_package(root)
    print(f"Targeting base package: {base_pkg}")
    update_libs_toml(root)
    update_build_gradle(root)
    scaffold_navigation_files(root, base_pkg)
    print("Navigation setup complete!")

if __name__ == "__main__":
    main()
