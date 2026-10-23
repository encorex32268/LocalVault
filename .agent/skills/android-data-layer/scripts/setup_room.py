#!/usr/bin/env python3
import sys
import os
import re
from pathlib import Path

APP_DATABASE_TEMPLATE = """package __PACKAGE__.core.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import __PACKAGE__.core.data.database.dao.ItemDao
import __PACKAGE__.core.data.database.entity.ItemEntity

@Database(
    entities = [
        ItemEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract val itemDao: ItemDao
}
"""

DATABASE_FACTORY_TEMPLATE = """package __PACKAGE__.core.data.database

import android.content.Context
import androidx.room.Room

object DatabaseFactory {
    private const val DB_NAME = "skills_app.db"

    fun create(context: Context): AppDatabase {
        return Room.databaseBuilder(
            context.applicationContext,
            AppDatabase::class.java,
            DB_NAME
        ).build()
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

    if 'room = ' not in content:
        content = re.sub(r'(\[versions\][^\[]*)', r'\1room = "2.8.4"\n', content)
        changed = True

    if 'ksp = ' not in content:
        content = re.sub(r'(\[versions\][^\[]*)', r'\1ksp = "2.2.10-2.0.2"\n', content)
        changed = True

    libs_to_add = [
        ('room-runtime', '{ group = "androidx.room", name = "room-runtime", version.ref = "room" }'),
        ('room-ktx', '{ group = "androidx.room", name = "room-ktx", version.ref = "room" }'),
        ('room-compiler', '{ group = "androidx.room", name = "room-compiler", version.ref = "room" }'),
    ]

    for lib_name, lib_val in libs_to_add:
        if f"{lib_name} = " not in content:
            content = re.sub(r'(\[libraries\][^\[]*)', f'\\1{lib_name} = {lib_val}\n', content)
            changed = True

    if 'ksp = ' not in content.split('[plugins]')[1] if '[plugins]' in content else True:
        if '[plugins]' in content and 'ksp = ' not in content[content.find('[plugins]'):]:
            content = re.sub(r'(\[plugins\][^\[]*)', r'\1ksp = { id = "com.google.devtools.ksp", version.ref = "ksp" }\n', content)
            changed = True

    if changed:
        toml_path.write_text(content, encoding="utf-8")
        print("Updated gradle/libs.versions.toml with Room & KSP dependencies.")

def update_root_build_gradle(project_root):
    root_gradle = project_root / "build.gradle.kts"
    if not root_gradle.exists():
        return
    content = root_gradle.read_text(encoding="utf-8")
    if "alias(libs.plugins.ksp)" not in content:
        content = re.sub(r'(plugins\s*\{)', r'\1\n    alias(libs.plugins.ksp) apply false', content)
        root_gradle.write_text(content, encoding="utf-8")
        print("Updated root build.gradle.kts with KSP plugin.")

def update_app_build_gradle(project_root):
    app_gradle = project_root / "app" / "build.gradle.kts"
    if not app_gradle.exists():
        return
    content = app_gradle.read_text(encoding="utf-8")
    changed = False

    if "alias(libs.plugins.ksp)" not in content:
        content = re.sub(r'(plugins\s*\{)', r'\1\n    alias(libs.plugins.ksp)', content)
        changed = True

    deps = [
        "implementation(libs.room.runtime)",
        "implementation(libs.room.ktx)",
        "ksp(libs.room.compiler)"
    ]

    for dep in deps:
        if dep not in content:
            content = re.sub(r'(dependencies\s*\{)', f'\\1\n    {dep}', content)
            changed = True

    if changed:
        app_gradle.write_text(content, encoding="utf-8")
        print("Updated app/build.gradle.kts with Room dependencies and KSP compiler.")

def update_gradle_properties(project_root):
    props = project_root / "gradle.properties"
    if not props.exists():
        return
    content = props.read_text(encoding="utf-8")
    changed = False

    if "android.disallowKotlinSourceSets" not in content:
        content += "\nandroid.disallowKotlinSourceSets=false\n"
        changed = True

    if "android.sync.suppressAgpWarnings" not in content:
        content += "android.sync.suppressAgpWarnings=UNSUPPORTED_PROJECT_OPTION_USE\n"
        changed = True

    if changed:
        props.write_text(content, encoding="utf-8")
        print("Updated gradle.properties for KSP source set compatibility.")

def scaffold_room_files(project_root, base_package):
    db_pkg = project_root / "app" / "src" / "main" / "java" / Path(*base_package.split(".")) / "core" / "data" / "database"
    db_pkg.mkdir(parents=True, exist_ok=True)

    db_file = db_pkg / "AppDatabase.kt"
    if not db_file.exists():
        db_file.write_text(APP_DATABASE_TEMPLATE.replace("__PACKAGE__", base_package), encoding="utf-8")
        print(f"Created {db_file.relative_to(project_root)}")

    factory_file = db_pkg / "DatabaseFactory.kt"
    if not factory_file.exists():
        factory_file.write_text(DATABASE_FACTORY_TEMPLATE.replace("__PACKAGE__", base_package), encoding="utf-8")
        print(f"Created {factory_file.relative_to(project_root)}")

def main():
    root = find_project_root()
    base_pkg = find_base_package(root)
    print(f"Setting up Room Database for base package: {base_pkg}")
    update_libs_toml(root)
    update_root_build_gradle(root)
    update_app_build_gradle(root)
    update_gradle_properties(root)
    scaffold_room_files(root, base_pkg)
    print("Room Database setup complete!")

if __name__ == "__main__":
    main()
