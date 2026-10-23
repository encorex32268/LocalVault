#!/usr/bin/env python3
import os
import re
import sys
import argparse
from pathlib import Path

def find_project_root() -> Path:
    """Finds the root directory of the Android project by checking for Gradle markers."""
    current = Path.cwd()
    while current != current.parent:
        if (current / "gradlew").exists() or (current / "settings.gradle.kts").exists() or (current / "settings.gradle").exists():
            return current
        current = current.parent
    return Path.cwd()

def update_libs_toml(project_root: Path) -> bool:
    """Adds coil version and library entry to gradle/libs.versions.toml."""
    toml_path = project_root / "gradle" / "libs.versions.toml"
    if not toml_path.exists():
        print(f"[WARN] Version catalog not found at: {toml_path}")
        return False

    content = toml_path.read_text(encoding="utf-8")
    changed = False

    # Check and add coil version
    if "coil =" not in content and 'coil="' not in content:
        if "[versions]" in content:
            content = re.sub(
                r'(\[versions\][^\n]*\n)',
                r'\1coil = "2.7.0"\n',
                content,
                count=1
            )
            changed = True

    # Check and add coil-compose library
    if "coil-compose" not in content:
        entry = 'coil-compose = { group = "io.coil-kt", name = "coil-compose", version.ref = "coil" }\n'
        if "[libraries]" in content:
            content = re.sub(
                r'(\[libraries\][^\n]*\n)',
                r'\1' + entry,
                content,
                count=1
            )
            changed = True

    if changed:
        toml_path.write_text(content, encoding="utf-8")
        print(f"[OK] Updated {toml_path.relative_to(project_root)} with Coil dependency.")
    else:
        print(f"[SKIP] Coil already present in {toml_path.relative_to(project_root)}.")

    return changed

def update_build_gradle_kts(build_gradle_path: Path, project_root: Path) -> bool:
    """Configures Coil in the target module's build.gradle.kts."""
    if not build_gradle_path.exists():
        print(f"[WARN] Target Gradle file not found: {build_gradle_path}")
        return False

    content = build_gradle_path.read_text(encoding="utf-8")
    changed = False

    if "coil.compose" not in content and "coil-compose" not in content:
        dep_line = "    implementation(libs.coil.compose)"
        if "dependencies {" in content:
            content = re.sub(
                r'(dependencies\s*\{)',
                r'\1\n' + dep_line,
                content,
                count=1
            )
            changed = True

    if changed:
        build_gradle_path.write_text(content, encoding="utf-8")
        print(f"[OK] Added Coil dependency to {build_gradle_path.relative_to(project_root)}.")
    else:
        print(f"[SKIP] Coil dependency already in {build_gradle_path.relative_to(project_root)}.")

    return changed

def update_manifest_internet_permission(manifest_path: Path, project_root: Path) -> bool:
    """Ensures INTERNET permission is declared in AndroidManifest.xml."""
    if not manifest_path.exists():
        print(f"[WARN] AndroidManifest.xml not found at: {manifest_path}")
        return False

    content = manifest_path.read_text(encoding="utf-8")
    if "android.permission.INTERNET" in content:
        print(f"[SKIP] INTERNET permission already present in {manifest_path.relative_to(project_root)}.")
        return False

    perm_tag = '    <uses-permission android:name="android.permission.INTERNET" />\n'
    if "<application" in content:
        content = content.replace("<application", perm_tag + "    <application", 1)
    else:
        content = re.sub(r'(<manifest[^>]*>)', r'\1\n' + perm_tag, content, count=1)

    manifest_path.write_text(content, encoding="utf-8")
    print(f"[OK] Added INTERNET permission to {manifest_path.relative_to(project_root)}.")
    return True

def configure_high_volume_caching(project_root: Path, module_name: str) -> bool:
    """Finds Application class and configures ImageLoaderFactory with large cache."""
    src_dir = project_root / module_name / "src" / "main" / "java"
    if not src_dir.exists():
        print(f"[WARN] Source directory not found: {src_dir}")
        return False

    app_classes = []
    for kt_file in src_dir.rglob("*.kt"):
        content = kt_file.read_text(encoding="utf-8")
        if ": Application()" in content or ": Application ()" in content:
            app_classes.append(kt_file)

    if not app_classes:
        print("[WARN] No Application subclass found to configure ImageLoaderFactory.")
        return False

    app_file = app_classes[0]
    content = app_file.read_text(encoding="utf-8")

    if "ImageLoaderFactory" in content:
        print(f"[SKIP] ImageLoaderFactory already configured in {app_file.name}.")
        return False

    # 1. Add imports
    imports = """import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.CachePolicy
"""
    # Place after package declaration
    content = re.sub(r'(package\s+[^\n]+\n)', r'\1\n' + imports, content, count=1)

    # 2. Add ImageLoaderFactory to class definition
    content = re.sub(
        r'(class\s+\w+\s*:\s*Application\(\))',
        r'\1, ImageLoaderFactory',
        content,
        count=1
    )

    # 3. Add newImageLoader implementation before closing bracket
    image_loader_impl = """
    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.25)
                    .strongReferencesEnabled(true)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizeBytes(512L * 1024 * 1024) // 512 MB for high-volume caching
                    .build()
            }
            .crossfade(true)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .build()
    }
}
"""
    # Replace the last '}'
    last_brace_index = content.rfind("}")
    if last_brace_index != -1:
        content = content[:last_brace_index] + image_loader_impl

    app_file.write_text(content, encoding="utf-8")
    print(f"[OK] Configured high-volume ImageLoaderFactory in {app_file.relative_to(project_root)}.")
    return True

def main():
    parser = argparse.ArgumentParser(description="Configure Coil Image Loader and caching for Android.")
    parser.add_argument(
        "--module",
        type=str,
        default="app",
        help="Target module directory relative to project root (default: 'app')"
    )
    parser.add_argument(
        "--high-volume",
        action="store_true",
        help="Enable high-volume caching (512MB disk cache + ImageLoaderFactory in Application class)"
    )
    args = parser.parse_args()

    project_root = find_project_root()
    print(f"Detected project root: {project_root}")

    # 1. Update libs.versions.toml
    update_libs_toml(project_root)

    # 2. Update target module build.gradle.kts
    target_gradle = project_root / args.module / "build.gradle.kts"
    update_build_gradle_kts(target_gradle, project_root)

    # 3. Ensure INTERNET permission
    manifest_path = project_root / args.module / "src" / "main" / "AndroidManifest.xml"
    update_manifest_internet_permission(manifest_path, project_root)

    # 4. If high-volume requested, configure Application class
    if args.high_volume:
        configure_high_volume_caching(project_root, args.module)

    print("\nCoil setup complete!")

if __name__ == "__main__":
    main()
