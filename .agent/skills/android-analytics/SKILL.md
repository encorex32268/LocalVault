---
name: android-analytics
description: "Sets up a decoupled, NiA-inspired Analytics pipeline in core/analytics (AnalyticsEvent, AnalyticsHelper, LogcatAnalyticsHelper, TrackScreenViewEvent). Use this skill whenever adding analytics, tracking screen views or user events, or integrating an analytics SDK such as Firebase Analytics. Trigger on phrases like 'analytics', 'track event', 'log event', 'screen view tracking', 'AnalyticsHelper', or 'Firebase Analytics'."
---

# Android Analytics Setup

## Architecture

```text
app/src/main/java/<base_package>/core/analytics/
├── AnalyticsEvent.kt          
├── AnalyticsHelper.kt         
├── LogcatAnalyticsHelper.kt   
└── TrackScreenViewEvent.kt    
```

- **SDK Decoupling**: Screens/ViewModels depend on `AnalyticsHelper`, not SDKs.
- **Crash-free**: Uses `LogcatAnalyticsHelper` by default.
- **Compose Tracking**: `TrackScreenViewEvent("screen")` tracks impression once per composition.
- **Propagation**: `LocalAnalyticsHelper` provides helper down Compose tree.

## Setup Script

```bash
python3 .agents/skills/android-analytics/scripts/setup_analytics.py
```

## Usage Examples

### 1. Composable Screen

```kotlin
import <base_package>.core.analytics.TrackScreenViewEvent

@Composable
fun FeatureAScreen(state: FeatureAState, onAction: (FeatureAAction) -> Unit) {
    TrackScreenViewEvent(screenName = "feature_a")
    // UI Content...
}
```

### 2. ViewModel

```kotlin
import <base_package>.core.analytics.AnalyticsEvent
import <base_package>.core.analytics.AnalyticsHelper

class FeatureAViewModel(private val analyticsHelper: AnalyticsHelper) : ViewModel() {
    fun onAction(action: FeatureAAction) {
        when (action) {
            FeatureAAction.OnSubmitClick -> {
                analyticsHelper.logEvent(
                    AnalyticsEvent(
                        type = AnalyticsEvent.Types.BUTTON_CLICK,
                        extras = listOf(AnalyticsEvent.Param(AnalyticsEvent.ParamKeys.BUTTON_NAME, "submit_form"))
                    )
                )
            }
        }
    }
}
```

## Future Extension: Firebase Analytics

Add class in `core/analytics/`:

```kotlin
class FirebaseAnalyticsHelper(private val firebaseAnalytics: FirebaseAnalytics) : AnalyticsHelper {
    override fun logEvent(event: AnalyticsEvent) {
        firebaseAnalytics.logEvent(event.type) {
            event.extras.forEach { param(it.key, it.value) }
        }
    }
}
```

Bind in Koin `coreModule`:
```kotlin
val coreModule = module {
    single<AnalyticsHelper> { FirebaseAnalyticsHelper(get()) }
}
```
This ensures both ViewModels (injected via constructor) and Composables (via Koin or Local provider) use the identical analytics backend.
