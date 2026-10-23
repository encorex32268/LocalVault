---
name: android-image-loader-coil
description: "Configures and integrates Coil (Image Loader) for Jetpack Compose, setting up dependencies, high-volume/heavy image caching strategies via ImageLoaderFactory, and reusable Compose image components. Trigger on phrases like 'load image', 'AsyncImage', 'Coil', 'image cache', 'placeholder image', or 'remote image URL'."
---

# Android Image Loader Coil Setup

## Core Principles

1. **Declarative Image Loading**: Use Coil's Compose integration (`AsyncImage`, `SubcomposeAsyncImage`) for idiomatic Jetpack Compose image rendering.
2. **Centralized Dependency Management**: Keep Coil library versions centralized in `gradle/libs.versions.toml`.
3. **Internet Permission Safety**: Ensure `android.permission.INTERNET` is declared in `AndroidManifest.xml` whenever loading remote URLs.
4. **Adaptive Caching Strategy (High-Volume / Heavy-Image Projects)**:
   - **Default Caching**: By default, Coil automatically uses 25% of available RAM for `MemoryCache` and 2% of disk space (up to 250MB) for `DiskCache`.
   - **High-Volume Caching**: When an application downloads large quantities of images (e.g. social feeds, e-commerce, galleries, wall-papers):
     - Implement `ImageLoaderFactory` in the `Application` class.
     - Increase `DiskCache` allocation (e.g. 512MB ~ 1GB) and configure LRU memory limits.
     - Enable `crossfade(true)` globally.
     - Optionally configure `respectCacheHeaders(false)` if the backend lacks proper cache headers but image URLs are immutable.

---

## Setup Script

```bash
# Basic setup (adds dependencies & internet permission)
python3 .agents/skills/android-image-loader-coil/scripts/setup_coil.py

# Setup with high-volume custom caching in Application class
python3 .agents/skills/android-image-loader-coil/scripts/setup_coil.py --high-volume
```

---

## Gradle Configuration

### 1. `gradle/libs.versions.toml`

Add the Coil version and library entry:

```toml
[versions]
coil = "2.7.0"

[libraries]
coil-compose = { group = "io.coil-kt", name = "coil-compose", version.ref = "coil" }
```

### 2. Module `build.gradle.kts` (e.g. `app/build.gradle.kts`)

```kotlin
dependencies {
    implementation(libs.coil.compose)
}
```

### 3. `AndroidManifest.xml`

```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <uses-permission android:name="android.permission.INTERNET" />
    ...
</manifest>
```

---

## High-Volume Image Caching Configuration

When the app is expected to handle heavy image traffic (feed lists, photo galleries, e-commerce):

Implement `ImageLoaderFactory` directly on your `Application` class to provide an optimized custom `ImageLoader`:

```kotlin
import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.CachePolicy
import coil.util.DebugLogger

class MyApp : Application(), ImageLoaderFactory {

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .memoryCache {
                MemoryCache.Builder(this)
                    // Allocate 25% of app memory for image cache
                    .maxSizePercent(0.25)
                    .strongReferencesEnabled(true)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    // Custom disk cache limit: 512 MB (adjust for high-volume needs)
                    .maxSizeBytes(512L * 1024 * 1024)
                    .build()
            }
            // Global crossfade transition
            .crossfade(true)
            // Memory & Disk cache policies
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            // Uncomment to debug cache hits/misses in Logcat:
            // .logger(DebugLogger())
            .build()
    }
}
```

---

## Compose UI Best Practices

### 1. Basic AsyncImage
```kotlin
AsyncImage(
    model = imageUrl,
    contentDescription = stringResource(R.string.product_image_desc),
    contentScale = ContentScale.Crop,
    modifier = Modifier
        .fillMaxWidth()
        .height(200.dp)
        .clip(RoundedCornerShape(8.dp))
)
```

> [!TIP]
> **List Performance**: In `LazyColumn` or `LazyRow`, always prefer `AsyncImage` with `placeholder` and `error` painters over `SubcomposeAsyncImage` to avoid subcomposition overhead during rapid scrolling.

### 2. SubcomposeAsyncImage (Loading & Error States)
Use when an animated progress bar or complex layout is strictly required (e.g., in a detail screen):

```kotlin
SubcomposeAsyncImage(
    model = imageUrl,
    contentDescription = stringResource(R.string.product_image_desc),
    contentScale = ContentScale.Crop,
    loading = {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(modifier = Modifier.size(24.dp))
        }
    },
    error = {
        Box(
            modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = AppIcons.Warning,
                contentDescription = stringResource(R.string.image_load_error)
            )
        }
    },
    modifier = modifier
)
```
