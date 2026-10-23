#!/usr/bin/env python3
import os
import re
import sys
from pathlib import Path

ANALYTICS_EVENT_CONTENT = """package __PACKAGE__.core.analytics

data class AnalyticsEvent(
    val type: String,
    val extras: List<Param> = emptyList(),
) {
    data class Param(val key: String, val value: String)

    object Types {
        const val SCREEN_VIEW = "screen_view"
        const val BUTTON_CLICK = "button_click"
    }

    object ParamKeys {
        const val SCREEN_NAME = "screen_name"
        const val BUTTON_NAME = "button_name"
    }
}
"""

ANALYTICS_HELPER_CONTENT = """package __PACKAGE__.core.analytics

interface AnalyticsHelper {
    fun logEvent(event: AnalyticsEvent)
}
"""

LOGCAT_ANALYTICS_HELPER_CONTENT = """package __PACKAGE__.core.analytics

import android.util.Log

class LogcatAnalyticsHelper : AnalyticsHelper {
    override fun logEvent(event: AnalyticsEvent) {
        val extrasFormatted = event.extras.joinToString { "${it.key}=${it.value}" }
        Log.d("Analytics", "Logging event [${event.type}]: $extrasFormatted")
    }
}
"""

TRACK_SCREEN_VIEW_CONTENT = """package __PACKAGE__.core.analytics

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.staticCompositionLocalOf

val LocalAnalyticsHelper = staticCompositionLocalOf<AnalyticsHelper> {
    LogcatAnalyticsHelper()
}

@Composable
fun TrackScreenViewEvent(
    screenName: String,
    analyticsHelper: AnalyticsHelper = LocalAnalyticsHelper.current
) {
    DisposableEffect(screenName) {
        analyticsHelper.logEvent(
            AnalyticsEvent(
                type = AnalyticsEvent.Types.SCREEN_VIEW,
                extras = listOf(
                    AnalyticsEvent.Param(AnalyticsEvent.ParamKeys.SCREEN_NAME, screenName)
                )
            )
        )
        onDispose {}
    }
}
"""

def find_app_base_package(project_root: Path):
    gradle_file = project_root / "app" / "build.gradle.kts"
    if gradle_file.exists():
        content = gradle_file.read_text(encoding="utf-8")
        match = re.search(r'namespace\s*=\s*"([^"]+)"', content)
        if match:
            pkg = match.group(1)
            pkg_path = project_root / "app" / "src" / "main" / "java" / Path(*pkg.split("."))
            if pkg_path.exists():
                return pkg, pkg_path

    java_root = project_root / "app" / "src" / "main" / "java"
    if java_root.exists():
        for root, dirs, files in os.walk(java_root):
            if "MainActivity.kt" in files:
                rel = Path(root).relative_to(java_root)
                pkg = ".".join(rel.parts)
                return pkg, Path(root)

    raise RuntimeError("Could not determine base package under app/src/main/java")

def setup_analytics(project_root: Path):
    base_pkg, base_path = find_app_base_package(project_root)
    print(f"Base Package: {base_pkg}")

    analytics_dir = base_path / "core" / "analytics"
    analytics_dir.mkdir(parents=True, exist_ok=True)

    files = [
        ("AnalyticsEvent.kt", ANALYTICS_EVENT_CONTENT),
        ("AnalyticsHelper.kt", ANALYTICS_HELPER_CONTENT),
        ("LogcatAnalyticsHelper.kt", LOGCAT_ANALYTICS_HELPER_CONTENT),
        ("TrackScreenViewEvent.kt", TRACK_SCREEN_VIEW_CONTENT),
    ]

    for filename, content in files:
        target_file = analytics_dir / filename
        if target_file.exists():
            print(f"  = {target_file.name} already exists, skipped")
        else:
            target_file.write_text(content.replace("__PACKAGE__", base_pkg), encoding="utf-8")
            print(f"  + {target_file.relative_to(project_root)}")

    print("\nAnalytics module setup completed successfully in core/analytics!")

if __name__ == "__main__":
    script_path = Path(__file__).resolve()
    guessed_root = script_path.parent.parent.parent.parent
    if (guessed_root / "app").exists():
        proj_root = guessed_root
    else:
        proj_root = Path(os.getcwd())

    setup_analytics(proj_root)
