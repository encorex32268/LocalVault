---
name: android-navigation
description: "Type-safe Compose Navigation for Android/KMP — route objects, TopLevelDestination, AppState, BottomNavigationBar, feature nav graphs, cross-feature callbacks, and wiring in app module. Use this skill whenever setting up navigation, defining routes, adding screens to nav graphs, navigating between features, managing bottom bars, or wiring nav graphs. Trigger on phrases like 'set up navigation', 'add a route', 'navigate between screens', 'nav graph', 'NavController', 'type-safe nav', 'cross-feature navigation', 'NavGraphBuilder', 'TopLevelDestination', 'AppState', or 'BottomBar'."
---

# Android / KMP Type-Safe Navigation 2.8+ & Architecture

## Core Principles

1. **Type-Safe Routes**: Destinations are `@Serializable` data objects (for screens without arguments) or data classes (for screens with arguments) using KotlinX Serialization.
2. **Top-Level Destinations (NiA Pattern)**: Encapsulate primary tabs (icon, label, route) and preserve backstack state across tab switches (`saveState = true`, `restoreState = true`).
3. **Adaptive BottomBar (`AppState`)**: Automatically displayed on top-level destinations and hidden elsewhere using `currentDestination?.hasRoute(route)`.
4. **Feature Nav Graph Isolation**: Each feature exposes a `NavGraphBuilder.<feature>Graph` (or `NavGraphBuilder.<feature>Screen`) extension function defined in the feature's `presentation` module.
5. **Intra-Feature vs. Cross-Feature Navigation**:
   - **Within a feature**: Navigation uses the `NavController` passed into the feature nav graph.
   - **Across features**: Navigation is strictly handled via **lambda callbacks** (e.g., `onNavigateToEditor: (String) -> Unit`). Features never import routes from another feature module.
6. **Wiring in `:app`**: All feature nav graphs and cross-feature callbacks are assembled in the `:app` module.
7. **Scalar Parameter Passing Only**: Never pass complex domain models or Parcelable/Serializable objects across routes. Pass scalar identifiers (ID strings, integers) and fetch data in the destination screen's ViewModel.

## Directory Structure

```text
navigation/
├── Route.kt
├── TopLevelDestination.kt
├── AppState.kt
├── AppBottomBar.kt
└── NavigationRoot.kt
<feature>/presentation/
├── <Feature>Navigation.kt
└── <Feature>Screen.kt
```

## Quick Setup Script

```bash
python3 .agents/skills/android-navigation/scripts/setup_navigation.py
```

## Dependencies

`gradle/libs.versions.toml`:
```toml
[versions]
kotlinx-serialization = "1.7.3"
navigationCompose = "2.8.5"

[libraries]
kotlinx-serialization-json = { group = "org.jetbrains.kotlinx", name = "kotlinx-serialization-json", version.ref = "kotlinx-serialization" }
androidx-navigation-compose = { group = "androidx.navigation", name = "navigation-compose", version.ref = "navigationCompose" }
androidx-compose-material-icons-core = { group = "androidx.compose.material", name = "material-icons-core" }
androidx-compose-material-icons-extended = { group = "androidx.compose.material", name = "material-icons-extended" }

[plugins]
kotlin-serialization = { id = "org.jetbrains.kotlin.plugin.serialization", version.ref = "kotlin" }
```

`build.gradle.kts`:
```kotlin
plugins {
    alias(libs.plugins.kotlin.serialization)
}

dependencies {
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.compose.material.icons.core)
    implementation(libs.androidx.compose.material.icons.extended)
}
```

## Parameter Passing Rules

- **Scalar arguments only**: Restrict route parameters to primitive/scalar types (`String`, `Int`, `Long`, `Boolean`).
- **Prohibit complex models**: Never pass Parcelable, Serializable domain objects, or large entity classes via navigation arguments.
- **Pass IDs, load in ViewModel**: Pass the unique identifier via the route, and let the destination screen's ViewModel load fresh state from the repository or use case.
- **Avoid large bundles**: Passing large objects risks `TransactionTooLargeException` and state desynchronization during Android process death or config changes.
- **Extract with `backStackEntry.toRoute<Route>()`**: Use Navigation 2.8+'s type-safe deserializer.

```kotlin
// Define route with scalar argument
@Serializable data class NoteDetailRoute(val noteId: String)

// Navigate to route
navController.navigate(NoteDetailRoute(noteId = "abc123"))

// Extract in composable destination
composable<NoteDetailRoute> { backStackEntry ->
    val route: NoteDetailRoute = backStackEntry.toRoute()
    NoteDetailRoot(
        noteId = route.noteId,
        onNavigateBack = { navController.navigateUp() }
    )
}
```

## Code Templates

### 1. Routes (`navigation/Route.kt` or `<feature>/presentation/Route.kt`)

Use `data object` for screens with no parameters, `data class` for screens with arguments:

```kotlin
import kotlinx.serialization.Serializable

// Top-Level / Core Routes
@Serializable data object HomeRoute
@Serializable data object SearchRoute
@Serializable data object SettingsRoute

// Feature Routes with Arguments
@Serializable data class DetailRoute(val id: String, val title: String? = null)

// Feature-specific Route examples
@Serializable data object NotesGraphRoute
@Serializable data object NoteListRoute
@Serializable data class NoteDetailRoute(val noteId: String)
@Serializable data class EditorRoute(val noteId: String? = null)
```

### 2. Top-Level Destinations (`navigation/TopLevelDestination.kt`)

```kotlin
import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import kotlin.reflect.KClass

enum class TopLevelDestination(
    val route: KClass<*>,
    val startRoute: Any,
    val icon: ImageVector,
    @param:StringRes val labelRes: Int
) {
    HOME(HomeRoute::class, HomeRoute, Icons.Default.Home, R.string.nav_home),
    SEARCH(SearchRoute::class, SearchRoute, Icons.Default.Search, R.string.nav_search),
    SETTINGS(SettingsRoute::class, SettingsRoute, Icons.Default.Settings, R.string.nav_settings)
}
```

### 3. State Holder (`navigation/AppState.kt`)

```kotlin
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navOptions

@Composable
fun rememberAppState(
    navController: NavHostController = rememberNavController()
): AppState {
    return remember(navController) { AppState(navController) }
}

@Stable
class AppState(val navController: NavHostController) {
    val currentDestination: NavDestination?
        @Composable get() = navController.currentBackStackEntryAsState().value?.destination

    val currentTopLevelDestination: TopLevelDestination?
        @Composable get() = TopLevelDestination.entries.firstOrNull { topLevel ->
            currentDestination?.hierarchy?.any { it.hasRoute(route = topLevel.route) } == true
        }

    val shouldShowBottomBar: Boolean @Composable get() = currentTopLevelDestination != null
    val topLevelDestinations: List<TopLevelDestination> = TopLevelDestination.entries

    fun navigateToTopLevelDestination(topLevelDestination: TopLevelDestination) {
        val topLevelNavOptions = navOptions {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
        navController.navigate(topLevelDestination.startRoute, topLevelNavOptions)
    }
}
```

### 4. Bottom Navigation Bar (`navigation/AppBottomBar.kt`)

```kotlin
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy

@Composable
fun AppBottomBar(
    destinations: List<TopLevelDestination>,
    onNavigateToDestination: (TopLevelDestination) -> Unit,
    currentDestination: NavDestination?,
    modifier: Modifier = Modifier
) {
    NavigationBar(modifier = modifier) {
        destinations.forEach { destination ->
            val selected = currentDestination?.hierarchy?.any { it.hasRoute(destination.route) } == true
            NavigationBarItem(
                selected = selected,
                onClick = { onNavigateToDestination(destination) },
                icon = {
                    Icon(
                        imageVector = destination.icon,
                        contentDescription = stringResource(destination.labelRes)
                    )
                },
                label = {
                    Text(text = stringResource(destination.labelRes))
                }
            )
        }
    }
}
```

### 5. Feature Nav Graph & Navigation Extensions (`<feature>/presentation/<Feature>Navigation.kt`)

#### Pattern A: Feature Graph Extension (`NavGraphBuilder.<feature>Graph`)
Used when a feature contains multiple sub-screens or forms a coherent navigation sub-graph. Cross-feature navigation is exposed strictly via lambda callbacks:

```kotlin
// feature:notes:presentation
fun NavGraphBuilder.notesGraph(
    navController: NavController,
    onNavigateToEditor: (String) -> Unit // Decoupled callback for cross-feature navigation
) {
    navigation<NotesGraphRoute>(startDestination = NoteListRoute) {
        composable<NoteListRoute> {
            NoteListRoot(
                // Intra-feature navigation uses navController directly
                onNavigateToDetail = { noteId ->
                    navController.navigate(NoteDetailRoute(noteId))
                }
            )
        }
        composable<NoteDetailRoute> { backStackEntry ->
            val route: NoteDetailRoute = backStackEntry.toRoute()
            NoteDetailRoot(
                noteId = route.noteId,
                // Cross-feature navigation invokes the lambda callback
                onNavigateToEditor = onNavigateToEditor,
                onNavigateBack = { navController.navigateUp() }
            )
        }
    }
}
```

#### Pattern B: Single Screen Extension (`NavGraphBuilder.<feature>Screen`)
Used for standalone screens or top-level tabs:

```kotlin
fun NavController.navigateToFeature(navOptions: NavOptions? = null) {
    navigate(route = HomeRoute, navOptions = navOptions)
}

fun NavGraphBuilder.featureScreen(onNavigateToDetail: (String) -> Unit = {}) {
    composable<HomeRoute> {
        FeatureRoot(onNavigateToDetail = onNavigateToDetail)
    }
}
```

### 6. Wiring in `:app` Module (`navigation/NavigationRoot.kt`)

All feature nav graphs and cross-feature callbacks are assembled in `:app`. Cross-feature navigation is expressed as a lambda callback — never by importing routes between feature modules:

```kotlin
// :app module
@Composable
fun NavigationRoot(
    appState: AppState = rememberAppState(),
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier,
        bottomBar = {
            if (appState.shouldShowBottomBar) {
                AppBottomBar(
                    destinations = appState.topLevelDestinations,
                    onNavigateToDestination = appState::navigateToTopLevelDestination,
                    currentDestination = appState.currentDestination
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = appState.navController,
            startDestination = HomeRoute,
            modifier = Modifier.padding(innerPadding)
        ) {
            // Standalone top-level screen
            featureScreen(
                onNavigateToDetail = { id ->
                    appState.navController.navigate(DetailRoute(id))
                }
            )

            // Register Detail screen
            composable<DetailRoute> { backStackEntry ->
                val route: DetailRoute = backStackEntry.toRoute()
                DetailRoot(
                    id = route.id,
                    title = route.title,
                    onNavigateBack = { appState.navController.navigateUp() }
                )
            }

            // Feature graph with cross-feature lambda callback
            notesGraph(
                navController = appState.navController,
                onNavigateToEditor = { noteId ->
                    // App module bridges feature:notes to feature:editor
                    appState.navController.navigate(EditorRoute(noteId))
                }
            )

            // Destination feature graph
            editorGraph(
                navController = appState.navController,
                onNavigateBack = { appState.navController.navigateUp() }
            )
        }
    }
}
```

## Naming Conventions

| Thing | Convention | Example |
|---|---|---|
| Nav route | `<Screen>Route` | `NoteListRoute`, `NoteDetailRoute`, `HomeRoute` |
| Feature nav graph | `<feature>Graph(...)` on `NavGraphBuilder` | `notesGraph(...)`, `editorGraph(...)` |
| Feature single screen | `<feature>Screen(...)` on `NavGraphBuilder` | `featureScreen(...)`, `settingsScreen(...)` |
| Navigation action | `navigateTo<Destination>(...)` on `NavController` | `navigateToFeature(...)`, `navigateToDetail(...)` |
| Cross-feature callback | `onNavigateTo<Destination>: (...) -> Unit` | `onNavigateToEditor: (String) -> Unit` |

## Checklist: Adding Navigation to a New Feature

- [ ] Define `@Serializable` route objects for each screen in `feature:presentation` (`data object` for no arguments, `data class` with scalar arguments).
- [ ] Add feature nav graph or screen extension function (`NavGraphBuilder.<feature>Graph(...)` or `NavGraphBuilder.<feature>Screen(...)`).
- [ ] Pass `NavController` for intra-feature navigation within the graph.
- [ ] Expose cross-feature destinations strictly as lambda callbacks (prohibit importing routes from other feature modules).
- [ ] Restrict route parameters to scalar types and extract using `backStackEntry.toRoute<Route>()`.
- [ ] Wire the nav graph and cross-feature callbacks in `:app`'s `NavHost`.
