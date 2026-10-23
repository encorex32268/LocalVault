---
name: android-module-structure
description: "Module layout, dependency rules, Gradle configuration, and desugaring for Android/KMP projects — single-module feature folders, multi-module architecture, convention plugins, version catalogs, and Java 8+ API desugaring. Use this skill whenever setting up a new project, deciding module structure, creating feature modules, configuring Gradle plugins, working with version catalogs, enabling desugaring, or making project architecture decisions. Trigger on phrases like 'set up the project', 'add a module', 'project structure', 'convention plugin', 'build-logic', 'where does X live', 'desugaring', 'java.time', 'desugar_jdk_libs', or 'version catalog'."
---

# Android / KMP Project Architecture

## 1. Single-Module Project Architecture (Primary)

In a single-module setup (e.g., just an `:app` module), the project is modularized using a **feature-folder** structure. Each feature contains its own architectural layers.

### Feature Folder Structure

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

### Folder-Level Dependency Conventions
- **Clean Architecture layers**: `presentation` → `domain` ← `data`. Domain is innermost and depends on nothing within the feature.
- **Cross-feature communication**: Features **never** directly depend on other features' presentation or data layers. If features need to communicate or share models, the shared logic should be extracted to a common `core/` or `shared/` package at the root level.

### Code Placement Rules
- **Feature-Specific**: Code lives inside its respective `<feature_name>/` folder if it is only used by that feature.
- **Core / Shared**: Code that is used by more than one feature (e.g., generic UI components, network clients, base domain models, shared database setup) lives in a top-level `core/` or `shared/` folder (e.g., `core/network`, `core/design_system`, `core/domain`).

---

## 2. Multi-Module Project Architecture (Reference)

For larger scale applications, the codebase is split into physical Gradle modules.

### Module Tree
```
:app
:build-logic                    ← Gradle convention plugins
:core:domain                    ← Shared domain models, repository interfaces, error types, Result
:core:data                      ← Shared data logic, Ktor HttpClient factory, shared DB schemas/DAOs
:core:presentation              ← Shared UI utilities (ObserveAsEvents, UiText, etc.)
:core:design-system             ← Reusable Compose components, colors, theme, typography
:feature:<name>:domain          ← Feature-specific domain models, repo interfaces, error types
:feature:<name>:data            ← Repo implementations, DTOs, mappers, Room DAOs
:feature:<name>:presentation    ← ViewModel, screen composables, state, actions, events
```

### Layer Dependency Rules Matrix
| Layer | May depend on |
|---|---|
| `presentation` | `domain` (own feature), `core:domain`, `core:presentation`, `core:design-system` |
| `data` | `domain` (own feature), `core:domain`, `core:data` |
| `domain` | `core:domain` only — never `data` or `presentation` |
| `:app` | everything (wires all modules) |

**Every** layer and module may access `core:domain`.

### Convention Plugins (`:build-logic`)
Define a convention plugin for every non-trivial Gradle config:

| Plugin | Purpose |
|---|---|
| `android-application` | App module config (applicationId, versionCode, etc.) |
| `android-library` | Base Android library config |
| `android-feature` | Android library + Compose + Koin + shared feature deps bundled |
| `domain-module` | Pure Kotlin/KMP module, no Android deps |
| `compose` | Compose compiler + BOM |
| `koin` | Koin dependency block |
| `ktor` | Ktor client + serialization |
| `room` | Room + KSP config |
| `kotlinx-serialization` | KotlinX Serialization plugin + dep |

### Version Catalog
Use **version catalogs** (`gradle/libs.versions.toml`) for all dependency and version management. No hardcoded versions in build files.

---

## 3. Gradle Base Configuration

### Java 8+ Desugaring
Configures Android Java 8+ API desugaring to safely use modern Java APIs (like `java.time.*`, streams) without runtime `NoClassDefFoundError` on versions below API 26.

#### Setup Script
```bash
python3 .agents/skills/android-module-structure/scripts/setup_desugaring.py
```

#### `desugar_jdk_libs` Setup
Add to `gradle/libs.versions.toml`:
```toml
[versions]
desugarJdkLibs = "2.1.5"

[libraries]
desugar-jdk-libs = { group = "com.android.tools", name = "desugar_jdk_libs", version.ref = "desugarJdkLibs" }
```

In your module's `build.gradle.kts`:
```kotlin
android {
    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    coreLibraryDesugaring(libs.desugar.jdk.libs)
}
```

#### `java.time.*` Unlocking
Enabling core library desugaring unlocks:
- **`java.time.*`**: `Instant`, `LocalDate`, `LocalDateTime`, `ZonedDateTime`, `ZoneId`, `DateTimeFormatter`, `Duration`, `Period`
- **`java.util.stream.*`**: Streams, collectors, operations
- **`java.util.function.*`**: `Consumer`, `Function`, `Predicate`, `Supplier`, etc.
- **`java.util.concurrent.ConcurrentHashMap`**: Newer Java 8 concurrent operations
- **`java.util.Optional`**, `OptionalInt`, `OptionalDouble`, `OptionalLong`

#### Multi-Module Desugaring Notes
- **Library Modules** (`:core:*`, `:feature:*`): If a library module directly references `java.time.*` or other desugared APIs, it must enable `isCoreLibraryDesugaringEnabled = true` and include the `coreLibraryDesugaring` dependency.
- **Application Module** (`:app`): Even if `:app` does not directly use `java.time.*`, it **MUST** declare `coreLibraryDesugaring(libs.desugar.jdk.libs)` if any transitively included library module uses desugared APIs. The packaging step in `:app` strips and embeds the runtime desugar bytecode.

---

## 4. Recommended Technology Stack

| Concern | Library |
|---|---|
| DI | Koin |
| Networking | Ktor Client |
| Local DB | Room |
| Preferences | DataStore |
| Navigation | Compose Navigation (type-safe) |
| Serialization | KotlinX Serialization (Ktor + Nav routes) |
| Image loading | Coil |
| Logging | Kermit |
| Async | Coroutines + Flow |
| Background tasks | WorkManager |
| Secrets | `local.properties` + `BuildConfig` (Android); `BuildKonfig` (KMP) |
| Testing | JUnit5, Turbine, AssertK, `kotlinx-coroutines-test` |
| UI testing | `ComposeTestRule` |
