<div align="center">

<img src="app/src/main/res/drawable/play_store_icon_512x512.png" width="120" height="120" alt="LocalVault Logo" />

# LocalVault

**100% Offline, Hardware-Encrypted Password & Credential Manager for Android**

[![Kotlin](https://img.shields.io/badge/Kotlin-2.2.10-7F52FF?style=flat&logo=kotlin&logoColor=white)](https://kotlinlang.org/)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285F4?style=flat&logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![Platform](https://img.shields.io/badge/Platform-Android%20(API%2024%2B)-3DDC84?style=flat&logo=android&logoColor=white)](https://www.android.com/)
[![Architecture](https://img.shields.io/badge/Architecture-Clean%20%2B%20MVI-D97706?style=flat)](https://developer.android.com/topic/architecture)
[![Network](https://img.shields.io/badge/Network-Zero%20Internet%20Permission-16A34A?style=flat)](app/src/main/AndroidManifest.xml)
[![License](https://img.shields.io/badge/License-GPL--3.0-blue?style=flat)](LICENSE)

<br />

<a href="https://play.google.com/store/apps/details?id=com.lihan.localvault">
  <img src="https://img.shields.io/badge/Google%20Play-Download%20on%20Google%20Play-01875F?style=for-the-badge&logo=googleplay&logoColor=white" alt="Get it on Google Play" />
</a>

<br /><br />

**English** &nbsp;|&nbsp; [繁體中文](README.zh-TW.md) &nbsp;|&nbsp; [日本語](README.ja.md)

</div>

---

<p align="center">
  <img src="screenshots/en/feature_graphic_en_1024x500.webp" width="100%" alt="LocalVault Feature Graphic" />
</p>

## Overview

**LocalVault** is a privacy-first, **100% offline** password, credential, and private attachment manager for Android. Unlike cloud-hosted password managers, LocalVault does not even declare the `android.permission.INTERNET` permission in its manifest. Every vault entry and attachment stays strictly on your physical device, protected by **Android KeyStore** hardware-backed **AES-256-GCM** encryption and **BiometricPrompt** authentication.

---

## Demo

<p align="center">
  <img src="screenshots/localvault_demo.gif" width="280" alt="LocalVault Interactive Demo" />
</p>

---

## Screenshots

| Vault List | Expanded Details | Create / Edit | Icon & Color Picker | Instant Search |
| :---: | :---: | :---: | :---: | :---: |
| <img src="screenshots/en/Google%20Pixel%2010%20Screenshot%201.webp" width="175" alt="Vault List" /> | <img src="screenshots/en/Google%20Pixel%2010%20Screenshot%202.webp" width="175" alt="Expanded Details" /> | <img src="screenshots/en/Google%20Pixel%2010%20Screenshot%203.webp" width="175" alt="Create Vault" /> | <img src="screenshots/en/Google%20Pixel%2010%20Screenshot%204.webp" width="175" alt="Custom Icon & Color" /> | <img src="screenshots/en/Google%20Pixel%2010%20Screenshot%205.webp" width="175" alt="Instant Search" /> |

---

## Key Features

- **Zero Network & Zero Telemetry** — No `INTERNET` permission in `AndroidManifest.xml`, `android:allowBackup="false"`, zero analytics, and zero cloud attack surface.
- **Hardware-Backed AES-256-GCM Encryption** — Master keys are generated and stored inside **Android KeyStore** with unique random IVs and 128-bit GCM authentication tags per encryption.
- **Biometric & Device Credential Unlock** — Integrates AndroidX `BiometricPrompt` supporting fingerprint, face unlock, and device PIN/pattern/password with session management.
- **Flexible Dynamic Fields** — Build each vault entry with customizable fields:
  - **Single-line Input**: Usernames, emails, IDs, or URLs.
  - **Password / PIN**: Masked by default with visibility toggle and one-tap clipboard copy.
  - **Multi-line Notes**: Recovery codes, seed phrases, licenses, or private notes.
- **Encrypted File & Image Attachments** — Attach photos, images, and documents encrypted on disk with smart image compression, in-app encrypted image preview, and automatic file cleanup on deletion.
- **Encrypted Local Backup & Restore** — Export and restore your entire vault (with an optional toggle to include encrypted attachments) protected by user passphrase derivation (`PBKDF2WithHmacSHA256`, 100,000 iterations + `AES-256-GCM`).
- **Lazy Decryption Architecture** — The vault list loads only lightweight metadata; encrypted payloads are decrypted strictly on demand when expanded.
- **Visual Personalization & Material 3** — Curated category icons, color themes, instant title search, and seamless Light/Dark mode switching.

---

## Security Architecture

| Layer | Implementation |
| :--- | :--- |
| **Vault Payload Encryption** | `AndroidKeyStoreCryptoManager` — `AES/GCM/NoPadding` (256-bit hardware-backed key, random IV) |
| **Attachment Storage** | `AndroidEncryptedFileManager` — AndroidX Security Crypto encrypted file storage + lifecycle cleanup |
| **Backup / Restore** | `BackupCryptoManager` — `PBKDF2WithHmacSHA256` (100,000 iterations, 16-byte random salt) + `AES-256-GCM` |
| **Authentication** | AndroidX `BiometricPrompt` (Biometrics & Device Credentials) + `SessionManager` |
| **Network Isolation** | `android.permission.INTERNET` is intentionally absent from `AndroidManifest.xml` |
| **System Backup Opt-Out** | `android:allowBackup="false"` prevents automatic cloud backup of local app data |

---

## Tech Stack & Architecture

LocalVault is built with **Clean Architecture** and **MVI (Model-View-Intent)** unidirectional data flow.

| Category | Technology |
| :--- | :--- |
| **Language** | Kotlin `2.2.10` |
| **UI** | Jetpack Compose (BOM `2026.02.01`), Material 3 |
| **Navigation** | Jetpack Navigation Compose `2.8.8` (Type-safe routes) |
| **DI** | Koin `4.0.2` (`koin-android`, `koin-androidx-compose`) |
| **Database** | Room `2.8.4` (SQLite + KSP + schema migrations) |
| **Preferences** | Jetpack DataStore Preferences `1.2.1` |
| **Security** | Android KeyStore, AndroidX Biometric, AndroidX Security Crypto |
| **Media & Images** | Coil Compose `2.7.0`, AndroidX ExifInterface |
| **Async & Serialization** | Kotlin Coroutines `1.10.1`, Flow, Kotlinx Serialization JSON `1.8.0` |

---

## Project Structure

```text
app/src/main/java/com/lihan/localvault/
├── core/
│   ├── data/
│   │   ├── datastore/          # Theme & app preferences (DataStore)
│   │   └── security/
│   │       ├── crypto/         # AndroidKeyStoreCryptoManager (AES-256-GCM)
│   │       └── storage/        # AndroidEncryptedFileManager (encrypted attachments)
│   ├── database/               # Room AppDatabase, DAO, Entities, DTOs, Mappers
│   ├── di/                     # CoreModule (Koin)
│   ├── domain/
│   │   ├── security/           # CryptoManager, BackupCryptoManager, EncryptedFileManager
│   │   └── session/            # SessionManager
│   └── presentation/           # Theme, Icons, BiometricPromptManager, EncryptedImageView
├── feature/
│   └── vault/
│       ├── data/               # VaultRepositoryImpl
│       ├── di/                 # VaultModule (Koin)
│       ├── domain/             # VaultRepository
│       └── presentation/
│           ├── vault/          # Vault list, search, expand, backup/restore (MVI)
│           └── create/         # Create/edit vault, fields, attachments, icon/color picker (MVI)
└── navigation/                 # NavigationRoot & type-safe Route definitions
```

---

## Getting Started

### Prerequisites

- **Android Studio** Ladybug or newer
- **JDK** 17 or 21
- **Android SDK** 37 (`compileSdk = 37`, `minSdk = 24`)

### Build & Run

```bash
# Clone the repository
git clone https://github.com/encorex32268/LocalVault.git
cd LocalVault

# Run unit tests
./gradlew testDebugUnitTest

# Build Debug APK
./gradlew assembleDebug

# Install on a connected device or emulator
./gradlew installDebug
```

---

## License

This project is licensed under the **GNU General Public License v3.0** — see the [LICENSE](LICENSE) file for details.
