---
name: android-compose-presentation
description: Compose UI patterns, Design System components, and presentation utilities (ObserveAsEvents, UiText) for Android/KMP - stability, recomposition, side effects, lazy layouts, graphicsLayer, previews, a11y, TextField MVI, AppIcons, stringResource rules, three-tier component hierarchy, splash screen, installSplashScreen, setKeepOnScreenCondition.
---

# Android Compose Presentation

## Core Principles

1. **The UI is Dumb**: Composables strictly render State and emit Actions to the ViewModel. Zero business logic.
2. **Standardized Root Navigation & `onAction` Dispatch**:
   - In `<Feature>Root`, navigation actions are handled via exhaustive `when(action)`, always passing down to `viewModel.onAction(action)`. Never swallow actions. `<Feature>Screen` accepts only `(action: <Feature>Action) -> Unit`.
3. **File Separation**:
   - `State` and `Action` must be in separate files. Do not combine Enums/UI models in `State.kt` or `Screen.kt`.

---

## Part 1: Compose UI Patterns

### 1. Stability & Recomposition

Modern Compose uses Strong Skipping by default.
- Use `@Stable` only for unstable types (e.g., `List<T>`, generic types).

```kotlin
@Stable
data class UserListState(val users: List<UserUi> = emptyList(), val isLoading: Boolean = false)
```

### 2. State Ownership & Lifecycle

- State lives in ViewModel as a single `_state = MutableStateFlow(<Feature>State())`.
- Collect via `collectAsStateWithLifecycle()`. Use `remember` only for framework-internal states.
- Use `derivedStateOf` only for high-frequency internal state reading.

```kotlin
val state by viewModel.state.collectAsStateWithLifecycle()
val showScrollToTop by remember { derivedStateOf { listState.firstVisibleItemIndex > 5 } }
```

### 3. Side Effects & Lifecycle Safety

- Prefer ViewModel Actions over composable side effects.
- One-off events: use `ObserveAsEvents` (see [Presentation Utilities](#part-2-presentation-utilities)). Lifecycle events: use `DisposableEffect`.

```kotlin
@Composable
fun ObserveLifecycle(onStart: () -> Unit = {}, onStop: () -> Unit = {}) {
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) { Lifecycle.Event.ON_START -> onStart(); Lifecycle.Event.ON_STOP -> onStop(); else -> Unit }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
}
```

### 4. Lazy Layouts

Provide a stable `key` in `items()` to avoid layout thrashing:

```kotlin
LazyColumn(state = listState) {
    items(items = state.items, key = { it.id }) { item -> ItemRow(item = item, onClick = { onAction(FeatureAction.OnItemClick(item.id)) }) }
}
```

### 5. Optimized Animations

Animate in the **Draw phase** using `Modifier.graphicsLayer` or Offset lambdas to avoid recomposition.

```kotlin
val alpha by animateFloatAsState(if (state.isVisible) 1f else 0f)
Box(modifier = Modifier.graphicsLayer { this.alpha = alpha })
```

### 6. Modifier Extensions

Use plain modifier extensions/factories. Avoid `@Composable` unless necessary.

```kotlin
fun Modifier.surfaceCard(shape: Shape = RoundedCornerShape(16.dp), backgroundColor: Color): Modifier = 
    this.clip(shape).background(backgroundColor)
```

### 7. Previews

- Every Component/Screen MUST have a `@Preview(showBackground = true)` wrapped in `AppTheme`.
- Preview composables should be `private` with realistic mock UI models.

### 8. Accessibility (a11y)

- Informational components: localized `contentDescription`.
- Decorative elements: `contentDescription = null`.

### 9. Form Inputs (MVI)

Hoist input state to ViewModel. Keystrokes dispatch Actions:

```kotlin
OutlinedTextField(
    value = state.searchQuery,
    onValueChange = { onAction(FeatureAction.OnSearchQueryChange(it)) },
    modifier = Modifier.fillMaxWidth()
)
```

### 10. Process Death — SavedStateHandle

When a screen involves complex forms or critical user input, restore essential fields using `SavedStateHandle`:

```kotlin
class NoteEditorViewModel(
    private val savedStateHandle: SavedStateHandle,
    private val noteRepository: NoteRepository
) : ViewModel() {
    private val _state = MutableStateFlow(
        NoteEditorState(
            title = savedStateHandle["title"] ?: "",
            body = savedStateHandle["body"] ?: ""
        )
    )
 
    fun onAction(action: NoteEditorAction) {
        when (action) {
            is NoteEditorAction.OnTitleChange -> {
                savedStateHandle["title"] = action.title
                _state.update { it.copy(title = action.title) }
            }
        }
    }
}
```

Only save what truly matters after process death — not the entire state.

### 11. Custom Modifier Extensions with Modifier.Node

Prefer plain `Modifier` extension functions or `Modifier.Node`-based factories. Do not make modifier extensions `@Composable`:

```kotlin
// Good — plain extension
fun Modifier.shimmerEffect(): Modifier = composed {
    // shimmer implementation
}

// Better — Modifier factory (no composition needed)
fun Modifier.roundedBackground(color: Color, radius: Dp) =
    background(color, RoundedCornerShape(radius))
```

### 12. TextField Action-Dispatched Pattern

Text input state lives in the ViewModel. Every keystroke dispatches an Action, no local text state:

```kotlin
// In the Screen composable
TextField(
    value = state.title,
    onValueChange = { onAction(NoteEditorAction.OnTitleChange(it)) }
)
```

---

## Part 2: Presentation Utilities

### Architecture
```text
app/src/main/java/<base_package>/core/presentation/util/
├── ObserveAsEvents.kt
└── UiText.kt
```

### Setup Script

```bash
python3 .agents/skills/android-compose-presentation/scripts/setup_presentation_util.py
```

### 1. `ObserveAsEvents`
```kotlin
@Composable
fun <T> ObserveAsEvents(
    flow: Flow<T>,
    key1: Any? = null,
    key2: Any? = null,
    onEvent: (T) -> Unit
)
```
- Stops collecting in background (`Lifecycle.State.STARTED`).
- Ensures UI execution on `Dispatchers.Main.immediate`.

### 2. `UiText`
```kotlin
@Stable
sealed interface UiText {
    @Stable data class DynamicString(val value: String) : UiText
    @Stable data class StringResource(
        @param:StringRes val resId: Int,
        val args: List<Any> = emptyList()
    ) : UiText
    @Composable fun asString(): String
    fun asString(context: Context): String
    suspend fun asStringAsync(context: Context): String
}
```

#### `UiText` Scope Rules

- **`UiText` is strictly for Data/Domain error propagation**: Use only when an error originates from Data/Domain layer (e.g. `DataError.toUiText()`) and is stored in `State.error: UiText?` without Android `Context`.
- **NEVER use `UiText` as parameters for standard UI components** like `LoadingView(message: UiText)` — call-sites already have `@Composable` context and should use `stringResource(...)`.
- **NEVER wrap hardcoded string literals with `UiText.DynamicString("...")`** to bypass localization. `UiText.DynamicString` is strictly reserved for dynamic runtime strings originating from backend servers or direct user inputs.

### Usage Examples

#### In State & ViewModel
```kotlin
data class FeatureState(val errorMessage: UiText? = null)

class FeatureViewModel : ViewModel() {
    fun onAction(action: FeatureAction) {
        // String resource without Context
        _state.update { it.copy(errorMessage = UiText.StringResource(R.string.error)) }
    }
}
```

#### In Compose UI
```kotlin
@Composable
fun FeatureRoot(viewModel: FeatureViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    ObserveAsEvents(flow = viewModel.events) { event -> /* Handle event */ }
    
    state.errorMessage?.let { error ->
        Text(text = error.asString(), color = MaterialTheme.colorScheme.error)
    }
}
```

#### Converting Domain `DataError`
```kotlin
fun DataError.asUiText(): UiText {
    return when (this) {
        DataError.Network.NO_INTERNET -> UiText.StringResource(R.string.error_no_internet)
        else -> UiText.StringResource(R.string.error_unknown)
    }
}
```

---

## Part 3: Design System Guidelines

### Strict String Resource Usage

- **Composables should accept `String` (or `@Composable () -> Unit`)**: Standard UI components like `LoadingView(message: String?)` or `EmptyStateView(title: String, description: String)` must accept plain `String` parameters, passed from the call-site via `stringResource(R.string.*)`.
- Always add new string IDs to `res/values/strings.xml` first.

### Centralized Icon Management (`AppIcons`)

- All `ImageVector` icons must be referenced from `AppIcons`, never directly from `androidx.compose.material.icons.*` in screens.

### Three-Tier Component Hierarchy

- **Tier 1: Design System** (`core/presentation/designsystem/`): Atomic building blocks. 0% business logic. Slot API. Primitives only. (e.g., `AppButton`)
- **Tier 2: Shared Common Components** (`core/presentation/components/`): Cross-feature shared composite views (e.g., `EmptyStateView`). Accept `String` from `stringResource`. Error components can accept `UiText` from `State.error`.
- **Tier 3: Feature-Specific Components** (`<feature>/presentation/components/`): Dedicated to a single screen/flow. Binds to `<Feature>Ui` model.

### Directory Layout

```text
core/presentation/
├── designsystem/
│   ├── icon/AppIcons.kt
│   └── components/AppButton.kt
├── components/EmptyStateView.kt
└── util/
    ├── ObserveAsEvents.kt
    └── UiText.kt
<feature>/presentation/components/<Feature>ItemRow.kt
```

### Component Placement Decision Rule

| Rule | Location |
| --- | --- |
| Reusable across projects without code modification? | **Design System** (`core/presentation/designsystem/components/`) |
| Generic UX pattern shared across screens (empty list, loading)? | **Common Shared** (`core/presentation/components/`) |
| Tied to a specific domain entity or feature screen? | **Feature Specific** (`<feature>/presentation/components/`) |

### Setup Script

```bash
python3 .agents/skills/android-compose-presentation/scripts/setup_designsystem.py
```

### Code Templates

#### 1. `AppIcons.kt`

```kotlin
object AppIcons {
    val Home = Icons.Default.Home
    val Search = Icons.Default.Search
    val Settings = Icons.Default.Settings
    val ArrowBack = Icons.AutoMirrored.Filled.ArrowBack
    val Refresh = Icons.Default.Refresh
    val Inbox = Icons.Default.Inbox
}
```

#### 2. `AppButton.kt`

```kotlin
@Composable
fun AppButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    content: @Composable RowScope.() -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled && !isLoading,
        shape = MaterialTheme.shapes.medium
    ) {
        if (isLoading) CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
        else content()
    }
}

@Preview(showBackground = true)
@Composable
private fun AppButtonPreview() {
    AppTheme {
        AppButton(onClick = {}) {
            Text(text = "App Button")
        }
    }
}
```

#### 3. `EmptyStateView.kt`

```kotlin
@Composable
fun EmptyStateView(
    title: String,
    description: String,
    icon: ImageVector = AppIcons.Inbox,
    actionText: String? = null,
    onActionClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(64.dp))
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = title, style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = description, style = MaterialTheme.typography.bodyMedium)
        if (actionText != null && onActionClick != null) {
            Spacer(modifier = Modifier.height(24.dp))
            AppButton(onClick = onActionClick) { Text(text = actionText) }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun EmptyStateViewPreview() {
    AppTheme {
        EmptyStateView(
            title = "No Items",
            description = "There are currently no items to display.",
            actionText = "Retry",
            onActionClick = {}
        )
    }
}
```

---

## Part 4: Splash Screen Setup

### Core Principles

1. **Unified Splash UX Across All Android Versions**: Utilize `androidx.core:core-splashscreen` to deliver a consistent launch experience from Android 5.0 (API 21) up to Android 12+ (API 31+) native SplashScreen API without blank screen flashes.
2. **Centralized Dependency Management**: Keep the SplashScreen library version centralized in `gradle/libs.versions.toml`.
3. **Theme-Driven Transition**: Use `Theme.SplashScreen` with `postSplashScreenTheme` so that Android OS renders the splash screen instantly before any process or Compose layout initializes, cleanly transitioning to the main application theme.
4. **Controlled Hold Condition**: Use `splashScreen.setKeepOnScreenCondition { ... }` tied to ViewModel state (e.g. auth check, initial sync) rather than artificial timers or delays.
5. **Icon Mask Guidelines (Splash Screen)**:
   - Without background: total container 288dp, inner circular safe zone diameter 192dp.
   - With icon background: total container 240dp, inner circular safe zone diameter 160dp.
   *(Note: 108dp container / 72dp safe zone applies to adaptive launcher icons, not splash screens).*

### Setup Script

```bash
# Automated setup (injects toml, build.gradle.kts, splash_theme.xml, and manifest)
python3 .agents/skills/android-compose-presentation/scripts/setup_splash_screen.py
```

### Theme Configuration

Create or add to `res/values/splash_theme.xml` (or `themes.xml`):

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <!-- Starting splash theme extending Theme.SplashScreen -->
    <style name="Theme.App.Starting" parent="Theme.SplashScreen">
        <!-- Splash screen background color -->
        <item name="windowSplashScreenBackground">@color/splash_background</item>

        <!-- Centered icon (vector drawable recommended) -->
        <item name="windowSplashScreenAnimatedIcon">@drawable/ic_splash_logo</item>

        <!-- Target app theme to apply after splash finishes -->
        <item name="postSplashScreenTheme">@style/Theme.MyApp</item>
    </style>
</resources>
```

### Activity & ViewModel Implementation

#### 1. `MainActivity.kt`

```kotlin
package com.example.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import org.koin.androidx.viewmodel.ext.android.viewModel
import com.example.app.ui.theme.MyAppTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        // 1. MUST be called BEFORE super.onCreate()
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        // 2. Keep splash visible while app data/auth is loading
        splashScreen.setKeepOnScreenCondition {
            viewModel.isReady.value == false
        }

        setContent {
            MyAppTheme {
                MainAppNavHost()
            }
        }
    }
}
```

#### 2. `MainViewModel.kt` (State-driven dismiss)

```kotlin
class MainViewModel : ViewModel() {

    private val _isReady = MutableStateFlow(false)
    val isReady = _isReady.asStateFlow()

    init {
        viewModelScope.launch {
            // Perform necessary async initialization (e.g. User Session, Preferences)
            loadInitialData()
            _isReady.value = true
        }
    }

    private suspend fun loadInitialData() {
        // e.g. delay / auth validation / db warmup
    }
}
```
