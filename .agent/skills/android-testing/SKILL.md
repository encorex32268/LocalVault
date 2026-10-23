---
name: android-testing
description: "Testing patterns for Android/KMP — ViewModel unit tests with JUnit5, Turbine, AssertK, UnconfinedTestDispatcher, MainCoroutineExtension, fake repositories, SavedStateHandle, Compose UI tests, and Robot pattern. Use this skill whenever writing or reviewing tests for ViewModels, repositories, use cases, or Compose screens. Trigger on phrases like 'write a test', 'unit test the ViewModel', 'test a repository', 'Turbine', 'fake repository', 'UnconfinedTestDispatcher', 'runTest', 'ComposeTestRule', 'JUnit5', 'Robot pattern', or 'MainCoroutineExtension'."
---

# Android / KMP Testing

## Stack
| Concern | Library | Gradle Dependency |
|---|---|---|
| Test framework | JUnit 5 | `junit-jupiter-api` + `engine` + `params` |
| Test Platform Runner | JUnit Platform Launcher | `junit-platform-launcher` |
| JUnit 4 runner (for ComposeTestRule) | JUnit Vintage Engine | `junit-vintage-engine` |
| Assertions | AssertK | `assertk` |
| Flow testing | Turbine | `turbine` |
| Coroutine testing | Coroutines Test | `kotlinx-coroutines-test` |
| UI testing | Compose Test Rule | `ui-test-junit4` + `ui-test-manifest` |

## Setup Script
```bash
python3 .agents/skills/android-testing/scripts/setup_testing.py
```

## Gradle Configuration
```toml
# gradle/libs.versions.toml
[versions]
junit5 = "5.10.1"
junitPlatform = "1.10.1"
assertk = "0.28.1"
turbine = "1.2.0"
coroutinesTest = "1.10.1"

[libraries]
junit-jupiter-api = { group = "org.junit.jupiter", name = "junit-jupiter-api", version.ref = "junit5" }
junit-jupiter-params = { group = "org.junit.jupiter", name = "junit-jupiter-params", version.ref = "junit5" }
junit-jupiter-engine = { group = "org.junit.jupiter", name = "junit-jupiter-engine", version.ref = "junit5" }
junit-vintage-engine = { group = "org.junit.vintage", name = "junit-vintage-engine", version.ref = "junit5" }
junit-platform-launcher = { group = "org.junit.platform", name = "junit-platform-launcher", version.ref = "junitPlatform" }
assertk = { group = "com.willowtreeapps.assertk", name = "assertk", version.ref = "assertk" }
turbine = { group = "app.cash.turbine", name = "turbine", version.ref = "turbine" }
kotlinx-coroutines-test = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-test", version.ref = "coroutinesTest" }

[bundles]
testing-unit = ["junit-jupiter-api", "junit-jupiter-params", "assertk", "turbine", "kotlinx-coroutines-test"]

# app/build.gradle.kts
android {
    testOptions { unitTests.all { it.useJUnitPlatform() } }
}
dependencies {
    testImplementation(libs.bundles.testing.unit)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testRuntimeOnly(libs.junit.platform.launcher)
    testRuntimeOnly(libs.junit.vintage.engine) // Required for ComposeTestRule (JUnit 4) under useJUnitPlatform()
}
```

## Reusable Main Coroutine Extension
`app/src/test/java/<base_package>/core/test/MainCoroutineExtension.kt`:
```kotlin
@OptIn(ExperimentalCoroutinesApi::class)
class MainCoroutineExtension(
    val testDispatcher: TestDispatcher = UnconfinedTestDispatcher()
) : BeforeEachCallback, AfterEachCallback {
    override fun beforeEach(context: ExtensionContext?) { Dispatchers.setMain(testDispatcher) }
    override fun afterEach(context: ExtensionContext?) { Dispatchers.resetMain() }
}
```

## ViewModel Unit Tests

### Testing State and Events with Turbine
```kotlin
@ExtendWith(MainCoroutineExtension::class)
class NoteListViewModelTest {
    private lateinit var viewModel: NoteListViewModel
    private val repository = FakeNoteRepository()

    @BeforeEach
    fun setUp() {
        viewModel = NoteListViewModel(repository)
    }

    @Test
    fun `loading notes emits notes in state`() = runTest {
        viewModel.state.test {
            viewModel.onAction(NoteListAction.OnRefreshClick)
            assertThat(awaitItem().isLoading).isTrue()
            assertThat(awaitItem().notes).isNotEmpty()
        }
    }

    @Test
    fun `clicking note sends NavigateToDetail event`() = runTest {
        viewModel.events.test {
            viewModel.onAction(NoteListAction.OnNoteClick("123"))
            assertThat(awaitItem()).isEqualTo(NoteListEvent.NavigateToDetail("123"))
        }
    }
}
```

## Fake Repositories
Prefer **fakes** (in-memory implementations) over mocks for repository dependencies:
```kotlin
class FakeNoteRepository : NoteRepository {
    private val notes = mutableListOf<Note>()
    var shouldReturnError = false

    override suspend fun getNotes() = if (shouldReturnError) Result.Error(DataError.Local.UNKNOWN) else Result.Success(notes.toList())
    
    override suspend fun insertNote(note: Note): EmptyResult<DataError.Local> {
        notes.add(note)
        return Result.Success(Unit)
    }
}
```

## SavedStateHandle in Tests
Instantiate directly with a map — no mocking required:
```kotlin
val savedStateHandle = SavedStateHandle(mapOf("noteId" to "123"))
val viewModel = NoteEditorViewModel(savedStateHandle, FakeNoteRepository())
```

## When to Inject Dispatchers
- Only inject `CoroutineDispatcher` into a class when:
  1. It dispatches to a non-main dispatcher (e.g., `Dispatchers.IO`), AND
  2. That class is directly unit-tested.
- ViewModels that only use `viewModelScope` do not need injected dispatchers. Use `MainCoroutineExtension` in tests instead.

If a non-ViewModel class uses `withContext(Dispatchers.IO)` and is unit-tested, inject the dispatcher:
```kotlin
class ImageCompressor(private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO) {
    suspend fun compress(bytes: ByteArray): ByteArray = withContext(ioDispatcher) { /* ... */ }
}

// In test:
val compressor = ImageCompressor(ioDispatcher = UnconfinedTestDispatcher())
```

## What to Test
- Unit-test every ViewModel and any non-trivial domain/data logic.
- Unit-test any logic that is likely to change.
- Use fakes over mocks where possible — fakes are simpler and catch more real bugs.
- Write integration tests where database/network interactions are non-trivial.
- Write E2E Compose tests for critical user flows.
- Use the robot pattern for complex UI/E2E tests with multiple test cases or shared interaction sequences.

## Compose UI Tests with Robot Pattern
For complex end-to-end or multi-step UI tests, use the **Robot Pattern** to separate test intent from Compose interactions. A robot encapsulates all `composeTestRule` interactions for a screen, keeping tests readable and DRY.

### Structure
Every robot function returns `this` so calls can be chained like a builder:
```kotlin
// Robot class — owns all UI interactions for the screen
class NoteListRobot(private val composeTestRule: ComposeContentTestRule) {

    fun setContent(
        state: NoteListState,
        onAction: (NoteListAction) -> Unit = {}
    ) = apply {
        composeTestRule.setContent {
            NoteListScreen(state = state, onAction = onAction)
        }
    }

    fun assertNoteVisible(title: String) = apply {
        composeTestRule.onNodeWithText(title).assertIsDisplayed()
    }

    fun clickNote(title: String) = apply {
        composeTestRule.onNodeWithText(title).performClick()
    }

    fun assertEmptyState() = apply {
        composeTestRule.onNodeWithTag("empty_state").assertIsDisplayed()
    }
}
```

### Usage in Tests
```kotlin
class NoteListScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val robot by lazy { NoteListRobot(composeTestRule) }

    @Test
    fun displaysNotes_afterLoad() {
        robot
            .setContent(NoteListState(notes = listOf(NoteUi("1", "Hello", "Mar 15"))))
            .assertNoteVisible("Hello")
    }

    @Test
    fun showsEmptyState_whenNoNotes() {
        robot
            .setContent(NoteListState(notes = emptyList()))
            .assertEmptyState()
    }

    @Test
    fun clickingNote_triggersAction() {
        var clickedId: String? = null
        robot
            .setContent(
                state = NoteListState(notes = listOf(NoteUi("1", "Hello", "Mar 15"))),
                onAction = { if (it is NoteListAction.OnNoteClick) clickedId = it.noteId }
            )
            .assertNoteVisible("Hello")
            .clickNote("Hello")

        assertThat(clickedId).isEqualTo("1")
    }
}
```
