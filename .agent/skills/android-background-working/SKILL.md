---
name: android-background-working
description: "Standards and templates for Android background processing using WorkManager and AlarmManager, featuring Clean Architecture abstraction, Koin Worker constructor injection, Android 12+ exact alarm permissions, and testing patterns. Trigger on phrases like 'background task', 'WorkManager', 'Worker', 'periodic sync', 'schedule a job', 'AlarmManager', 'exact alarm', 'reminder', or 'foreground service'."
---

# Android Background Working (WorkManager & AlarmManager)

## 1. Core Principles & Decision Matrix

When scheduling background work in Android, always choose the right tool based on **timing precision** and **execution guarantees**:

| Requirement | Recommended Tool | Execution Guarantee | Doze Mode Behavior |
| :--- | :--- | :--- | :--- |
| **Exact time execution** (e.g. Alarm clock, calendar reminder, precise timed trigger) | **AlarmManager** | Fired at exact wall-clock time (`RTC_WAKEUP`) | Wakes CPU temporarily via `setExactAndAllowWhileIdle()` or `setAlarmClock()` |
| **Deferrable & guaranteed execution** (e.g. Data sync, image upload, database cleanup) | **WorkManager** | Guaranteed to run even across app restarts or device reboots | Batched with system maintenance windows |
| **Exact time trigger + Heavy/Long task** | **AlarmManager + WorkManager** | Alarm triggers `BroadcastReceiver`, which enqueues an **Expedited / OneTime** WorkRequest | Alarm wakes device briefly; WorkManager safely manages execution |
| **Active user-perceived task** (e.g. Navigation, media playback) | **Foreground Service** | Continues while user leaves app, visible ongoing notification | Exempt from Doze restrictions |

### Golden Rules
1. **Never use `Thread.sleep()` or Coroutine `delay()` for long-term background scheduling** in background services or ViewModels.
2. **WorkManager Periodic minimum interval is 15 minutes** (with minimum 5 minutes flex interval). For intervals shorter or needing exact minute accuracy, use AlarmManager.
3. **Keep Domain pure Kotlin**: Domain layer contains `AppWorkManager` and `AppAlarmManager` interfaces and pure models (e.g. `WorkStatus`, `kotlin.time.Duration`). No Android SDK classes (e.g. `Context`, `WorkInfo`, `Constraints`) should leak into Domain.
4. **Use Koin Constructor Injection for Workers**: Inject dependencies directly via constructor using `koin-androidx-workmanager` and `workerOf`, initialized through `workManagerFactory()` in `Application`.
5. **Always check exact alarm permission on Android 12+ (API 31+)** before setting exact alarms.

---

## 2. Directory Structure

```text
app/src/main/java/com/<app>/
├── core/
│   ├── domain/
│   │   ├── model/
│   │   │   ├── WorkStatus.kt
│   │   │   └── WorkSchedule.kt
│   │   └── repository/
│   │       ├── AppWorkManager.kt
│   │       └── AppAlarmManager.kt
│   ├── data/
│   │   └── repository/
│   │       ├── AndroidWorkManager.kt
│   │       └── AndroidAlarmManager.kt
│   ├── work/
│   │   └── SyncDataWorker.kt
│   ├── receiver/
│   │   └── ExactAlarmReceiver.kt
│   └── di/
│       └── CoreModule.kt        # 全域通用/基礎設施放置於 CoreModule；若為 Feature 專屬則放於該 Feature Module
```

---

## 3. Gradle Configuration

```toml
# gradle/libs.versions.toml
[versions]
work = "2.10.0"
koin = "4.0.2"

[libraries]
# WorkManager
androidx-work-runtime-ktx = { group = "androidx.work", name = "work-runtime-ktx", version.ref = "work" }
androidx-work-testing = { group = "androidx.work", name = "work-testing", version.ref = "work" }

# Koin WorkManager Integration
koin-androidx-workmanager = { group = "io.insert-koin", name = "koin-androidx-workmanager", version.ref = "koin" }
```

```kotlin
// app/build.gradle.kts
dependencies {
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.koin.androidx.workmanager)
    
    testImplementation(libs.androidx.work.testing)
}
```

---

## 4. WorkManager with Koin Constructor Injection

### 4.1. Worker Implementation (Constructor Injection)

```kotlin
package com.example.app.core.work

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.example.app.core.domain.repository.DataRepository
import kotlinx.coroutines.CancellationException

class SyncDataWorker(
    appContext: Context,
    workerParams: WorkerParameters,
    private val dataRepository: DataRepository // Injected by Koin!
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            // Check cancellation cooperatively
            if (isStopped) return Result.failure()

            val success = dataRepository.sync()
            if (success) {
                Result.success()
            } else {
                Result.retry()
            }
        } catch (e: CancellationException) {
            // Rethrow CancellationException for coroutines
            throw e
        } catch (e: Exception) {
            if (runAttemptCount < 3) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }

    // Required for Expedited WorkRequests on Android 11 (API 30) and lower
    override suspend fun getForegroundInfo(): ForegroundInfo {
        val channelId = "sync_channel"
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Data Sync", NotificationManager.IMPORTANCE_LOW)
            notificationManager.createNotificationChannel(channel)
        }
        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setContentTitle("Syncing data")
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setOngoing(true)
            .build()
        return ForegroundInfo(1001, notification)
    }
}
```

### 4.2. Koin Module & Application Setup

> [!TIP]
> **模組放置原則**：
> - **涵蓋整個專案的通用/底層排程元件**：直接在 `CoreModule.kt` 註冊（例如 `AppWorkManager`、`AppAlarmManager`）。
> - **特定業務功能專屬的 Worker**：直接在該業務模組的 `<Feature>Module.kt` 註冊，不隨意建立零碎的 `WorkModule` / `AlarmModule`。

```kotlin
package com.example.app.core.di

import com.example.app.core.data.repository.AndroidAlarmManager
import com.example.app.core.data.repository.AndroidWorkManager
import com.example.app.core.domain.repository.AppAlarmManager
import com.example.app.core.domain.repository.AppWorkManager
import com.example.app.core.work.SyncDataWorker
import org.koin.androidx.workmanager.dsl.workerOf
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

// 若為全域/核心基礎設施，直接置於 coreModule
val coreModule = module {
    // 註冊 Worker（若為特定 Feature 專屬，則放置於該 Feature 的 Module）
    workerOf(::SyncDataWorker)
    
    singleOf(::AndroidWorkManager) { bind<AppWorkManager>() }
    singleOf(::AndroidAlarmManager) { bind<AppAlarmManager>() }
}
```

```kotlin
package com.example.app

import android.app.Application
import com.example.app.core.di.coreModule
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.workmanager.koin.workManagerFactory
import org.koin.core.context.startKoin

class App : Application() {

    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@App)
            workManagerFactory() // Integrates Koin WorkerFactory into WorkManager
            modules(coreModule, /* feature modules */)
        }
    }
}
```

> [!IMPORTANT]
> If implementing `Configuration.Provider`, remember to remove the default `WorkManagerInitializer` from `AndroidManifest.xml` to prevent automatic initialization before Koin starts:
> ```xml
> <provider
>     android:name="androidx.startup.InitializationProvider"
>     android:authorities="${applicationId}.androidx-startup"
>     tools:node="merge">
>     <meta-data
>         android:name="androidx.work.WorkManagerInitializer"
>         android:value="androidx.startup"
>         tools:node="remove" />
> </provider>
> ```

---

## 5. WorkManager Advanced Execution Strategies

### 5.1. Constraints & Unique Work
- **Constraints**: Set requirements such as unmetered network (Wi-Fi), battery not low, or device charging.
- **Unique Work**:
  - `ExistingWorkPolicy.KEEP`: If a task is pending or running, keep it and ignore the new request.
  - `ExistingWorkPolicy.REPLACE`: Cancel the existing task and queue the new one.
  - `ExistingPeriodicWorkPolicy.UPDATE` (WorkManager 2.8+): Update the existing periodic work configuration without resetting the execution timer.

```kotlin
val constraints = Constraints.Builder()
    .setRequiredNetworkType(NetworkType.CONNECTED)
    .setRequiresBatteryNotLow(true)
    .build()

val syncRequest = PeriodicWorkRequestBuilder<SyncDataWorker>(
    repeatInterval = 1, 
    repeatIntervalTimeUnit = TimeUnit.HOURS
)
    .setConstraints(constraints)
    .setBackoffCriteria(
        BackoffPolicy.EXPONENTIAL,
        WorkRequest.MIN_BACKOFF_MILLIS,
        TimeUnit.MILLISECONDS
    )
    .build()

workManager.enqueueUniquePeriodicWork(
    "sync_data_work",
    ExistingPeriodicWorkPolicy.UPDATE,
    syncRequest
)
```

### 5.2. Cooperative Cancellation & Retries
- In `CoroutineWorker`, long operations must handle coroutine cancellation gracefully. Check `isStopped` or periodically check coroutine active state (`yield()`, `ensureActive()`).
- Use `runAttemptCount` to guard against infinite retry loops on permanent failures.

---

## 6. AlarmManager & Modern Android (API 31+ / 33+ / 34+)

### 6.1. Permissions
1. **Android 12 (API 31)+ `SCHEDULE_EXACT_ALARM`**:
   - Needs declaration in `AndroidManifest.xml`.
   - User can revoke this in System Settings. App **must** check `alarmManager.canScheduleExactAlarms()` before calling exact alarm APIs.
2. **Android 13 (API 33)+ `USE_EXACT_ALARM`**:
   - Granted automatically at install time.
   - **Restricted by Google Play Store policy**: Only allowed for alarm clocks, timers, or calendar reminder apps. General apps must use `SCHEDULE_EXACT_ALARM`.

```xml
<!-- AndroidManifest.xml -->
<uses-permission android:name="android.permission.SCHEDULE_EXACT_ALARM" />
<!-- Use USE_EXACT_ALARM only if building clock/timer/calendar app:
<uses-permission android:name="android.permission.USE_EXACT_ALARM" />
-->

<receiver
    android:name=".core.receiver.ExactAlarmReceiver"
    android:exported="false" />
```

### 6.2. Checking & Requesting Exact Alarm Permission

```kotlin
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
    if (!alarmManager.canScheduleExactAlarms()) {
        // Guide user to System Settings
        val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
            data = Uri.parse("package:${context.packageName}")
        }
        context.startActivity(intent)
        return
    }
}
```

### 6.3. Alarm Receiver to WorkManager Pattern

When exact alarm fires, the `BroadcastReceiver` executes on the main thread and has a strict timeout (~10-15 seconds). For heavy operations, immediately dispatch an **Expedited** or **OneTime** WorkRequest.

```kotlin
package com.example.app.core.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import com.example.app.core.work.SyncDataWorker

class ExactAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val workRequest = OneTimeWorkRequestBuilder<SyncDataWorker>()
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            "alarm_triggered_sync",
            ExistingWorkPolicy.REPLACE,
            workRequest
        )
    }
}
```

---

## 7. Clean Architecture Code Templates

### 7.1. Domain Models & Interfaces

```kotlin
// core/domain/model/WorkStatus.kt
package com.example.app.core.domain.model

enum class WorkStatus {
    ENQUEUED,
    RUNNING,
    SUCCEEDED,
    FAILED,
    CANCELLED
}
```

```kotlin
// core/domain/repository/AppWorkManager.kt
package com.example.app.core.domain.repository

import com.example.app.core.domain.model.WorkStatus
import kotlinx.coroutines.flow.Flow
import kotlin.time.Duration

interface AppWorkManager {
    fun enqueueOneTimeSync()
    fun schedulePeriodicSync(interval: Duration)
    fun cancelSync()
    fun observeSyncStatus(): Flow<WorkStatus?>
}
```

```kotlin
// core/domain/repository/AppAlarmManager.kt
package com.example.app.core.domain.repository

interface AppAlarmManager {
    fun canScheduleExactAlarms(): Boolean
    fun scheduleExactAlarm(triggerAtMillis: Long)
    fun cancelAlarm()
}
```

### 7.2. Data Layer Implementations

```kotlin
// core/data/repository/AndroidWorkManager.kt
package com.example.app.core.data.repository

import android.content.Context
import androidx.work.*
import com.example.app.core.domain.model.WorkStatus
import com.example.app.core.domain.repository.AppWorkManager
import com.example.app.core.work.SyncDataWorker
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.concurrent.TimeUnit
import kotlin.time.Duration
import kotlin.time.toJavaDuration

class AndroidWorkManager(
    private val context: Context
) : AppWorkManager {

    companion object {
        private const val SYNC_WORK_NAME = "sync_data_work"
    }

    private val workManager by lazy { WorkManager.getInstance(context) }

    override fun enqueueOneTimeSync() {
        val request = OneTimeWorkRequestBuilder<SyncDataWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()

        workManager.enqueueUniqueWork(
            SYNC_WORK_NAME,
            ExistingWorkPolicy.KEEP,
            request
        )
    }

    override fun schedulePeriodicSync(interval: Duration) {
        val javaDuration = interval.toJavaDuration()
        val request = PeriodicWorkRequestBuilder<SyncDataWorker>(javaDuration)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()

        workManager.enqueueUniquePeriodicWork(
            SYNC_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    override fun cancelSync() {
        workManager.cancelUniqueWork(SYNC_WORK_NAME)
    }

    override fun observeSyncStatus(): Flow<WorkStatus?> {
        return workManager.getWorkInfosForUniqueWorkFlow(SYNC_WORK_NAME).map { list ->
            val state = list.firstOrNull()?.state ?: return@map null
            when (state) {
                WorkInfo.State.ENQUEUED, WorkInfo.State.BLOCKED -> WorkStatus.ENQUEUED
                WorkInfo.State.RUNNING -> WorkStatus.RUNNING
                WorkInfo.State.SUCCEEDED -> WorkStatus.SUCCEEDED
                WorkInfo.State.FAILED -> WorkStatus.FAILED
                WorkInfo.State.CANCELLED -> WorkStatus.CANCELLED
            }
        }
    }
}
```

```kotlin
// core/data/repository/AndroidAlarmManager.kt
package com.example.app.core.data.repository

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.app.core.domain.repository.AppAlarmManager
import com.example.app.core.receiver.ExactAlarmReceiver

class AndroidAlarmManager(
    private val context: Context
) : AppAlarmManager {

    private val alarmManager by lazy {
        context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    }

    companion object {
        private const val ALARM_REQUEST_CODE = 2001
    }

    override fun canScheduleExactAlarms(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }
    }

    override fun scheduleExactAlarm(triggerAtMillis: Long) {
        if (!canScheduleExactAlarms()) return

        val intent = Intent(context, ExactAlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            ALARM_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerAtMillis,
            pendingIntent
        )
    }

    override fun cancelAlarm() {
        val intent = Intent(context, ExactAlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            ALARM_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        pendingIntent?.let { alarmManager.cancel(it) }
    }
}
```

---

## 8. Testing Strategies

### 8.1. Unit Testing ViewModels (Fake Managers)

```kotlin
class FakeAppWorkManager : AppWorkManager {
    private val _status = MutableStateFlow<WorkStatus?>(null)
    var enqueueCalled = false
    var cancelCalled = false

    override fun enqueueOneTimeSync() { enqueueCalled = true }
    override fun schedulePeriodicSync(interval: Duration) { enqueueCalled = true }
    override fun cancelSync() { cancelCalled = true }
    override fun observeSyncStatus(): Flow<WorkStatus?> = _status.asStateFlow()

    fun emitStatus(status: WorkStatus?) { _status.value = status }
}
```

### 8.2. Integration Testing Workers (`work-testing`)

```kotlin
@Test
fun testSyncDataWorker_success() = runTest {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val fakeRepo = FakeDataRepository(shouldSucceed = true)

    val worker = TestListenableWorkerBuilder<SyncDataWorker>(context)
        .setWorkerFactory(object : WorkerFactory() {
            override fun createWorker(
                appContext: Context,
                workerClassName: String,
                workerParameters: WorkerParameters
            ): ListenableWorker {
                return SyncDataWorker(appContext, workerParameters, fakeRepo)
            }
        })
        .build()

    val result = worker.doWork()
    assertThat(result).isEqualTo(ListenableWorker.Result.success())
}
```
