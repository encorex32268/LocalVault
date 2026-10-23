#!/usr/bin/env python3
import os
import re
import sys
from pathlib import Path

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
        UNKNOWN
    }

    enum class Local : DataError {
        DISK_FULL,
        NOT_FOUND,
        UNKNOWN
    }
}
"""

NETWORK_MONITOR_INTERFACE = """package __PACKAGE__.core.data.util

import kotlinx.coroutines.flow.Flow

interface NetworkMonitor {
    val isOnline: Flow<Boolean>
}
"""

CONNECTIVITY_MANAGER_NETWORK_MONITOR = """package __PACKAGE__.core.data.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import androidx.core.content.getSystemService
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.flowOn

/**
 * 1. 建立 (Callback)
 * 2. 註冊 (Register)
 * 3. Send 初始值
 * 4. 取消註冊 (await close)
 * Tips
 * - 手機可以同時連 Wi-Fi 和 4G：概念：斷掉一個網路不代表沒網，所以要用 Set 存起來，判斷 set.isNotEmpty()。
 * - 連上 Wi-Fi 不代表能上網（例如需要登入的飯店網路）: 概念：不能只看 INTERNET，還要看 VALIDATED。
 */
class ConnectivityManagerNetworkMonitor(
    private val context: Context,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : NetworkMonitor {

    override val isOnline: Flow<Boolean> = callbackFlow {
        val connectivityManager = context.getSystemService<ConnectivityManager>()
        if (connectivityManager == null) {
            channel.trySend(false)
            channel.close()
            return@callbackFlow
        }

        val callback = object : ConnectivityManager.NetworkCallback() {

            private val networks = mutableSetOf<Network>()

            override fun onAvailable(network: Network) {
                super.onAvailable(network)
                networks += network
                channel.trySend(true)
            }

            override fun onLost(network: Network) {
                super.onLost(network)
                networks -= network
                channel.trySend(networks.isNotEmpty())
            }

            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities
            ) {
                super.onCapabilitiesChanged(network, networkCapabilities)

                val hasInternet = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                val isValidated = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)

                if (hasInternet && isValidated) {
                    networks += network
                } else {
                    networks -= network
                }
                channel.trySend(networks.isNotEmpty())
            }
        }

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        connectivityManager.registerNetworkCallback(request, callback)

        channel.trySend(connectivityManager.isCurrentlyConnected())

        awaitClose {
            connectivityManager.unregisterNetworkCallback(callback)
        }
    }
        .flowOn(ioDispatcher)
        .conflate()
}

/**
 * 檢查當前活躍網路是否具備 Internet 訪問能力
 */
private fun ConnectivityManager.isCurrentlyConnected(): Boolean {
    val capabilities = getNetworkCapabilities(activeNetwork) ?: return false
    return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
}
"""

HTTP_CLIENT_EXT_CONTENT = """package __PACKAGE__.core.data.networking

import __PACKAGE__.core.domain.util.DataError
import __PACKAGE__.core.domain.util.Result
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.request.url
import io.ktor.client.statement.HttpResponse
import io.ktor.util.network.UnresolvedAddressException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerializationException

suspend inline fun <reified Response : Any> HttpClient.get(
    route: String,
    queryParameters: Map<String, Any?> = mapOf()
): Result<Response, DataError.Network> {
    return safeCall {
        get {
            url(route)
            queryParameters.forEach { (key, value) ->
                parameter(key, value)
            }
        }
    }
}

suspend inline fun <reified Request : Any, reified Response : Any> HttpClient.post(
    route: String,
    body: Request,
    queryParameters: Map<String, Any?> = mapOf()
): Result<Response, DataError.Network> {
    return safeCall {
        post {
            url(route)
            setBody(body)
            queryParameters.forEach { (key, value) ->
                parameter(key, value)
            }
        }
    }
}

suspend inline fun <reified Request : Any, reified Response : Any> HttpClient.put(
    route: String,
    body: Request,
    queryParameters: Map<String, Any?> = mapOf()
): Result<Response, DataError.Network> {
    return safeCall {
        put {
            url(route)
            setBody(body)
            queryParameters.forEach { (key, value) ->
                parameter(key, value)
            }
        }
    }
}

suspend inline fun <reified Response : Any> HttpClient.delete(
    route: String,
    queryParameters: Map<String, Any?> = mapOf()
): Result<Response, DataError.Network> {
    return safeCall {
        delete {
            url(route)
            queryParameters.forEach { (key, value) ->
                parameter(key, value)
            }
        }
    }
}

suspend inline fun <reified T> safeCall(
    execute: () -> HttpResponse
): Result<T, DataError.Network> {
    val response = try {
        execute()
    } catch (e: UnresolvedAddressException) {
        return Result.Error(DataError.Network.NO_INTERNET)
    } catch (e: UnknownHostException) {
        return Result.Error(DataError.Network.NO_INTERNET)
    } catch (e: SocketTimeoutException) {
        return Result.Error(DataError.Network.REQUEST_TIMEOUT)
    } catch (e: HttpRequestTimeoutException) {
        return Result.Error(DataError.Network.REQUEST_TIMEOUT)
    } catch (e: SerializationException) {
        return Result.Error(DataError.Network.SERIALIZATION)
    } catch (e: Exception) {
        if (e is CancellationException) throw e
        return Result.Error(DataError.Network.UNKNOWN)
    }
    return responseToResult(response)
}

suspend inline fun <reified T> responseToResult(response: HttpResponse): Result<T, DataError.Network> {
    return when (response.status.value) {
        in 200..299 -> {
            try {
                Result.Success(response.body<T>())
            } catch (e: SerializationException) {
                Result.Error(DataError.Network.SERIALIZATION)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Result.Error(DataError.Network.UNKNOWN)
            }
        }
        400 -> Result.Error(DataError.Network.BAD_REQUEST)
        401 -> Result.Error(DataError.Network.UNAUTHORIZED)
        403 -> Result.Error(DataError.Network.FORBIDDEN)
        404 -> Result.Error(DataError.Network.NOT_FOUND)
        408 -> Result.Error(DataError.Network.REQUEST_TIMEOUT)
        409 -> Result.Error(DataError.Network.CONFLICT)
        413 -> Result.Error(DataError.Network.PAYLOAD_TOO_LARGE)
        429 -> Result.Error(DataError.Network.TOO_MANY_REQUESTS)
        503 -> Result.Error(DataError.Network.SERVICE_UNAVAILABLE)
        in 500..599 -> Result.Error(DataError.Network.SERVER_ERROR)
        else -> Result.Error(DataError.Network.UNKNOWN)
    }
}
"""

HTTP_CLIENT_FACTORY_CONTENT = """package __PACKAGE__.core.data.networking

import android.util.Log
import __PACKAGE__.BuildConfig
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

class HttpClientFactory {
    fun build(): HttpClient {
        return HttpClient(CIO) {
            install(ContentNegotiation) {
                json(
                    json = Json {
                        isLenient = true
                        ignoreUnknownKeys = true
                    }
                )
            }
            install(Logging) {
                logger = object : Logger {
                    override fun log(message: String) {
                        if (BuildConfig.DEBUG) {
                            Log.d("HttpClient", message)
                        }
                    }
                }
                level = if (BuildConfig.DEBUG) LogLevel.ALL else LogLevel.NONE
            }
            defaultRequest {
                contentType(ContentType.Application.Json)
            }
        }
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

def ensure_internet_permission(project_root: Path):
    manifest_path = project_root / "app" / "src" / "main" / "AndroidManifest.xml"
    if not manifest_path.exists():
        return
    content = manifest_path.read_text(encoding="utf-8")
    permission_tag = '<uses-permission android:name="android.permission.INTERNET" />'
    if "android.permission.INTERNET" in content:
        print("  ✓ INTERNET permission already present in AndroidManifest.xml")
    else:
        app_idx = content.find("<application")
        if app_idx != -1:
            updated = content[:app_idx] + "    " + permission_tag + "\n    " + content[app_idx:]
            manifest_path.write_text(updated, encoding="utf-8")
            print("  + Injected INTERNET permission into AndroidManifest.xml")

def ensure_build_config_feature(project_root: Path):
    gradle_file = project_root / "app" / "build.gradle.kts"
    if not gradle_file.exists():
        return
    content = gradle_file.read_text(encoding="utf-8")
    if "buildConfig = true" in content:
        print("  ✓ buildConfig = true already present in app/build.gradle.kts")
    else:
        if "buildFeatures {" in content:
            updated = content.replace("buildFeatures {", "buildFeatures {\n        buildConfig = true")
            gradle_file.write_text(updated, encoding="utf-8")
            print("  + Enabled buildConfig = true in app/build.gradle.kts")

def setup_networking(project_root: Path):
    base_pkg, base_path = find_app_base_package(project_root)
    print(f"Base Package: {base_pkg}")

    # 1. Permission & Gradle feature
    ensure_internet_permission(project_root)
    ensure_build_config_feature(project_root)

    # 2. Domain DataError.kt
    domain_dir = base_path / "core" / "domain" / "util"
    domain_dir.mkdir(parents=True, exist_ok=True)
    data_error_file = domain_dir / "DataError.kt"
    if data_error_file.exists():
        print(f"  = {data_error_file.name} already exists, skipped")
    else:
        data_error_file.write_text(DATA_ERROR_CONTENT.replace("__PACKAGE__", base_pkg), encoding="utf-8")
        print(f"  + {data_error_file.relative_to(project_root)}")

    # 3. Data networking
    net_dir = base_path / "core" / "data" / "networking"
    net_dir.mkdir(parents=True, exist_ok=True)

    ext_file = net_dir / "HttpClientExt.kt"
    if ext_file.exists():
        print(f"  = {ext_file.name} already exists, skipped")
    else:
        ext_file.write_text(HTTP_CLIENT_EXT_CONTENT.replace("__PACKAGE__", base_pkg), encoding="utf-8")
        print(f"  + {ext_file.relative_to(project_root)}")

    factory_file = net_dir / "HttpClientFactory.kt"
    if factory_file.exists():
        print(f"  = {factory_file.name} already exists, skipped")
    else:
        factory_file.write_text(HTTP_CLIENT_FACTORY_CONTENT.replace("__PACKAGE__", base_pkg), encoding="utf-8")
        print(f"  + {factory_file.relative_to(project_root)}")

    print("\nNetworking pipeline setup completed successfully in core/data/networking and core/domain/util!")

def ensure_network_state_permission(project_root: Path):
    manifest_path = project_root / "app" / "src" / "main" / "AndroidManifest.xml"
    if not manifest_path.exists():
        print(f"Warning: AndroidManifest.xml not found at {manifest_path}")
        return

    content = manifest_path.read_text(encoding="utf-8")
    permission_tag = '<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />'

    if "ACCESS_NETWORK_STATE" in content:
        print("  ✓ ACCESS_NETWORK_STATE permission already present in AndroidManifest.xml")
    else:
        app_idx = content.find("<application")
        if app_idx != -1:
            updated_content = content[:app_idx] + "    " + permission_tag + "\n\n    " + content[app_idx:]
            manifest_path.write_text(updated_content, encoding="utf-8")
            print("  + Injected ACCESS_NETWORK_STATE permission into AndroidManifest.xml")
        else:
            print("Warning: Could not locate <application> tag to inject permission.")

def setup_network_monitor(project_root: Path):
    base_pkg, base_path = find_app_base_package(project_root)

    # 1. Manifest permission
    ensure_network_state_permission(project_root)

    # 2. core/data/util
    util_dir = base_path / "core" / "data" / "util"
    util_dir.mkdir(parents=True, exist_ok=True)

    interface_file = util_dir / "NetworkMonitor.kt"
    if interface_file.exists():
        print(f"  = {interface_file.name} already exists, skipped")
    else:
        interface_file.write_text(NETWORK_MONITOR_INTERFACE.replace("__PACKAGE__", base_pkg), encoding="utf-8")
        print(f"  + {interface_file.relative_to(project_root)}")

    impl_file = util_dir / "ConnectivityManagerNetworkMonitor.kt"
    if impl_file.exists():
        print(f"  = {impl_file.name} already exists, skipped")
    else:
        impl_file.write_text(CONNECTIVITY_MANAGER_NETWORK_MONITOR.replace("__PACKAGE__", base_pkg), encoding="utf-8")
        print(f"  + {impl_file.relative_to(project_root)}")

    print("\nNetworkMonitor setup completed successfully (Now in Android pattern: core/data/util)!")

if __name__ == "__main__":
    import argparse
    parser = argparse.ArgumentParser(description="Setup Ktor networking and optional connectivity monitoring.")
    parser.add_argument(
        "--with-monitor",
        action="store_true",
        help="Also set up NetworkMonitor for connectivity monitoring (ACCESS_NETWORK_STATE permission)."
    )
    args = parser.parse_args()

    script_path = Path(__file__).resolve()
    guessed_root = script_path.parent.parent.parent.parent
    if (guessed_root / "app").exists():
        proj_root = guessed_root
    else:
        proj_root = Path(os.getcwd())

    setup_networking(proj_root)

    if args.with_monitor:
        setup_network_monitor(proj_root)
