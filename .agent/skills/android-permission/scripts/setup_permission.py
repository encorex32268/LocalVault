#!/usr/bin/env python3
import os
import re
import sys
from pathlib import Path

NOTIFICATION_EFFECT = """package __PACKAGE__.core.presentation.permission

import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalInspectionMode
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionStatus
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale

@Composable
@OptIn(ExperimentalPermissionsApi::class)
fun NotificationPermissionEffect(
    onGranted: () -> Unit = {}
) {
    if (LocalInspectionMode.current) return
    if (VERSION.SDK_INT < VERSION_CODES.TIRAMISU) {
        onGranted()
        return
    }
    val notificationsPermissionState = rememberPermissionState(
        android.Manifest.permission.POST_NOTIFICATIONS,
    )
    LaunchedEffect(notificationsPermissionState.status) {
        when (val status = notificationsPermissionState.status) {
            is PermissionStatus.Granted -> onGranted()
            is PermissionStatus.Denied -> {
                if (!status.shouldShowRationale) {
                    notificationsPermissionState.launchPermissionRequest()
                }
            }
        }
    }
}
"""

CAMERA_EFFECT = """package __PACKAGE__.core.presentation.permission

import android.Manifest
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalInspectionMode
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionStatus
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale

@Composable
@OptIn(ExperimentalPermissionsApi::class)
fun CameraPermissionEffect(
    onGranted: () -> Unit = {},
    onShowRationale: () -> Unit = {}
) {
    if (LocalInspectionMode.current) return
    val cameraPermissionState = rememberPermissionState(
        Manifest.permission.CAMERA
    )
    LaunchedEffect(cameraPermissionState.status) {
        when (val status = cameraPermissionState.status) {
            is PermissionStatus.Granted -> onGranted()
            is PermissionStatus.Denied -> {
                if (status.shouldShowRationale) {
                    onShowRationale()
                } else {
                    cameraPermissionState.launchPermissionRequest()
                }
            }
        }
    }
}
"""

LOCATION_EFFECT = """package __PACKAGE__.core.presentation.permission

import android.Manifest
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalInspectionMode
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState

@Composable
@OptIn(ExperimentalPermissionsApi::class)
fun LocationPermissionEffect(
    onGranted: () -> Unit = {},
    onShowRationale: () -> Unit = {}
) {
    if (LocalInspectionMode.current) return
    val locationPermissionsState = rememberMultiplePermissionsState(
        listOf(
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    )
    LaunchedEffect(locationPermissionsState.allPermissionsGranted) {
        if (locationPermissionsState.allPermissionsGranted) {
            onGranted()
        } else if (locationPermissionsState.shouldShowRationale) {
            onShowRationale()
        } else {
            locationPermissionsState.launchMultiplePermissionRequest()
        }
    }
}
"""

APP_SETTINGS_UTIL = """package __PACKAGE__.core.presentation.permission

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings

fun Context.openAppSettings() {
    Intent(
        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        Uri.fromParts("package", packageName, null)
    ).also { intent ->
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
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

def inject_manifest_tags(project_root: Path, tags_to_add: list[str]):
    manifest_path = project_root / "app" / "src" / "main" / "AndroidManifest.xml"
    if not manifest_path.exists():
        return
    content = manifest_path.read_text(encoding="utf-8")
    app_idx = content.find("<application")
    if app_idx == -1:
        return

    added = []
    for tag in tags_to_add:
        # Check if permission or feature is already in manifest
        search_term = tag.split('android:name="')[1].split('"')[0] if 'android:name="' in tag else tag
        if search_term not in content:
            content = content[:app_idx] + "    " + tag + "\n" + content[app_idx:]
            app_idx += len("    " + tag + "\n")
            added.append(search_term)
        else:
            print(f"  ✓ {search_term} already present in AndroidManifest.xml")

    if added:
        manifest_path.write_text(content, encoding="utf-8")
        print(f"  + Injected into AndroidManifest.xml: {', '.join(added)}")

def setup_permission(permission_type: str, project_root: Path):
    base_pkg, base_path = find_app_base_package(project_root)
    print(f"Base Package: {base_pkg}")

    perm_dir = base_path / "core" / "presentation" / "permission"
    perm_dir.mkdir(parents=True, exist_ok=True)

    # Always write AppSettingsUtil
    app_settings_file = perm_dir / "AppSettingsUtil.kt"
    if app_settings_file.exists():
        print(f"  = {app_settings_file.name} already exists, skipped")
    else:
        app_settings_file.write_text(APP_SETTINGS_UTIL.replace("__PACKAGE__", base_pkg), encoding="utf-8")

    perm_lower = permission_type.lower()
    
    if perm_lower in ["notification", "notifications", "all"]:
        inject_manifest_tags(project_root, ['<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />'])
        notif_file = perm_dir / "NotificationPermissionEffect.kt"
        if notif_file.exists():
            print(f"  = {notif_file.name} already exists, skipped")
        else:
            notif_file.write_text(NOTIFICATION_EFFECT.replace("__PACKAGE__", base_pkg), encoding="utf-8")
            print(f"  + {notif_file.relative_to(project_root)}")

    if perm_lower in ["camera", "all"]:
        inject_manifest_tags(project_root, [
            '<uses-permission android:name="android.permission.CAMERA" />',
            '<uses-feature android:name="android.hardware.camera" android:required="false" />'
        ])
        cam_file = perm_dir / "CameraPermissionEffect.kt"
        if cam_file.exists():
            print(f"  = {cam_file.name} already exists, skipped")
        else:
            cam_file.write_text(CAMERA_EFFECT.replace("__PACKAGE__", base_pkg), encoding="utf-8")
            print(f"  + {cam_file.relative_to(project_root)}")

    if perm_lower in ["location", "all"]:
        inject_manifest_tags(project_root, [
            '<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />',
            '<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />'
        ])
        loc_file = perm_dir / "LocationPermissionEffect.kt"
        if loc_file.exists():
            print(f"  = {loc_file.name} already exists, skipped")
        else:
            loc_file.write_text(LOCATION_EFFECT.replace("__PACKAGE__", base_pkg), encoding="utf-8")
            print(f"  + {loc_file.relative_to(project_root)}")

    print(f"\nPermission setup for '{permission_type}' completed successfully in core/presentation/permission!")

if __name__ == "__main__":
    target = sys.argv[1] if len(sys.argv) > 1 else "all"
    script_path = Path(__file__).resolve()
    guessed_root = script_path.parent.parent.parent.parent
    if (guessed_root / "app").exists():
        proj_root = guessed_root
    else:
        proj_root = Path(os.getcwd())

    setup_permission(target, proj_root)
