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
    """Adds desugar_jdk_libs version and library entry to gradle/libs.versions.toml."""
    toml_path = project_root / "gradle" / "libs.versions.toml"
    if not toml_path.exists():
        print(f"[WARN] Version catalog not found at: {toml_path}")
        return False

    content = toml_path.read_text(encoding="utf-8")
    changed = False

    # Check and add to [versions]
    if "desugarJdkLibs" not in content:
        if "[versions]" in content:
            content = re.sub(
                r'(\[versions\][^\n]*\n)',
                r'\1desugarJdkLibs = "2.1.5"\n',
                content,
                count=1
            )
            changed = True
        else:
            content = '[versions]\ndesugarJdkLibs = "2.1.5"\n\n' + content
            changed = True

    # Check and add to [libraries]
    if "desugar-jdk-libs" not in content and "desugar_jdk_libs" not in content:
        entry = 'desugar-jdk-libs = { group = "com.android.tools", name = "desugar_jdk_libs", version.ref = "desugarJdkLibs" }\n'
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
        print(f"[OK] Updated {toml_path.relative_to(project_root)} with desugar_jdk_libs.")
    else:
        print(f"[SKIP] Desugar dependencies already exist in {toml_path.relative_to(project_root)}.")

    return changed

def update_build_gradle_kts(build_gradle_path: Path, project_root: Path) -> bool:
    """Configures compileOptions and dependencies for desugaring in the specified build.gradle.kts."""
    if not build_gradle_path.exists():
        print(f"[WARN] Target Gradle file not found: {build_gradle_path}")
        return False

    content = build_gradle_path.read_text(encoding="utf-8")
    changed = False

    # 1. Ensure isCoreLibraryDesugaringEnabled = true in compileOptions
    if "isCoreLibraryDesugaringEnabled" not in content:
        if "compileOptions {" in content:
            content = re.sub(
                r'(compileOptions\s*\{)',
                r'\1\n        isCoreLibraryDesugaringEnabled = true',
                content,
                count=1
            )
            changed = True
        elif "android {" in content:
            compile_options_block = """    compileOptions {
        isCoreLibraryDesugaringEnabled = true
    }
"""
            content = re.sub(
                r'(android\s*\{)',
                r'\1\n' + compile_options_block,
                content,
                count=1
            )
            changed = True
        else:
            print(f"[WARN] Could not find android block in {build_gradle_path.name}.")

    # 2. Ensure coreLibraryDesugaring dependency is added
    if "coreLibraryDesugaring" not in content:
        dep_line = "    coreLibraryDesugaring(libs.desugar.jdk.libs)"
        if "dependencies {" in content:
            content = re.sub(
                r'(dependencies\s*\{)',
                r'\1\n' + dep_line,
                content,
                count=1
            )
            changed = True
        else:
            content = content + f"\ndependencies {{\n{dep_line}\n}}\n"
            changed = True

    if changed:
        build_gradle_path.write_text(content, encoding="utf-8")
        print(f"[OK] Enabled desugaring in {build_gradle_path.relative_to(project_root)}.")
    else:
        print(f"[SKIP] Desugaring already configured in {build_gradle_path.relative_to(project_root)}.")

    return changed

def main():
    parser = argparse.ArgumentParser(description="Configure Android Core Library Desugaring.")
    parser.add_argument(
        "--module",
        type=str,
        default="app",
        help="Target module directory relative to project root (default: 'app')"
    )
    args = parser.parse_args()

    project_root = find_project_root()
    print(f"Detected project root: {project_root}")

    # 1. Update libs.versions.toml
    update_libs_toml(project_root)

    # 2. Update target module build.gradle.kts
    target_gradle = project_root / args.module / "build.gradle.kts"
    if not target_gradle.exists():
        # Fallback to root build.gradle.kts if user is running in single-module root
        if (project_root / "build.gradle.kts").exists():
            target_gradle = project_root / "build.gradle.kts"

    update_build_gradle_kts(target_gradle, project_root)
    print("\nDesugaring setup complete!")

if __name__ == "__main__":
    main()
