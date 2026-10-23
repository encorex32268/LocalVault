#!/usr/bin/env python3
import os
import re
import sys
from pathlib import Path

SPLASH_THEME_XML = """<?xml version="1.0" encoding="utf-8"?>
<resources>
    <!-- Starting splash theme extending Theme.SplashScreen -->
    <style name="Theme.App.Starting" parent="Theme.SplashScreen">
        <!-- Background color of the splash screen -->
        <item name="windowSplashScreenBackground">@android:color/white</item>

        <!-- Centered splash icon (Vector drawable recommended) -->
        <!-- Replace @drawable/ic_launcher_foreground with your app logo -->
        <item name="windowSplashScreenAnimatedIcon">@drawable/ic_launcher_foreground</item>

        <!-- Theme to switch to once splash screen is dismissed -->
        <!-- Adjust @style/Theme.App to your application base theme -->
        <item name="postSplashScreenTheme">@android:style/Theme.Material.Light.NoActionBar</item>
    </style>
</resources>
"""

def find_project_root() -> Path:
    """Finds the root directory of the Android project by checking for Gradle markers."""
    current = Path.cwd()
    while current != current.parent:
        if (current / "gradlew").exists() or (current / "settings.gradle.kts").exists() or (current / "settings.gradle").exists():
            return current
        current = current.parent
    return Path.cwd()

def update_libs_toml(project_root: Path) -> bool:
    """Adds androidx.core:core-splashscreen to gradle/libs.versions.toml."""
    toml_path = project_root / "gradle" / "libs.versions.toml"
    if not toml_path.exists():
        print(f"[WARN] Version catalog not found at: {toml_path}")
        return False

    content = toml_path.read_text(encoding="utf-8")
    changed = False

    # Check and add to [versions]
    if "androidxCoreSplashScreen" not in content:
        if "[versions]" in content:
            content = re.sub(
                r'(\[versions\][^\n]*\n)',
                r'\1androidxCoreSplashScreen = "1.0.1"\n',
                content,
                count=1
            )
            changed = True
        else:
            content = '[versions]\nandroidxCoreSplashScreen = "1.0.1"\n\n' + content
            changed = True

    # Check and add to [libraries]
    if "androidx-core-splashscreen" not in content and "core-splashscreen" not in content:
        entry = 'androidx-core-splashscreen = { group = "androidx.core", name = "core-splashscreen", version.ref = "androidxCoreSplashScreen" }\n'
        if "[libraries]" in content:
            content = re.sub(
                r'(\[libraries\][^\n]*\n)',
                r'\1' + entry,
                content,
                count=1
            )
            changed = True
        else:
            content = content + f"\n[libraries]\n{entry}"
            changed = True

    if changed:
        toml_path.write_text(content, encoding="utf-8")
        print(f"[OK] Updated {toml_path.relative_to(project_root)} with core-splashscreen dependency.")
    else:
        print(f"[SKIP] core-splashscreen already exists in {toml_path.relative_to(project_root)}.")

    return changed

def update_build_gradle_kts(build_gradle_path: Path, project_root: Path) -> bool:
    """Adds implementation(libs.androidx.core.splashscreen) to module build.gradle.kts."""
    if not build_gradle_path.exists():
        print(f"[WARN] Target Gradle file not found: {build_gradle_path}")
        return False

    content = build_gradle_path.read_text(encoding="utf-8")
    changed = False

    if "core.splashscreen" not in content and "androidx-core-splashscreen" not in content:
        dep_line = "    implementation(libs.androidx.core.splashscreen)\n"
        if "dependencies {" in content:
            content = re.sub(
                r'(dependencies\s*\{[^\n]*\n)',
                r'\1' + dep_line,
                content,
                count=1
            )
            changed = True
        else:
            content += f"\ndependencies {{\n{dep_line}}}\n"
            changed = True

    if changed:
        build_gradle_path.write_text(content, encoding="utf-8")
        print(f"[OK] Added splashscreen dependency to {build_gradle_path.relative_to(project_root)}.")
    else:
        print(f"[SKIP] Splashscreen dependency already present in {build_gradle_path.relative_to(project_root)}.")

    return changed

def create_splash_theme_xml(project_root: Path) -> bool:
    """Creates res/values/splash_theme.xml in app module if it doesn't already exist."""
    res_values_dir = project_root / "app" / "src" / "main" / "res" / "values"
    if not res_values_dir.exists():
        # Fallback search for any res/values directory
        candidates = list(project_root.glob("**/src/main/res/values"))
        if candidates:
            res_values_dir = candidates[0]
        else:
            res_values_dir.mkdir(parents=True, exist_ok=True)

    splash_xml_path = res_values_dir / "splash_theme.xml"
    if splash_xml_path.exists():
        print(f"[SKIP] Splash theme XML already exists at: {splash_xml_path.relative_to(project_root)}")
        return False

    splash_xml_path.write_text(SPLASH_THEME_XML, encoding="utf-8")
    print(f"[OK] Created splash theme at: {splash_xml_path.relative_to(project_root)}")
    return True

def inspect_manifest(project_root: Path):
    """Inspects AndroidManifest.xml and suggests launcher theme configuration."""
    manifest_candidates = list(project_root.glob("**/src/main/AndroidManifest.xml"))
    if not manifest_candidates:
        print("[INFO] No AndroidManifest.xml found to inspect.")
        return

    manifest_path = manifest_candidates[0]
    content = manifest_path.read_text(encoding="utf-8")
    if "Theme.App.Starting" in content:
        print(f"[OK] Launcher theme 'Theme.App.Starting' already set in {manifest_path.relative_to(project_root)}")
    else:
        print(f"[ACTION REQUIRED] In {manifest_path.relative_to(project_root)}:")
        print("Set android:theme=\"@style/Theme.App.Starting\" on your launcher Activity.")

def main():
    print("=== Android Splash Screen Auto Setup ===")
    project_root = find_project_root()
    print(f"Target Project Root: {project_root}")

    # 1. Update libs.versions.toml
    update_libs_toml(project_root)

    # 2. Update app build.gradle.kts
    app_build_gradle = project_root / "app" / "build.gradle.kts"
    if not app_build_gradle.exists():
        gradle_files = list(project_root.glob("**/build.gradle.kts"))
        for gf in gradle_files:
            if "app" in str(gf):
                app_build_gradle = gf
                break

    if app_build_gradle.exists():
        update_build_gradle_kts(app_build_gradle, project_root)
    else:
        print("[WARN] Could not automatically locate app/build.gradle.kts.")

    # 3. Create splash_theme.xml
    create_splash_theme_xml(project_root)

    # 4. Manifest check
    inspect_manifest(project_root)

    print("\n--- Setup Complete ---")
    print("Next Steps:")
    print("1. In MainActivity.kt, call installSplashScreen() before super.onCreate().")
    print("2. Adjust @color/splash_background and @drawable/ic_splash_logo in splash_theme.xml.")
    print("3. Ensure postSplashScreenTheme points to your actual application theme.")

if __name__ == "__main__":
    main()
