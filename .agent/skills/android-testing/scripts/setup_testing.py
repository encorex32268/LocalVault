#!/usr/bin/env python3
import sys
import os
import re
from pathlib import Path

MAIN_COROUTINE_EXTENSION_TEMPLATE = """package __PACKAGE__.core.test

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.extension.AfterEachCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext

/**
 * JUnit5 Extension that sets the main coroutine dispatcher to a [TestDispatcher]
 * before each test and resets it afterwards.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MainCoroutineExtension(
    val testDispatcher: TestDispatcher = UnconfinedTestDispatcher()
) : BeforeEachCallback, AfterEachCallback {

    override fun beforeEach(context: ExtensionContext?) {
        Dispatchers.setMain(testDispatcher)
    }

    override fun afterEach(context: ExtensionContext?) {
        Dispatchers.resetMain()
    }
}
"""

def find_project_root():
    current = Path.cwd()
    while current != current.parent:
        if (current / "gradlew").exists() or (current / "settings.gradle.kts").exists():
            return current
        current = current.parent
    return Path.cwd()

def find_base_package(project_root):
    app_java = project_root / "app" / "src" / "main" / "java"
    if not app_java.exists():
        return "com.example.app"
    for path in app_java.rglob("*.kt"):
        content = path.read_text(encoding="utf-8")
        match = re.search(r"package\s+([a-zA-Z0-9_.]+)", content)
        if match:
            pkg = match.group(1)
            parts = pkg.split(".")
            if len(parts) >= 3:
                return ".".join(parts[:3])
            return pkg
    return "com.example.app"

def update_libs_toml(project_root):
    toml_path = project_root / "gradle" / "libs.versions.toml"
    if not toml_path.exists():
        return
    content = toml_path.read_text(encoding="utf-8")
    changed = False

    versions = [
        ('junit5', '"5.10.1"'),
        ('junitPlatform', '"1.10.1"'),
        ('assertk', '"0.28.1"'),
        ('turbine', '"1.2.0"'),
        ('coroutinesTest', '"1.10.1"'),
    ]

    for ver_name, ver_val in versions:
        if f"{ver_name} = " not in content:
            content = re.sub(r'(\[versions\][^\[]*)', f'\\1{ver_name} = {ver_val}\n', content)
            changed = True

    libs = [
        ('junit-jupiter-api', '{ group = "org.junit.jupiter", name = "junit-jupiter-api", version.ref = "junit5" }'),
        ('junit-jupiter-engine', '{ group = "org.junit.jupiter", name = "junit-jupiter-engine", version.ref = "junit5" }'),
        ('junit-jupiter-params', '{ group = "org.junit.jupiter", name = "junit-jupiter-params", version.ref = "junit5" }'),
        ('junit-platform-launcher', '{ group = "org.junit.platform", name = "junit-platform-launcher", version.ref = "junitPlatform" }'),
        ('assertk', '{ group = "com.willowtreeapps.assertk", name = "assertk", version.ref = "assertk" }'),
        ('turbine', '{ group = "app.cash.turbine", name = "turbine", version.ref = "turbine" }'),
        ('kotlinx-coroutines-test', '{ group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-test", version.ref = "coroutinesTest" }'),
    ]

    for lib_name, lib_val in libs:
        if f"{lib_name} = " not in content:
            content = re.sub(r'(\[libraries\][^\[]*)', f'\\1{lib_name} = {lib_val}\n', content)
            changed = True

    bundle_str = """[bundles]
testing-unit = [
    "junit-jupiter-api",
    "junit-jupiter-params",
    "assertk",
    "turbine",
    "kotlinx-coroutines-test"
]
"""
    if 'testing-unit = ' not in content:
        if '[bundles]' in content:
            content = content.replace('[bundles]', '[bundles]\ntesting-unit = [\n    "junit-jupiter-api",\n    "junit-jupiter-params",\n    "assertk",\n    "turbine",\n    "kotlinx-coroutines-test"\n]')
        else:
            content += "\n" + bundle_str
        changed = True

    if changed:
        toml_path.write_text(content, encoding="utf-8")
        print("Updated gradle/libs.versions.toml with testing dependencies.")

def update_build_gradle(project_root):
    build_gradle = project_root / "app" / "build.gradle.kts"
    if not build_gradle.exists():
        return
    content = build_gradle.read_text(encoding="utf-8")
    changed = False

    if 'it.useJUnitPlatform()' not in content:
        test_options = """    testOptions {
        unitTests.all {
            it.useJUnitPlatform()
        }
    }
"""
        if 'testOptions {' not in content:
            content = re.sub(r'(android\s*\{)', f'\\1\n{test_options}', content)
            changed = True

    deps = [
        "testImplementation(libs.bundles.testing.unit)",
        "testRuntimeOnly(libs.junit.jupiter.engine)",
        "testRuntimeOnly(libs.junit.platform.launcher)",
    ]

    for dep in deps:
        if dep not in content:
            content = re.sub(r'(dependencies\s*\{)', f'\\1\n    {dep}', content)
            changed = True

    if changed:
        build_gradle.write_text(content, encoding="utf-8")
        print("Updated app/build.gradle.kts with testOptions and testing dependencies.")

def scaffold_test_files(project_root, base_package):
    pkg_path = project_root / "app" / "src" / "test" / "java" / Path(*base_package.split(".")) / "core" / "test"
    pkg_path.mkdir(parents=True, exist_ok=True)

    ext_file = pkg_path / "MainCoroutineExtension.kt"
    if not ext_file.exists():
        ext_file.write_text(MAIN_COROUTINE_EXTENSION_TEMPLATE.replace("__PACKAGE__", base_package), encoding="utf-8")
        print(f"Created {ext_file.relative_to(project_root)}")

def main():
    root = find_project_root()
    base_pkg = find_base_package(root)
    print(f"Setting up testing for base package: {base_pkg}")
    update_libs_toml(root)
    update_build_gradle(root)
    scaffold_test_files(root, base_pkg)
    print("Testing setup complete!")

if __name__ == "__main__":
    main()
