---
name: android-di-koin
description: "Koin dependency injection setup for Android/KMP — module definitions per layer, ViewModel injection, assembling modules in Application, koinViewModel() in composables, constructor references, and scoping rules. Use this skill whenever setting up Koin, defining a DI module, providing a repository or ViewModel, injecting a dependency, or wiring modules. Trigger on phrases like 'set up Koin', 'add a Koin module', 'inject a dependency', 'DI module', 'koinViewModel', 'provide a ViewModel', 'startKoin', 'single/viewModel/factory', or 'constructor reference'."
---

# Android / KMP Dependency Injection (Koin)

## Principles
- One Koin module per feature layer / domain — create it only if there are dependencies to provide.
- Assemble modules in `Application` class (`startKoin`).
- In Compose root composables, always inject ViewModels via `koinViewModel()`.
- Single-module projects: Koin modules live in feature's `di/` package and `core/di/CoreModule.kt`.
- Multi-module projects: Modules are assembled in `:app`, never in feature modules themselves.

## Constructor References vs. Lambda Overloads
Prefer constructor-reference overloads (`singleOf`, `viewModelOf`, `factoryOf` with `::ClassName` syntax) — they are more concise and allow Koin to resolve dependencies automatically via reflection/generation without writing boilerplate parameter calls (`get()`).

Only fall back to lambda overloads (`single { ... }`, `viewModel { ... }`, `factory { ... }`) when constructor injection alone is not enough:
- Calling factory methods or builders (e.g., `HttpClientFactory.create(get())`, `Room.databaseBuilder(...)`)
- Passing named/qualified dependencies (e.g., `named("auth")`)
- Performing manual or post-construction setup / configuration

## Module Definitions

### 1. Presentation layer (ViewModel) module
```kotlin
val featureAModule = module {
    viewModelOf(::FeatureAViewModel)
}
```

Multiple ViewModels in a presentation module:
```kotlin
val notesPresentationModule = module {
    viewModelOf(::NoteListViewModel)
    viewModelOf(::NoteDetailViewModel)
}
```

### 2. Data layer module
```kotlin
val notesDataModule = module {
    singleOf(::OfflineFirstNoteRepository) { bind<NoteRepository>() }
}
```

### 3. Core module (System & Utility services)
```kotlin
val coreModule = module {
    // Constructor references when class has an injectable constructor
    singleOf(::ConnectivityManagerNetworkMonitor) { bind<NetworkMonitor>() }
    singleOf(::LogcatAnalyticsHelper) { bind<AnalyticsHelper>() }

    // Lambda overloads when calling factory methods or constructors requiring manual setup
    single { androidContext().appDataStore }
    singleOf(::DefaultAppDataStore) { bind<AppDataStore>() }
    single { HttpClientFactory.create(get()) }
    single { DatabaseFactory.create(androidContext()) }
}
```

## Assembly in `Application`

### Single-Module Project
```kotlin
class SkillsApp : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidLogger()
            androidContext(this@SkillsApp)
            modules(coreModule, featureAModule)
        }
    }
}
```
*Ensure `Application` is declared in `AndroidManifest.xml`.*

### Multi-Module Project Assembly (Reference)
In multi-module architectures, each feature module provides its own DI modules (e.g. `notesDataModule`, `notesPresentationModule`), and they are assembled strictly in the `:app` module's `Application` class:
```kotlin
class App : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidLogger()
            androidContext(this@App)
            modules(
                // core
                coreModule,
                coreDataModule,
                // features
                notesDataModule,
                notesPresentationModule,
                authDataModule,
                authPresentationModule,
                // ...
            )
        }
    }
}
```

## Injecting in Composables
Always inject at the Root level. Never pass ViewModels down the composable tree.
```kotlin
@Composable
fun FeatureARoot(
    viewModel: FeatureAViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    FeatureAScreen(state = state, onAction = viewModel::onAction)
}
```

Passing navigation events to Root composable:
```kotlin
@Composable
fun NoteListRoot(
    onNavigateToDetail: (String) -> Unit,
    viewModel: NoteListViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    NoteListScreen(
        state = state,
        onAction = { action ->
            when (action) {
                is NoteListAction.OnNoteClick -> onNavigateToDetail(action.id)
                else -> Unit
            }
            viewModel.onAction(action)
        }
    )
}
```

## Scoping Rules
| Scope | Preferred form | Fallback form | When to use |
|---|---|---|---|
| Singleton | `singleOf(::Impl) { bind<Interface>() }` | `single { ... }` | One instance for the app lifetime (repositories, HttpClient, DB, preferences) |
| ViewModel | `viewModelOf(::MyViewModel)` | `viewModel { ... }` | ViewModel instances scoped to their lifecycle (Activity/NavBackStackEntry) |
| Factory | `factoryOf(::Impl)` | `factory { ... }` | New instance on every injection (rare — prefer singleton or ViewModel) |

Use the `*Of` constructor-reference form by default. Only use the lambda form when you cannot express the binding with a constructor reference (factory methods, named qualifiers, manual setup).

## Naming Conventions
| Thing | Convention | Example |
|---|---|---|
| Koin module | `<feature><Layer>Module` or `<feature>Module` | `notesDataModule`, `notesPresentationModule`, `coreModule` |
| Location (single-module) | `<feature>/di/` or `core/di/` | `feature_a/di/FeatureAModule.kt`, `core/di/CoreModule.kt` |
| Location (multi-module) | `<feature>:<layer>` module | `feature:notes:data`, `feature:notes:presentation` |

## Checklist: Adding DI for a New Feature

### Single-Module Project:
- [ ] Define `val <feature>Module = module { viewModelOf(::<Feature>ViewModel) }` in `<feature>/di/`
- [ ] Register module in `Application`'s `startKoin { modules(...) }`
- [ ] Use `koinViewModel()` in Root composable

### Multi-Module Project (Reference):
- [ ] Define `val <feature>DataModule = module { ... }` in `feature:<feature>:data`
- [ ] Define `val <feature>PresentationModule = module { ... }` in `feature:<feature>:presentation`
- [ ] Register both modules in `:app`'s `startKoin { modules(...) }`
- [ ] Use `koinViewModel()` in all Root composables
