<div align="center">

<img src="app/src/main/res/drawable/play_store_icon_512x512.png" width="96" height="96" alt="LocalVault Logo" />

# LocalVault (日本語)

**安全 • 完全オフライン • ネットワーク権限不要 • 生体認証保護**

プライバシー重視の Android 向けオープンソース認証情報＆パスワードマネージャー。最新の Jetpack Compose と AndroidKeyStore によるハードウェア保護 AES-256-GCM 暗号化を採用。

<p>
  <a href="README.md"><b>English</b></a> •
  <a href="README.zh-TW.md"><b>繁體中文</b></a> •
  <a href="README.ja.md"><b>日本語</b></a>
</p>

[![Platform](https://img.shields.io/badge/Platform-Android-3DDC84?style=flat-square&logo=android)](https://www.android.com/)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0+-7F52FF?style=flat-square&logo=kotlin)](https://kotlinlang.org/)
[![Compose](https://img.shields.io/badge/UI-Jetpack%20Compose%20M3-4285F4?style=flat-square&logo=jetpackcompose)](https://developer.android.com/jetpack/compose)
[![Min SDK](https://img.shields.io/badge/Min%20SDK-24-orange?style=flat-square)](https://developer.android.com/about/versions/nougat)
[![Security](https://img.shields.io/badge/暗号化-AES--256--GCM-green?style=flat-square)](https://developer.android.com/reference/android/security/keystore/KeyGenParameterSpec)
[![Network](https://img.shields.io/badge/インターネット権限-なし%20%28完全オフライン%29-brightgreen?style=flat-square)](#)

</div>

---

<p align="center">
  <img src="screenshots/jp/feature_graphic_ja_1024x500.png" width="100%" alt="LocalVault Banner JP" />
</p>

### 概要

**LocalVault** は、プライバシーとセキュリティを最優先に設計された **完全オフライン・ゼロネットワーク権限** の Android 向けパスワード＆認証情報管理アプリです。機密データをサードパーティのクラウドサーバーに送信する従来のパスワードマネージャーとは異なり、LocalVault はインターネット権限を一切使用しません。すべてのデータはご自身の端末内にのみ保存され、**Android KeyStore** と **AES-256-GCM** による強力なハードウェア暗号化で安全に保護されます。

### 📱 スクリーンショット

| 1. 保管庫一覧 | 2. 詳細表示 | 3. 作成・編集 | 4. アイコン＆カラー設定 | 5. インスタント検索 |
| :---: | :---: | :---: | :---: | :---: |
| <img src="screenshots/jp/Google%20Pixel%2010%20Screenshot%201.png" width="180" alt="保管庫一覧" /> | <img src="screenshots/jp/Google%20Pixel%2010%20Screenshot%202.png" width="180" alt="詳細表示" /> | <img src="screenshots/jp/Google%20Pixel%2010%20Screenshot%203.png" width="180" alt="保管庫作成" /> | <img src="screenshots/jp/Google%20Pixel%2010%20Screenshot%204.png" width="180" alt="アイコン選択" /> | <img src="screenshots/jp/Google%20Pixel%2010%20Screenshot%205.png" width="180" alt="インスタント検索" /> |

### ✨ 主な機能

- 🚫 **完全オフライン＆インターネット権限ゼロ**：`AndroidManifest.xml` で `android.permission.INTERNET` を一切要求しません。トラッカーやアナリティクス、クラウド漏洩のリスクを根本から排除します。
- 🔐 **ハードウェア保護による AES-256-GCM 暗号化**：`AndroidKeyStoreCryptoManager` により、Android のセキュアハードウェア内にマスターキーを生成・保管。改ざん検知機能付きの AES-256-GCM（ランダム IV）で安全に暗号化します。
- 👆 **生体認証＆画面ロックによる保護**：`BiometricPrompt` に対応し、指紋認証、顔認証、または端末の PIN・パターン・パスワードによるスムーズな本人確認が可能です。
- 🧩 **自由度の高いカスタムフィールド**：
  - **1行テキスト**：ユーザー名、メールアドレス、URL、ID など。
  - **パスワード／PIN**：マスク表示による覗き見防止、表示切り替え、ワンタップでのクリップボードコピーに対応。
  - **複数行テキスト（メモ）**：シードフレーズ、リカバリーコード、自由なメモ内容。
  - **項目名の自由変更**：各フィールドのタイトルを必要に応じて自由に設定可能。
- 🎨 **ビジュアルカスタマイズ**：豊富なカテゴリ別アイコンとアクセントカラーから自由に選択し、視認性を高めます。
- 🔍 **高速インクリメンタル検索**：タイトル入力で即座に絞り込み検索が可能。
- 🌓 **Material 3 ダーク＆ライトテーマ**：最新の Material You に準拠し、ダークモードとライトモードを快適に切り替えられます。
- ⚡ **遅延復号アーキテクチャ（Lazy Decryption）**：一覧画面ではサマリー情報のみを高速に読み込み、ユーザーがカードを展開した時のみ詳細データを復号するため、軽快な動作と安全性を両立しています。

### 🛠️ アーキテクチャと技術スタック

本プロジェクトは **Clean Architecture** と **MVI (Model-View-Intent)** パターンを採用しています：

- **開発言語**：Kotlin 2.0+
- **UI フレームワーク**：Jetpack Compose (Material 3)
- **ローカルデータベース**：Room Database (SQLite)
- **セキュリティ・暗号化**：Android KeyStore (`AES/GCM/NoPadding`, 256-bit キー)
- **認証**：AndroidX Biometric (`BiometricPrompt`)
- **依存性注入**：Koin (`koin-android`, `koin-androidx-compose`)
- **環境設定・設定保存**：Jetpack DataStore Preferences
- **非同期・ストリーム**：Kotlin Coroutines & Flow
- **シリアライズ**：Kotlinx Serialization JSON
- **スプラッシュ画面**：AndroidX Core SplashScreen API

### 🚀 ビルドと実行方法

#### 前提条件
- Android Studio Ladybug (2024.2.1) 以降
- JDK 17 または JDK 21
- Android SDK 37 (Compile SDK 37, Min SDK 24)

#### ビルド手順
```bash
# リポジトリのクローン
git clone https://github.com/your-username/LocalVault.git
cd LocalVault

# Debug APK のビルド
./gradlew assembleDebug

# 接続端末またはエミュレーターへインストール
./gradlew installDebug
```

---

## 📄 ライセンス

本プロジェクトはオープンソースです。ライセンス条件はリポジトリをご確認ください。
