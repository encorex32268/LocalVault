#!/usr/bin/env python3
import os
import re
import sys
from pathlib import Path

ERROR_CONTENT = """package __PACKAGE__.core.domain.util

interface Error

typealias RootError = Error
"""

RESULT_CONTENT = """package __PACKAGE__.core.domain.util

sealed interface Result<out D, out E : RootError> {
    data class Success<out D>(val data: D) : Result<D, Nothing>
    data class Error<out E : RootError>(val error: E) : Result<Nothing, E>
}

inline fun <T, E : RootError, R> Result<T, E>.map(map: (T) -> R): Result<R, E> {
    return when (this) {
        is Result.Error -> Result.Error(error)
        is Result.Success -> Result.Success(map(data))
    }
}

inline fun <T, E : RootError> Result<T, E>.onSuccess(action: (T) -> Unit): Result<T, E> {
    if (this is Result.Success) action(data)
    return this
}

inline fun <T, E : RootError> Result<T, E>.onFailure(action: (E) -> Unit): Result<T, E> {
    if (this is Result.Error) action(error)
    return this
}

inline fun <T, E : RootError> Result<T, E>.onError(action: (E) -> Unit): Result<T, E> {
    if (this is Result.Error) action(error)
    return this
}

typealias EmptyResult<E> = Result<Unit, E>

fun <T, E : RootError> Result<T, E>.asEmptyResult(): EmptyResult<E> {
    return map { }
}

fun <T, E : RootError> Result<T, E>.asEmptyDataResult(): EmptyResult<E> {
    return map { }
}
"""

DATA_ERROR_CONTENT = """package __PACKAGE__.core.domain.util

sealed interface DataError : Error {
    enum class Network : DataError {
        BAD_REQUEST,
        REQUEST_TIMEOUT,
        UNAUTHORIZED,
        FORBIDDEN,
        NOT_FOUND,
        CONFLICT,
        TOO_MANY_REQUESTS,
        NO_INTERNET,
        PAYLOAD_TOO_LARGE,
        SERVER_ERROR,
        SERVICE_UNAVAILABLE,
        SERIALIZATION,
        UNKNOWN,
        CANCELED,
        FIREBASE_GET_DATA_FAILED
    }

    enum class Local : DataError {
        DISK_FULL,
        NOT_FOUND,
        UNKNOWN
    }
}
"""

def find_target_util_dir(project_root: Path):
    # 1. Check multi-module: core/domain
    core_domain_gradle = project_root / "core" / "domain" / "build.gradle.kts"
    if not core_domain_gradle.exists():
        core_domain_gradle = project_root / "core" / "domain" / "build.gradle"

    if core_domain_gradle.exists():
        content = core_domain_gradle.read_text(encoding="utf-8")
        match = re.search(r'namespace\s*=\s*"([^"]+)"', content)
        if match:
            pkg = match.group(1)
            # Typically com.example.core.domain -> base package is com.example
            base_pkg = re.sub(r'\.core\.domain$', '', pkg)
            src_dir = project_root / "core" / "domain" / "src" / "main" / "java" / Path(*pkg.split(".")) / "util"
            return base_pkg, src_dir

        # Try finding source root in core/domain
        java_root = project_root / "core" / "domain" / "src" / "main" / "java"
        if java_root.exists():
            for root, dirs, files in os.walk(java_root):
                rel = Path(root).relative_to(java_root)
                parts = rel.parts
                if "domain" in parts:
                    domain_idx = parts.index("domain")
                    base_parts = parts[:domain_idx-1] if domain_idx > 0 and parts[domain_idx-1] == "core" else parts[:domain_idx]
                    base_pkg = ".".join(base_parts)
                    return base_pkg, Path(root) / "util"

    # 2. Single-module: check app
    gradle_file = project_root / "app" / "build.gradle.kts"
    if gradle_file.exists():
        content = gradle_file.read_text(encoding="utf-8")
        match = re.search(r'namespace\s*=\s*"([^"]+)"', content)
        if match:
            pkg = match.group(1)
            util_dir = project_root / "app" / "src" / "main" / "java" / Path(*pkg.split(".")) / "core" / "domain" / "util"
            return pkg, util_dir

    java_root = project_root / "app" / "src" / "main" / "java"
    if java_root.exists():
        for root, dirs, files in os.walk(java_root):
            if "MainActivity.kt" in files:
                rel = Path(root).relative_to(java_root)
                pkg = ".".join(rel.parts)
                return pkg, Path(root) / "core" / "domain" / "util"

    raise RuntimeError("Could not determine package location under project")

def setup_error_handling(project_root: Path):
    base_pkg, util_dir = find_target_util_dir(project_root)
    print(f"Base Package: {base_pkg}")
    print(f"Target Util Directory: {util_dir}")
    util_dir.mkdir(parents=True, exist_ok=True)

    files_to_create = [
        ("Error.kt", ERROR_CONTENT),
        ("Result.kt", RESULT_CONTENT),
        ("DataError.kt", DATA_ERROR_CONTENT),
    ]

    for filename, template in files_to_create:
        target_file = util_dir / filename
        if target_file.exists():
            print(f"  = {target_file.name} already exists, skipped")
        else:
            target_file.write_text(template.replace("__PACKAGE__", base_pkg), encoding="utf-8")
            print(f"  + {target_file.relative_to(project_root)}")

    print("\nError handling mechanism setup completed successfully!")

if __name__ == "__main__":
    script_path = Path(__file__).resolve()
    guessed_root = script_path.parent.parent.parent.parent
    if (guessed_root / "app").exists() or (guessed_root / "settings.gradle.kts").exists():
        proj_root = guessed_root
    else:
        proj_root = Path(os.getcwd())

    setup_error_handling(proj_root)
