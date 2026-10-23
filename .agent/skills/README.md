# 🤖 Android Develop Skills

> **[中文](#中文)** | **[English](#english)** | **[日本語](#日本語)**

---

## 中文

一組可重複使用的 Android 開發技能模板與最佳實踐集合，基於 **Compose + MVI + Koin + Ktor + Room** 架構，靈感來自 Google [Now in Android](https://github.com/android/nowinandroid) 參考專案。

每個技能模組包含 `SKILL.md` 說明文件與自動化腳本，可快速套用到你的專案中。

### 📦 模組一覽

| 模組 | 說明 |
|------|------|
| **android-analytics** | 解耦式分析埋點管線，支援 Compose 螢幕曝光追蹤 |
| **android-background-working** | WorkManager 與 AlarmManager 背景排程，含 Clean Architecture 抽象與 Koin Worker 注入 |
| **android-compose-presentation** | Compose UI 最佳實踐、Design System 三層元件架構、`ObserveAsEvents`、`UiText`、Splash Screen、SavedStateHandle |
| **android-data-layer** | 統一資料層架構：Ktor HTTP 客戶端、Room 資料庫、DataStore 鍵值儲存、離線優先 SSOT 模式 |
| **android-di-koin** | Koin 依賴注入慣例、模組命名規範、Constructor Reference、多模組參考 |
| **android-error-handling** | 函數式 `Result<D, E>` 型別、Ktor safeCall、UiText 錯誤映射、統一跨層錯誤處理 |
| **android-image-loader-coil** | Coil 圖片載入設定，含高流量快取策略 |
| **android-module-structure** | 專案模組架構（單模組 / 多模組）、Gradle 設定、Convention Plugins、Java 8+ Desugaring |
| **android-navigation** | Navigation Compose 2.8+ 型別安全導航、底部標籤切換、跨 Feature Lambda 解耦 |
| **android-permission** | 宣告式 Compose 權限處理（通知、相機、定位） |
| **android-single-app-module-feature-create** | MVI 功能模組自動產生腳本 |
| **android-testing** | JUnit 5 + AssertK + Turbine + MainCoroutineExtension 測試模式與 Compose UI Robot Pattern |

### 🚀 使用方式

1. 進入目標模組資料夾
2. 閱讀 `SKILL.md` 了解架構與慣例
3. 執行 `scripts/` 中的自動化腳本（如有）套用到你的專案

---

## English

A collection of reusable Android development skill templates and best practices built on **Compose + MVI + Koin + Ktor + Room**, inspired by Google's [Now in Android](https://github.com/android/nowinandroid) reference app.

Each skill module includes a `SKILL.md` guide and automation scripts for quick project integration.

### 📦 Modules

| Module | Description |
|--------|-------------|
| **android-analytics** | Decoupled analytics pipeline with Compose screen view tracking |
| **android-background-working** | WorkManager & AlarmManager scheduling with Clean Architecture abstraction & Koin Worker injection |
| **android-compose-presentation** | Compose UI best practices, Design System three-tier hierarchy, `ObserveAsEvents`, `UiText`, Splash Screen, SavedStateHandle |
| **android-data-layer** | Unified data layer: Ktor HTTP client, Room database, DataStore key-value storage, offline-first SSOT pattern |
| **android-di-koin** | Koin DI conventions, module naming rules, constructor references, multi-module reference |
| **android-error-handling** | Functional `Result<D, E>` type, Ktor safeCall, UiText error mapping, unified cross-layer error handling |
| **android-image-loader-coil** | Coil image loading with high-volume caching strategies |
| **android-module-structure** | Project module architecture (single/multi-module), Gradle config, Convention Plugins, Java 8+ Desugaring |
| **android-navigation** | Type-safe Navigation Compose 2.8+ with bottom tab switching & cross-feature lambda decoupling |
| **android-permission** | Declarative Compose permission handling (notification, camera, location) |
| **android-single-app-module-feature-create** | MVI feature module scaffolding script |
| **android-testing** | JUnit 5 + AssertK + Turbine + MainCoroutineExtension patterns & Compose UI Robot Pattern |

### 🚀 Usage

1. Navigate to the target module folder
2. Read `SKILL.md` for architecture guidelines and conventions
3. Run the automation scripts in `scripts/` (if available) to apply to your project

---

## 日本語

**Compose + MVI + Koin + Ktor + Room** アーキテクチャに基づく、再利用可能な Android 開発スキルテンプレートとベストプラクティス集です。Google の [Now in Android](https://github.com/android/nowinandroid) を参考にしています。

各スキルモジュールには `SKILL.md` ガイドと自動化スクリプトが含まれています。

### 📦 モジュール一覧

| モジュール | 説明 |
|-----------|------|
| **android-analytics** | 疎結合な分析パイプライン、Compose 画面表示トラッキング |
| **android-background-working** | WorkManager と AlarmManager バックグラウンドスケジューリング、Clean Architecture 抽象化と Koin Worker 注入 |
| **android-compose-presentation** | Compose UI ベストプラクティス、Design System 3 層コンポーネント階層、`ObserveAsEvents`、`UiText`、スプラッシュ画面、SavedStateHandle |
| **android-data-layer** | 統合データ層：Ktor HTTP クライアント、Room データベース、DataStore キー値ストレージ、オフラインファースト SSOT パターン |
| **android-di-koin** | Koin DI 規約、モジュール命名規則、コンストラクタリファレンス、マルチモジュール参考 |
| **android-error-handling** | 関数型 `Result<D, E>`、Ktor safeCall、UiText エラーマッピング、統一エラーハンドリング |
| **android-image-loader-coil** | Coil 画像ローディング、大容量キャッシュ戦略 |
| **android-module-structure** | プロジェクトモジュール構成（シングル/マルチモジュール）、Gradle 設定、Convention Plugins、Java 8+ Desugaring |
| **android-navigation** | Navigation Compose 2.8+ 型安全ナビゲーション、ボトムタブ切替、クロスフィーチャー Lambda 分離 |
| **android-permission** | 宣言的 Compose 権限処理（通知・カメラ・位置情報） |
| **android-single-app-module-feature-create** | MVI 機能モジュール自動生成スクリプト |
| **android-testing** | JUnit 5 + AssertK + Turbine + MainCoroutineExtension テストパターンと Compose UI Robot Pattern |

### 🚀 使い方

1. 対象のモジュールフォルダに移動
2. `SKILL.md` を読んでアーキテクチャとルールを確認
3. `scripts/` 内の自動化スクリプト（ある場合）を実行してプロジェクトに適用

---

## 📄 License

This project is for personal/team reference use.
