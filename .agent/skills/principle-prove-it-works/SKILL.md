---
name: principle-prove-it-works
description: Prove It Works principle from Poteto mode. Verify against the real artifact, not a proxy or "it compiles". After a task, before declaring done. Enforces unit, live, and perf verification on real surfaces.
---

# Principle: Prove It Works

Tests alone are not sufficient verification. A PR is verified only when its unit, live, and perf boxes are all checked.

## Non-negotiables

- **Real artifact over proxy.** Never declare a task complete merely because code compiles (`./gradlew assembleDebug`) or static analysis passes. Verify behavior on the real execution artifact.
- **Reproduce before fixing.** For bug fixes, reproduce the defect first with runtime evidence or a failing test before editing production code.
- **No speculative sign-off.** Every verification claim must state the exact command executed, the surface tested, and the observed outcome in the same sentence.

## Verification Triad

### 1. Unit Verification
- Run local unit tests on touched packages:
  ```bash
  ./gradlew testDebugUnitTest
  ```
- Tests must assert observable behavior, not internal implementation details or private method calls.
- Room DAO queries and crypto transformation logic must be validated against real in-memory instances.

### 2. Live Verification
- Test UI changes directly on Android surfaces using:
  - Jetpack Compose Preview rendering for UI components.
  - Connected Android devices or Android Virtual Devices (AVD) via `adb`.
  - Installation and launch checks:
    ```bash
    ./gradlew installDebug
    adb shell am start -n com.lihan.localvault/.MainActivity
    ```
- Verify interactive flows (e.g. biometric authentication dialogs, card expansion, lazy decryption).

### 3. Perf & Constraint Verification
- **Zero-Network Audit.** Verify `app/src/main/AndroidManifest.xml` does not introduce `android.permission.INTERNET` or network listeners.
- **Crypto Round-Trip.** Verify `AndroidKeyStoreCryptoManager` encrypts and decrypts payloads without data corruption or memory leaks.
- **Lazy Decryption.** Verify `VaultSummaryTuple` queries do not prematurely load or decrypt `ciphertext` in list views.

## Verification Block Format

Every verification summary must open with:
`Tests alone are not sufficient verification. A PR is verified only when its unit, live, and perf boxes are all checked.`

Followed by:
- **Unit.** Command run, tests executed, all passed.
- **Live.** Device/emulator target, scenario exercised, observed behavior.
- **Perf / Constraints.** Zero-permission verified, crypto integrity confirmed.
