#!/usr/bin/env python3
import sys
import os
import re
from pathlib import Path

STATE_TEMPLATE = """package __PACKAGE__.__FEATURE_DIR__.presentation

import __PACKAGE__.__FEATURE_DIR__.presentation.model.__FEATURE__Ui

data class __FEATURE__State(
    val items: List<__FEATURE__Ui> = emptyList(),
    val isLoading: Boolean = false
)
"""

DOMAIN_MODEL_TEMPLATE = """package __PACKAGE__.__FEATURE_DIR__.domain.model

/**
 * Pure Kotlin domain model.
 */
data class __FEATURE__Item(
    val id: String,
    val title: String,
    val timestamp: Long = System.currentTimeMillis()
)
"""

UI_MODEL_TEMPLATE = """package __PACKAGE__.__FEATURE_DIR__.presentation.model

/**
 * Presentation UI Model.
 * Formatted and optimized for Jetpack Compose display without recomposition penalty.
 */
data class __FEATURE__Ui(
    val id: String,
    val title: String,
    val formattedDate: String
)
"""

UI_MAPPER_TEMPLATE = """package __PACKAGE__.__FEATURE_DIR__.presentation.mapper

import __PACKAGE__.__FEATURE_DIR__.domain.model.__FEATURE__Item
import __PACKAGE__.__FEATURE_DIR__.presentation.model.__FEATURE__Ui

fun __FEATURE__Item.to__FEATURE__Ui(): __FEATURE__Ui {
    return __FEATURE__Ui(
        id = id,
        title = title,
        formattedDate = timestamp.toString()
    )
}
"""

ACTION_TEMPLATE = """package __PACKAGE__.__FEATURE_DIR__.presentation

sealed interface __FEATURE__Action {}
"""

VIEW_MODEL_TEMPLATE = """package __PACKAGE__.__FEATURE_DIR__.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn

class __FEATURE__ViewModel : ViewModel() {

    private var hasLoadedInitialData = false

    private val _state = MutableStateFlow(__FEATURE__State())
    val state = _state.onStart {
        if (!hasLoadedInitialData) {
            /** init */
            hasLoadedInitialData = true
        }
    }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = __FEATURE__State()
        )

    fun onAction(action: __FEATURE__Action) {
        when (action) {
            else -> Unit
        }
    }
}
"""

SCREEN_TEMPLATE = """package __PACKAGE__.__FEATURE_DIR__.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.androidx.compose.koinViewModel

@Composable
fun __FEATURE__Root(
    onNavigateToDetail: (String) -> Unit = {},
    viewModel: __FEATURE__ViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    __FEATURE__Screen(
        state = state,
        onAction = viewModel::onAction,
        onNavigateToDetail = onNavigateToDetail
    )
}

@Composable
internal fun __FEATURE__Screen(
    state: __FEATURE__State,
    onAction: (__FEATURE__Action) -> Unit,
    modifier: Modifier = Modifier
) {

}

@Preview(showBackground = true)
@Composable
private fun __FEATURE__ScreenPreview() {
    AppTheme {
        __FEATURE__Screen(
            state = __FEATURE__State(),
            onAction = {}
        )
    }
}
"""

DI_MODULE_TEMPLATE = """package __PACKAGE__.__FEATURE_DIR__.di

import __PACKAGE__.__FEATURE_DIR__.presentation.__FEATURE__ViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val __CAMEL_FEATURE__Module = module {
    viewModelOf(::__FEATURE__ViewModel)
}
"""

NAVIGATION_TEMPLATE = """package __PACKAGE__.__FEATURE_DIR__.presentation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable

@Serializable
data object __FEATURE__Route

fun NavController.navigateTo__FEATURE__(navOptions: NavOptions? = null) {
    navigate(route = __FEATURE__Route, navOptions = navOptions)
}

fun NavGraphBuilder.__CAMEL_FEATURE__Screen(
    onNavigateToDetail: (String) -> Unit = {}
) {
    composable<__FEATURE__Route> {
        __FEATURE__Root(
            onNavigateToDetail = onNavigateToDetail
        )
    }
}
"""

def to_pascal_case(text: str) -> str:
    words = re.split(r'[-_]', text)
    if len(words) > 1:
        return "".join(word.capitalize() for word in words if word)
    return text[:1].upper() + text[1:]

def to_camel_case(text: str) -> str:
    pascal = to_pascal_case(text)
    return pascal[:1].lower() + pascal[1:]

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

def create_feature(feature_name: str, project_root: Path):
    feature_dir_name = feature_name.lower()
    pascal_name = to_pascal_case(feature_name)
    camel_name = to_camel_case(feature_name)
    
    base_pkg, base_path = find_app_base_package(project_root)
    feature_path = base_path / feature_dir_name
    if feature_path.exists() and any(feature_path.rglob("*.kt")):
        print(f"Error: feature '{feature_dir_name}' already exists at {feature_path}. Aborting to avoid overwriting.")
        sys.exit(1)

    print(f"Creating feature '{feature_name}' with Koin DI...")
    print(f"Directory: {feature_path}")
    print(f"Base Package: {base_pkg}")
    print(f"Prefix:    {pascal_name}")

    data_dir = feature_path / "data"
    domain_dir = feature_path / "domain"
    presentation_dir = feature_path / "presentation"
    di_dir = feature_path / "di"

    data_dir.mkdir(parents=True, exist_ok=True)
    domain_dir.mkdir(parents=True, exist_ok=True)
    presentation_dir.mkdir(parents=True, exist_ok=True)
    di_dir.mkdir(parents=True, exist_ok=True)

    def render(template: str) -> str:
        return (
            template.replace("__PACKAGE__", base_pkg)
            .replace("__FEATURE_DIR__", feature_dir_name)
            .replace("__FEATURE__", pascal_name)
            .replace("__CAMEL_FEATURE__", camel_name)
        )

    # 1. State
    state_file = presentation_dir / f"{pascal_name}State.kt"
    state_file.write_text(render(STATE_TEMPLATE), encoding="utf-8")
    print(f"  + {state_file.relative_to(project_root)}")

    # 2. Action
    action_file = presentation_dir / f"{pascal_name}Action.kt"
    action_file.write_text(render(ACTION_TEMPLATE), encoding="utf-8")
    print(f"  + {action_file.relative_to(project_root)}")

    # 3. ViewModel
    vm_file = presentation_dir / f"{pascal_name}ViewModel.kt"
    vm_file.write_text(render(VIEW_MODEL_TEMPLATE), encoding="utf-8")
    print(f"  + {vm_file.relative_to(project_root)}")

    # 4. Screen
    screen_file = presentation_dir / f"{pascal_name}Screen.kt"
    screen_file.write_text(render(SCREEN_TEMPLATE), encoding="utf-8")
    print(f"  + {screen_file.relative_to(project_root)}")

    # 5. DI Module
    di_file = di_dir / f"{pascal_name}Module.kt"
    di_file.write_text(render(DI_MODULE_TEMPLATE), encoding="utf-8")
    print(f"  + {di_file.relative_to(project_root)}")

    # 6. Navigation
    nav_file = presentation_dir / f"{pascal_name}Navigation.kt"
    nav_file.write_text(render(NAVIGATION_TEMPLATE), encoding="utf-8")
    print(f"  + {nav_file.relative_to(project_root)}")

    # 7. Domain Model
    domain_model_dir = domain_dir / "model"
    domain_model_dir.mkdir(parents=True, exist_ok=True)
    domain_model_file = domain_model_dir / f"{pascal_name}Item.kt"
    domain_model_file.write_text(render(DOMAIN_MODEL_TEMPLATE), encoding="utf-8")
    print(f"  + {domain_model_file.relative_to(project_root)}")

    # 8. Presentation UI Model
    pres_model_dir = presentation_dir / "model"
    pres_model_dir.mkdir(parents=True, exist_ok=True)
    pres_model_file = pres_model_dir / f"{pascal_name}Ui.kt"
    pres_model_file.write_text(render(UI_MODEL_TEMPLATE), encoding="utf-8")
    print(f"  + {pres_model_file.relative_to(project_root)}")

    # 9. Presentation UI Mapper
    pres_mapper_dir = presentation_dir / "mapper"
    pres_mapper_dir.mkdir(parents=True, exist_ok=True)
    pres_mapper_file = pres_mapper_dir / f"{pascal_name}UiMapper.kt"
    pres_mapper_file.write_text(render(UI_MAPPER_TEMPLATE), encoding="utf-8")
    print(f"  + {pres_mapper_file.relative_to(project_root)}")

    print(f"\nFeature '{feature_name}' successfully created with Clean Architecture (data -> domain <- presentation)!")
    print(f"Remember to register '{camel_name}Module' in SkillsApp.kt and '{camel_name}Screen' in NavigationRoot.kt!")

if __name__ == "__main__":
    if len(sys.argv) < 2:
        print("Usage: python3 create_feature.py <feature_name>")
        sys.exit(1)
    
    script_path = Path(__file__).resolve()
    guessed_root = script_path.parent.parent.parent.parent
    if (guessed_root / "app").exists():
        proj_root = guessed_root
    else:
        proj_root = Path(os.getcwd())

    create_feature(sys.argv[1], proj_root)
