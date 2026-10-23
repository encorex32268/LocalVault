---
name: android-data-layer
description: "Data layer patterns for Android/KMP — Ktor HTTP client, Room database, DataStore, DTOs, mappers, repositories, safeCall helpers, token storage, NetworkMonitor, and offline-first. Use this skill whenever writing or reviewing a data source, repository, DAO, DTO, Room entity, Ktor HttpClient, DataStore, network call, or implementing offline-first sync. Trigger on phrases like 'create a repository', 'create a data source', 'add a DAO', 'Ktor client', 'write a mapper', 'DTO', 'Room entity', 'network call', 'token storage', 'offline-first', 'DataStore', 'preferences', 'HttpClient', 'safeCall', or 'NetworkMonitor'."
---

# Android / KMP Data Layer Setup

This skill consolidates patterns for setting up a robust, offline-first data layer in Android/KMP, covering Network (Ktor), Local DB (Room), and Preferences (DataStore).

## Setup Scripts

```bash
# Set up Room Database
python3 .agents/skills/android-data-layer/scripts/setup_room.py

# Set up DataStore
python3 .agents/skills/android-data-layer/scripts/setup_datastore.py

# Set up Networking (Ktor HTTP client + safeCall + DataError)
python3 .agents/skills/android-data-layer/scripts/setup_networking.py

# Include connectivity monitoring (NetworkMonitor + ACCESS_NETWORK_STATE permission)
python3 .agents/skills/android-data-layer/scripts/setup_networking.py --with-monitor
```

## 1. Architecture Overview

### Error Handling
This skill uses `Result<T, E>`, `DataError`, and the extension helpers defined in the **android-error-handling** skill. Refer to that skill for the full `Result` wrapper, `DataError` sealed interface, and `map`/`onSuccess`/`onFailure`/`asEmptyResult` extensions.

### Data Source vs Repository

- **Data source** — accesses a single data source (local DB, remote API, file system). Most classes in the data layer are data sources.
- **Repository** — combines multiple data sources (e.g., a remote API + a local DB for offline-first). Only use the term "repository" when the class genuinely coordinates multiple sources.

```kotlin
// Single source → data source
interface NoteLocalDataSource {
    suspend fun getNotes(): Result<List<Note>, DataError.Local>
    suspend fun insertNote(note: Note): EmptyResult<DataError.Local>
}

interface NoteRemoteDataSource {
    suspend fun fetchNotes(): Result<List<Note>, DataError.Network>
}

// Multiple sources → repository
interface NoteRepository {
    suspend fun getNotes(): Result<List<Note>, DataError>
    suspend fun sync(): EmptyResult<DataError>
}
```

### Domain Layer Contracts

- Pure Kotlin — no Android/framework imports.
- Contains: domain models, data source/repository **interfaces**, error types.
- **Every data source or repository used by a ViewModel must have an interface in `domain`** — enforces that `presentation` never depends on `data`, and enables testing.

### DTOs and Domain Models

- Always separate: DTOs (data layer) ↔ Domain Models (domain layer).
- Domain models never go directly into Room entities or Ktor request/response bodies.
- Mappers are simple extension functions living in the data layer alongside the DTO:

```kotlin
fun NoteDto.toNote(): Note = Note(id = id, title = title)
fun Note.toNoteDto(): NoteDto = NoteDto(id = id, title = title)
fun NoteEntity.toNote(): Note = Note(id = id, title = title)
fun Note.toNoteEntity(): NoteEntity = NoteEntity(id = id, title = title)
```

### Naming Conventions & Implementations

Name implementations for what makes them unique — never suffix with `Impl`.

| Thing | Convention | Example |
|---|---|---|
| Data source interface | `<Entity><Local/Remote>DataSource` | `NoteLocalDataSource`, `NoteRemoteDataSource` |
| Data source impl | describe what makes it unique | `RoomNoteDataSource`, `KtorNoteDataSource` |
| Repository interface | `<Entity>Repository` (multi-source only) | `NoteRepository` |
| Repository impl | describe what makes it unique | `OfflineFirstNoteRepository` |
| DTO | `<Model>Dto` | `NoteDto` |
| Room entity | `<Model>Entity` | `NoteEntity` |
| Mapper | extension fun on source type | `fun NoteDto.toNote()` |

## 2. Network Layer — Ktor HTTP Client

### HttpClient Factory (`core:data`)

Configure the client once. Accept the engine externally so tests can swap in a mock engine:

```kotlin
object HttpClientFactory {
    fun create(engine: HttpClientEngine): HttpClient = HttpClient(engine) {
        install(ContentNegotiation) { json() }
        install(Auth) {
            bearer {
                loadTokens { /* load from DataStore */ }
                refreshTokens { /* call refresh endpoint, save new tokens */ }
            }
        }
        install(Logging) { 
            logger = Logger.DEFAULT
            level = if (BuildConfig.DEBUG) LogLevel.ALL else LogLevel.NONE
        }
        defaultRequest { contentType(ContentType.Application.Json) }
    }
}
```

Inject `HttpClient` via Koin. For KMP, use the platform default engine.

### Token Storage

Store tokens in DataStore (in `core:data` or a dedicated `:core:auth` / `:feature:auth:data` module). The Ktor `Auth` plugin reads/writes tokens and handles 401 refresh automatically.

### Safe Call Helpers

Use `safeCall` / `responseToResult` helpers and typed extension functions (`HttpClient.get`, `HttpClient.post`, `HttpClient.delete`) to keep data source call sites clean and uniform.

```kotlin
class KtorNoteDataSource(private val httpClient: HttpClient) : NoteRemoteDataSource {
    override suspend fun fetchNotes(): Result<List<Note>, DataError.Network> {
        val result: Result<List<NoteDto>, DataError.Network> = httpClient.get(route = "/notes")
        return result.map { dtos -> dtos.map { it.toNote() } }
    }
}
```

### Connectivity Monitoring (--with-monitor)

Provides real-time online/offline detection using Android's `ConnectivityManager.NetworkCallback`.
- Requires `android.permission.ACCESS_NETWORK_STATE` in `AndroidManifest.xml`.
- Provides `NetworkMonitor` interface and `ConnectivityManagerNetworkMonitor` implementation in `core/data/util/`.
- Exposes `isOnline: Flow<Boolean>`.

```kotlin
class HomeViewModel(private val networkMonitor: NetworkMonitor) : ViewModel() {
    val isOnline = networkMonitor.isOnline.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = true
    )
}
```

## 3. Local Database — Room

### Gradle Configuration

```toml
# libs.versions.toml
[versions]
room = "2.8.4"
ksp = "2.2.10-2.0.2"

[libraries]
room-runtime = { group = "androidx.room", name = "room-runtime", version.ref = "room" }
room-ktx = { group = "androidx.room", name = "room-ktx", version.ref = "room" }
room-compiler = { group = "androidx.room", name = "room-compiler", version.ref = "room" }

[plugins]
ksp = { id = "com.google.devtools.ksp", version.ref = "ksp" }
```

```kotlin
// app/build.gradle.kts
plugins { alias(libs.plugins.ksp) }
dependencies {
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)
}
```

### Entity & DAO Patterns

```kotlin
@Entity(tableName = "items")
data class ItemEntity(@PrimaryKey val id: String, val title: String, val timestamp: Long)

@Dao
interface ItemDao {
    @Query("SELECT * FROM items ORDER BY timestamp DESC")
    fun observeAllItems(): Flow<List<ItemEntity>>
    
    @Query("SELECT * FROM items WHERE id = :id LIMIT 1")
    suspend fun getItemById(id: String): ItemEntity?
    
    @Upsert
    suspend fun upsertItem(item: ItemEntity)
    
    @Upsert
    suspend fun upsertAll(items: List<ItemEntity>)
}
```

### Database, Factory, and Migrations

When using `@Database(autoMigrations = [...])`, Room requires `exportSchema = true` (and configuring schema directory in Gradle). Avoid `fallbackToDestructiveMigration()` in production to prevent data loss upon schema upgrades.

```kotlin
@Database(
    entities = [ItemEntity::class],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract val itemDao: ItemDao
}

object DatabaseFactory {
    fun create(context: Context): AppDatabase = Room.databaseBuilder(
        context.applicationContext, AppDatabase::class.java, "app.db"
    ).build()
}
```

## 4. Key-Value Storage — DataStore

### Domain Contract
`AppDataStore` is a pure interface in `core/domain/datastore/`. Domain/presentation never import DataStore.

```kotlin
interface AppDataStore {
    val isFirstTime: Flow<Boolean>
    suspend fun setIsFirstTime(isFirstTime: Boolean)
}
```

### Implementation

Resilient data reading handles corrupted files by catching `IOException`.

```kotlin
val Context.appDataStore: DataStore<Preferences> by preferencesDataStore(name = "app_preferences")

class DefaultAppDataStore(private val dataStore: DataStore<Preferences>) : AppDataStore {
    private companion object { val KEY_IS_FIRST_TIME = booleanPreferencesKey("is_first_time") }

    override val isFirstTime: Flow<Boolean> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[KEY_IS_FIRST_TIME] ?: true }

    override suspend fun setIsFirstTime(isFirstTime: Boolean) {
        dataStore.edit { it[KEY_IS_FIRST_TIME] = isFirstTime }
    }
}
```

### Koin DI & Testing with Fake

```kotlin
val coreModule = module {
    single { androidContext().appDataStore }
    singleOf(::DefaultAppDataStore) { bind<AppDataStore>() }
}

class FakeAppDataStore(initialIsFirstTime: Boolean = true) : AppDataStore {
    private val _isFirstTime = MutableStateFlow(initialIsFirstTime)
    override val isFirstTime: Flow<Boolean> = _isFirstTime.asStateFlow()

    override suspend fun setIsFirstTime(isFirstTime: Boolean) {
        _isFirstTime.value = isFirstTime
    }
}
```

## 5. Offline-First Integration Pattern

Follow **Room as Single Source of Truth**: fetch from network → persist to Room (`dao.upsertAll()`) → expose DB `Flow` to the ViewModel. The ViewModel never observes network responses directly.

Repositories expose reads as `Flow<List<T>>` directly from Room.

```kotlin
class OfflineFirstItemRepository(
    private val remoteDataSource: ItemRemoteDataSource,
    private val localDataSource: ItemLocalDataSource
) : ItemRepository {
    override fun observeItems(): Flow<List<Item>> = localDataSource.observeItems()
    
    override suspend fun syncItems(): Result<Unit, DataError.Network> {
        return remoteDataSource.fetchItems()
            .onSuccess { items -> localDataSource.upsertAll(items) }
            .map { Unit }
    }
}
```

### ViewModel Usage

```kotlin
class ItemListViewModel(private val repository: ItemRepository) : ViewModel() {
    val state = combine(repository.observeItems(), _isLoading, _error) { items, loading, error ->
        ItemListState(items = items, isLoading = loading && items.isEmpty(), error = error)
    }.onStart { repository.syncItems() }.stateIn(...)
}
```

## 6. Checklist

- [ ] Define domain model(s) in `feature:domain`
- [ ] Define data source or repository interface in `feature:domain`
- [ ] Define feature-specific error type(s) in `feature:domain` (implement `Error`) — see **android-error-handling** skill
- [ ] Define DTOs and Room entities in `feature:data`
- [ ] Write mappers as extension functions in `feature:data`
- [ ] Implement data source (single source) or repository (multi-source) in `feature:data`, named for what makes it unique
