---
name: android-permission
description: "Sets up Accompanist runtime permissions and declarative PermissionEffect composables (notification, camera, location). Use whenever the user requests implementing runtime permissions in Jetpack Compose. Trigger on phrases like 'runtime permission', 'request permission', 'POST_NOTIFICATIONS', 'camera permission', 'location permission', 'rationale', or 'open app settings'."
---

# Android Permission Setup

## Architecture
```text
app/src/main/
├── AndroidManifest.xml
└── java/<base_package>/core/presentation/permission/
    ├── NotificationPermissionEffect.kt
    ├── CameraPermissionEffect.kt
    ├── LocationPermissionEffect.kt
    └── AppSettingsUtil.kt
```

## Key Technical Features
- **Preview Safety**: Avoids `@Preview` crashes by checking for Activity context.
- **API Version-Adaptive**: `NotificationPermissionEffect` checks runtime permissions on API 33+.
- **Android 12+ Paired Location**: Requests `ACCESS_COARSE_LOCATION` and `ACCESS_FINE_LOCATION` together.
- **Permanent Denial Recovery**: Provides `context.openAppSettings()`.

## Setup Script

```bash
# Setup all standard permission effects:
python3 .agents/skills/android-permission/scripts/setup_permission.py all

# Setup specific permission only:
python3 .agents/skills/android-permission/scripts/setup_permission.py notification
python3 .agents/skills/android-permission/scripts/setup_permission.py camera
python3 .agents/skills/android-permission/scripts/setup_permission.py location
```

## Usage Examples

### 1. Notification Prompt on Start
```kotlin
@Composable
fun HomeRoot(...) {
    NotificationPermissionEffect(
        onGranted = { /* Notification enabled */ }
    )
    HomeScreen(...)
}
```

### 2. Camera Trigger
```kotlin
@Composable
fun ScanQrRoot(
    viewModel: ScanQrViewModel = koinViewModel()
) {
    CameraPermissionEffect(
        onGranted = { viewModel.onAction(ScanQrAction.CameraReady) },
        onShowRationale = { viewModel.onAction(ScanQrAction.ShowCameraRationaleDialog) }
    )
    ScanQrScreen(...)
}
```
