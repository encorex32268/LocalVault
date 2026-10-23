#!/usr/bin/env python3
import sys
import os
import re
from pathlib import Path

APP_DATA_STORE_INTERFACE = """package __PACKAGE__.core.domain.datastore

import kotlinx.coroutines.flow.Flow

/**
 * Domain contract for app key-value persistence.
 * Pure Kotlin interface decoupled from Android/DataStore framework.
 */
interface AppDataStore {
    /**
     * Example boolean flow (e.g. app first-time launch / onboarding flag).
     */
    val isFirstTime: Flow<Boolean>

    /**
     * Updates the boolean setting.
     */
    suspend fun setIsFirstTime(isFirstTime: Boolean)
}
"""

DEFAULT_APP_DATA_STORE = """package __PACKAGE__.core.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStore
import __PACKAGE__.core.domain.datastore.AppDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

val Context.appDataStore: DataStore<Preferences> by preferencesDataStore(name = "app_preferences")

class DefaultAppDataStore(
    private val dataStore: DataStore<Preferences>
) : AppDataStore {

    private companion object {
        val KEY_IS_FIRST_TIME = booleanPreferencesKey("is_first_time")
    }

    override val isFirstTime: Flow<Boolean> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[KEY_IS_FIRST_TIME] ?: true
        }

    override suspend fun setIsFirstTime(isFirstTime: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEY_IS_FIRST_TIME] = isFirstTime
        }
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

    if 'dataStore = ' not in content:
        content = re.sub(r'(\[versions\][^\[]*)', r'\1dataStore = "1.2.1"\n', content)
        changed = True

    if 'androidx-datastore-preferences = ' not in content:
        content = re.sub(
            r'(\[libraries\][^\[]*)',
            r'\1androidx-datastore-preferences = { group = "androidx.datastore", name = "datastore-preferences", version.ref = "dataStore" }\n',
            content
        )
        changed = True

    if changed:
        toml_path.write_text(content, encoding="utf-8")
        print("Updated gradle/libs.versions.toml with DataStore dependency.")

def update_build_gradle(project_root):
    build_gradle = project_root / "app" / "build.gradle.kts"
    if not build_gradle.exists():
        return
    content = build_gradle.read_text(encoding="utf-8")
    dep = "implementation(libs.androidx.datastore.preferences)"
    if dep not in content:
        content = re.sub(r'(dependencies\s*\{)', f'\\1\n    {dep}', content)
        build_gradle.write_text(content, encoding="utf-8")
        print("Updated app/build.gradle.kts with DataStore dependency.")

def scaffold_datastore_files(project_root, base_package):
    domain_pkg = project_root / "app" / "src" / "main" / "java" / Path(*base_package.split(".")) / "core" / "domain" / "datastore"
    data_pkg = project_root / "app" / "src" / "main" / "java" / Path(*base_package.split(".")) / "core" / "data" / "datastore"

    domain_pkg.mkdir(parents=True, exist_ok=True)
    data_pkg.mkdir(parents=True, exist_ok=True)

    interface_file = domain_pkg / "AppDataStore.kt"
    if not interface_file.exists():
        interface_file.write_text(APP_DATA_STORE_INTERFACE.replace("__PACKAGE__", base_package), encoding="utf-8")
        print(f"Created {interface_file.relative_to(project_root)}")

    impl_file = data_pkg / "DefaultAppDataStore.kt"
    if not impl_file.exists():
        impl_file.write_text(DEFAULT_APP_DATA_STORE.replace("__PACKAGE__", base_package), encoding="utf-8")
        print(f"Created {impl_file.relative_to(project_root)}")

def update_core_module(project_root, base_package):
    core_module_file = project_root / "app" / "src" / "main" / "java" / Path(*base_package.split(".")) / "core" / "di" / "CoreModule.kt"
    if not core_module_file.exists():
        return
    content = core_module_file.read_text(encoding="utf-8")
    changed = False

    if 'DefaultAppDataStore' not in content:
        # Add imports
        imports = f"""import {base_package}.core.data.datastore.DefaultAppDataStore
import {base_package}.core.data.datastore.appDataStore
import {base_package}.core.domain.datastore.AppDataStore
import org.koin.android.ext.koin.androidContext
"""
        content = re.sub(r'(package\s+[a-zA-Z0-9_.]+\s*\n)', f'\\1\n{imports}', content)
        
        # Add DI definitions
        di_bindings = """    single { androidContext().appDataStore }
    singleOf(::DefaultAppDataStore) { bind<AppDataStore>() }
"""
        content = re.sub(r'(val\s+coreModule\s*=\s*module\s*\{)', f'\\1\n{di_bindings}', content)
        changed = True

    if changed:
        core_module_file.write_text(content, encoding="utf-8")
        print("Updated CoreModule.kt with AppDataStore Koin bindings.")

def main():
    root = find_project_root()
    base_pkg = find_base_package(root)
    print(f"Setting up DataStore for base package: {base_pkg}")
    update_libs_toml(root)
    update_build_gradle(root)
    scaffold_datastore_files(root, base_pkg)
    update_core_module(root, base_pkg)
    print("DataStore setup complete!")

if __name__ == "__main__":
    main()
