---
name: android-single-app-module-feature-create
description: Creates a new feature folder structure and standardized MVI presentation pattern. Use whenever the user asks to create, scaffold, or generate a new feature.
---

# Android Single App Module Feature Creator

## Directory Structure

```text
<feature_name>/
├── data/
│   ├── database/
│   ├── mapper/
│   └── repository/
├── domain/
│   ├── model/
│   │   └── <Feature>Item.kt
│   └── repository/
├── presentation/
│   ├── model/
│   │   └── <Feature>Ui.kt
│   ├── mapper/
│   │   └── <Feature>UiMapper.kt
│   ├── <Feature>State.kt
│   ├── <Feature>Action.kt
│   ├── <Feature>ViewModel.kt
│   ├── <Feature>Screen.kt
│   └── <Feature>Navigation.kt
└── di/
    └── <Feature>Module.kt
```

## Conventions
- **Naming**: Folder lowercase (`home`). Classes PascalCase (`HomeState.kt`, `FeatureBViewModel.kt`).
- **MVI Pattern**:
  - **State**: `data class` with defaults.
  - **Action**: `sealed interface`.
  - **ViewModel**: Uses `MutableStateFlow` and `.stateIn()`, `onStart` guard, handles actions in `onAction`.
  - **Screen**: `<Feature>Root` collects state, handles navigation, delegates to `<Feature>Screen`.
- **Localization**:
  > See `android-compose-presentation` skill for UiText/stringResource rules.
- **File Separation**: State and Action in separate files. Enums/UI models not in State/Screen files.
- **Offline-First**:
  > See `android-data-layer` skill for offline-first repository patterns.
- **State Source of Truth**:
  > See `android-compose-presentation` skill for state ownership rules.

## Automated Scaffolding

```bash
python3 .agents/skills/android-single-app-module-feature-create/scripts/create_feature.py <feature_name>
```

## Code Templates

### 1. `<Feature>State.kt`
```kotlin
data class <Feature>State(
    val isLoading: Boolean = false
)
```

### 2. `<Feature>Action.kt`
```kotlin
sealed interface <Feature>Action {}
```

### 3. `<Feature>ViewModel.kt`
```kotlin
class <Feature>ViewModel : ViewModel() {
    private var hasLoadedInitialData = false

    private val _state = MutableStateFlow(<Feature>State())
    val state = _state.onStart {
        if (!hasLoadedInitialData) {
            hasLoadedInitialData = true
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = <Feature>State()
    )

    fun onAction(action: <Feature>Action) {
        when (action) {
            else -> Unit
        }
    }
}
```

### 4. `<Feature>Screen.kt`
```kotlin
@Composable
fun <Feature>Root(
    viewModel: <Feature>ViewModel = koinViewModel(),
    onNavigateToDetail: ((String) -> Unit)? = null
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    <Feature>Screen(
        state = state,
        onAction = { action ->
            when (action) {
                // is <Feature>Action.OnItemClick -> onNavigateToDetail?.invoke(action.id)
                else -> Unit
            }
            viewModel.onAction(action)
        }
    )
}

@Composable
internal fun <Feature>Screen(
    state: <Feature>State,
    onAction: (<Feature>Action) -> Unit,
    modifier: Modifier = Modifier
) {
}

@Preview(showBackground = true)
@Composable
private fun <Feature>ScreenPreview() {
    AppTheme {
        <Feature>Screen(
            state = <Feature>State(),
            onAction = {}
        )
    }
}
```
