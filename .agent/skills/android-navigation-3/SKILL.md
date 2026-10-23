---
name: android-navigation-3
description: "Standards and patterns for Android Jetpack Navigation 3 (androidx.navigation3) in Jetpack Compose — NavDisplay, rememberNavBackStack, NavKey, entryProvider DSL, custom back stacks, type-safe arguments, and MVI ViewModel integration. Trigger on phrases like 'Navigation 3', 'navigation3', 'NavDisplay', 'rememberNavBackStack', 'NavKey', 'entryProvider', 'backStack', or 'Android Navigation 3'."
---

# Android Jetpack Navigation 3 Architecture & Standards

## Core Principles

1. **Developer-Owned Back Stack**:
   - The back stack is a first-class, developer-owned snapshot state (`NavBackStack<T>` via `rememberNavBackStack(startKey)` or a `SnapshotStateList<T>`).
   - Forward navigation is `backStack.add(Key)`, and backward navigation is `backStack.removeAt(backStack.lastIndex)`.
   - Eliminates opaque `NavController` black boxes and Fragment/Activity bundle limitations.

2. **Compose-First `NavDisplay`**:
   - Replaces the legacy `NavHost`.
   - Takes `backStack`, `onBack`, and an `entryProvider` lambda resolving navigation keys to `@Composable` destinations.

3. **Type-Safe Navigation Keys (`NavKey`)**:
   - Routes are `@Serializable` data objects or data classes implementing `androidx.navigation3.runtime.NavKey`.
   - Restrict arguments to scalar primitives (`Long`, `Int`, `String`, `Boolean`) to guarantee state serialization safety.

4. **`entryProvider` DSL**:
   - Map route types directly using `entry<Key> { key -> ... }`.
   - The destination receives the strongly-typed route instance with all parameters directly accessible.

5. **Decoupled Feature Architecture**:
   - Features expose root composables (e.g. `<Feature>Root`) with lambda callbacks for navigation (e.g., `onNavigateToCreate`, `onNavigateToEdit`, `onBack`).
   - All navigation routes and back stack mutations are orchestrated centrally in `NavigationRoot.kt` (in `:app` or `:navigation`).

6. **Create / Edit Dual-Mode Pattern**:
   - A single screen/route can serve both Creation and Editing by accepting an optional scalar identifier:
     `@Serializable data class VaultEdit(val vaultId: Long? = null) : NavKey`
   - `vaultId == null`: Create mode.
   - `vaultId != null`: Edit mode. ViewModel loads existing data and pre-fills form state.

---

## Directory Structure

```text
navigation/
├── Route.kt              # Sealed interface Route : NavKey definitions
└── NavigationRoot.kt     # NavDisplay container & backStack orchestration
<feature>/presentation/
├── <Feature>Screen.kt    # Dumb UI (<Feature>Screen) & Nav wrapper (<Feature>Root)
├── <Feature>ViewModel.kt # MVI ViewModel with OnInit/Route handling
├── <Feature>State.kt     # Immutable UI State
└── <Feature>Action.kt    # User Actions
```

---

## Dependencies

### `gradle/libs.versions.toml`:
```toml
[versions]
navigation3 = "1.3.0-alpha03"
kotlinxSerializationJson = "1.8.0"

[libraries]
androidx-navigation3-runtime = { module = "androidx.navigation3:navigation3-runtime", version.ref = "navigation3" }
androidx-navigation3-ui = { module = "androidx.navigation3:navigation3-ui", version.ref = "navigation3" }
kotlinx-serialization-json = { group = "org.jetbrains.kotlinx", name = "kotlinx-serialization-json", version.ref = "kotlinxSerializationJson" }

[plugins]
kotlin-serialization = { id = "org.jetbrains.kotlin.plugin.serialization", version.ref = "kotlin" }
```

### `build.gradle.kts`:
```kotlin
plugins {
    alias(libs.plugins.kotlin.serialization)
}

dependencies {
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.kotlinx.serialization.json)
}
```

---

## Implementation Patterns

### 1. Routes Definition (`Route.kt`)

```kotlin
package com.example.app.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
sealed interface Route : NavKey {

    @Serializable
    data object VaultList : Route

    @Serializable
    data class VaultEdit(val vaultId: Long? = null) : Route
}
```

---

### 2. Navigation Container (`NavigationRoot.kt`)

```kotlin
package com.example.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.example.app.feature.vault.presentation.create.CreateVaultRoot
import com.example.app.feature.vault.presentation.vault.VaultRoot

@Composable
fun NavigationRoot(
    modifier: Modifier = Modifier
) {
    val backStack = rememberNavBackStack(Route.VaultList)

    NavDisplay(
        modifier = modifier,
        backStack = backStack,
        onBack = {
            if (backStack.size > 1) {
                backStack.removeAt(backStack.lastIndex)
            }
        },
        entryProvider = entryProvider {
            entry<Route.VaultList> {
                VaultRoot(
                    onNavigateToCreate = {
                        backStack.add(Route.VaultEdit(vaultId = null))
                    },
                    onNavigateToEdit = { vaultId ->
                        backStack.add(Route.VaultEdit(vaultId = vaultId))
                    }
                )
            }

            entry<Route.VaultEdit> { route ->
                CreateVaultRoot(
                    vaultId = route.vaultId,
                    onBack = {
                        if (backStack.size > 1) {
                            backStack.removeAt(backStack.lastIndex)
                        }
                    }
                )
            }
        }
    )
}
```

---

### 3. Screen Integration with Navigation Callbacks

```kotlin
@Composable
fun VaultRoot(
    viewModel: VaultViewModel = koinViewModel(),
    onNavigateToCreate: () -> Unit,
    onNavigateToEdit: (Long) -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    VaultScreen(
        state = state,
        onAction = { action ->
            when (action) {
                is VaultAction.OnCreateClick -> onNavigateToCreate()
                is VaultAction.OnEditClick -> onNavigateToEdit(action.id)
                else -> viewModel.onAction(action)
            }
        }
    )
}
```

---

### 4. Create / Edit Dual Mode ViewModel Pattern

```kotlin
class CreateVaultViewModel(
    private val vaultRepository: VaultRepository
) : ViewModel() {

    fun onAction(action: CreateVaultAction) {
        when (action) {
            is CreateVaultAction.OnInit -> {
                if (action.vaultId != null && action.vaultId != _state.value.vaultId) {
                    loadExistingVault(action.vaultId)
                } else if (action.vaultId == null) {
                    _state.value = CreateVaultState(title = "Create")
                }
            }
            is CreateVaultAction.OnSaveClick -> saveVault()
            else -> Unit
        }
    }

    private fun loadExistingVault(vaultId: Long) {
        _state.update { it.copy(vaultId = vaultId, title = "Edit", isLoading = true) }
        viewModelScope.launch {
            val vaultResult = vaultRepository.getVaultById(vaultId)
            val contentResult = vaultRepository.getVaultContent(vaultId)

            if (vaultResult is Result.Success && contentResult is Result.Success) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        vaultTitle = TextFieldState(vaultResult.data.title),
                        content = contentResult.data.toUi()
                    )
                }
            }
        }
    }
}
```
