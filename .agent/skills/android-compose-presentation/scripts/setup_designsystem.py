#!/usr/bin/env python3
import sys
import os
import re
from pathlib import Path

APP_ICONS_TEMPLATE = """package __PACKAGE__.core.presentation.designsystem.icon

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Centralized Design System Icon Object.
 * All application icons should be referenced from AppIcons rather than direct Material/Asset calls.
 */
object AppIcons {
    val Home: ImageVector = Icons.Default.Home
    val Search: ImageVector = Icons.Default.Search
    val Settings: ImageVector = Icons.Default.Settings
    val ArrowBack: ImageVector = Icons.AutoMirrored.Filled.ArrowBack
    val Refresh: ImageVector = Icons.Default.Refresh
    val Inbox: ImageVector = Icons.Default.Inbox
}
"""

APP_BUTTON_TEMPLATE = """package __PACKAGE__.core.presentation.designsystem.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/**
 * Design System standard button.
 * Uses Slot API for content, with unified brand shape and built-in loading indicator support.
 */
@Composable
fun AppButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    content: @Composable RowScope.() -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled && !isLoading,
        shape = MaterialTheme.shapes.medium
    ) {
        if (isLoading) {
            Box(contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
        } else {
            content()
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AppButtonPreview() {
    AppTheme {
        AppButton(onClick = {}) {
            Text(text = "App Button")
        }
    }
}
"""

APP_CARD_TEMPLATE = """package __PACKAGE__.core.presentation.designsystem.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Design System standard card with Slot API.
 */
@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            content = content
        )
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

def scaffold_designsystem(project_root, base_package):
    base_pkg_path = project_root / "app" / "src" / "main" / "java" / Path(*base_package.split("."))
    icon_dir = base_pkg_path / "core" / "presentation" / "designsystem" / "icon"
    ds_comp_dir = base_pkg_path / "core" / "presentation" / "designsystem" / "components"
    common_comp_dir = base_pkg_path / "core" / "presentation" / "components"

    icon_dir.mkdir(parents=True, exist_ok=True)
    ds_comp_dir.mkdir(parents=True, exist_ok=True)
    common_comp_dir.mkdir(parents=True, exist_ok=True)

    icons_file = icon_dir / "AppIcons.kt"
    if not icons_file.exists():
        icons_file.write_text(APP_ICONS_TEMPLATE.replace("__PACKAGE__", base_package), encoding="utf-8")
        print(f"Created {icons_file.relative_to(project_root)}")

    btn_file = ds_comp_dir / "AppButton.kt"
    if not btn_file.exists():
        btn_file.write_text(APP_BUTTON_TEMPLATE.replace("__PACKAGE__", base_package), encoding="utf-8")
        print(f"Created {btn_file.relative_to(project_root)}")

    card_file = ds_comp_dir / "AppCard.kt"
    if not card_file.exists():
        card_file.write_text(APP_CARD_TEMPLATE.replace("__PACKAGE__", base_package), encoding="utf-8")
        print(f"Created {card_file.relative_to(project_root)}")

def main():
    root = find_project_root()
    base_pkg = find_base_package(root)
    print(f"Setting up Design System for base package: {base_pkg}")
    scaffold_designsystem(root, base_pkg)
    print("Design System setup complete!")

if __name__ == "__main__":
    main()
