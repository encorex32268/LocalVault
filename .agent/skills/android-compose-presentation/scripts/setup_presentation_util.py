#!/usr/bin/env python3
import os
import re
import sys
from pathlib import Path

OBSERVE_AS_EVENTS_CONTENT = """package __PACKAGE__.core.presentation.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

@Composable
fun <T> ObserveAsEvents(
    flow: Flow<T>,
    key1: Any? = null,
    key2: Any? = null,
    onEvent: (T) -> Unit
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner.lifecycle, key1, key2) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            withContext(Dispatchers.Main.immediate) {
                flow.collect(onEvent)
            }
        }
    }
}
"""

UI_TEXT_CONTENT = """package __PACKAGE__.core.presentation.util

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.res.stringResource

@Stable
sealed interface UiText {

    @Stable
    data class DynamicString(val value: String) : UiText

    @Stable
    data class StringResource(
        @param:StringRes val resId: Int,
        val args: Array<Any> = arrayOf()
    ) : UiText {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as StringResource

            if (resId != other.resId) return false
            if (!args.contentEquals(other.args)) return false

            return true
        }

        override fun hashCode(): Int {
            var result = resId
            result = 31 * result + args.contentHashCode()
            return result
        }
    }

    @Composable
    fun asString(): String {
        return when (this) {
            is DynamicString -> value
            is StringResource -> stringResource(resId, *args)
        }
    }

    fun asString(context: Context): String {
        return when (this) {
            is DynamicString -> value
            is StringResource -> context.getString(resId, *args)
        }
    }

    suspend fun asStringAsync(context: Context): String {
        return when (this) {
            is DynamicString -> value
            is StringResource -> context.getString(resId, *args)
        }
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

def setup_presentation_util(project_root: Path):
    base_pkg, base_path = find_app_base_package(project_root)
    print(f"Base Package: {base_pkg}")

    util_dir = base_path / "core" / "presentation" / "util"
    util_dir.mkdir(parents=True, exist_ok=True)

    event_file = util_dir / "ObserveAsEvents.kt"
    if event_file.exists():
        print(f"  = {event_file.name} already exists, skipped")
    else:
        event_file.write_text(OBSERVE_AS_EVENTS_CONTENT.replace("__PACKAGE__", base_pkg), encoding="utf-8")
        print(f"  + {event_file.relative_to(project_root)}")

    uitext_file = util_dir / "UiText.kt"
    if uitext_file.exists():
        print(f"  = {uitext_file.name} already exists, skipped")
    else:
        uitext_file.write_text(UI_TEXT_CONTENT.replace("__PACKAGE__", base_pkg), encoding="utf-8")
        print(f"  + {uitext_file.relative_to(project_root)}")

    print("\nPresentation utilities setup completed successfully in core/presentation/util!")

if __name__ == "__main__":
    script_path = Path(__file__).resolve()
    guessed_root = script_path.parent.parent.parent.parent
    if (guessed_root / "app").exists():
        proj_root = guessed_root
    else:
        proj_root = Path(os.getcwd())

    setup_presentation_util(proj_root)
