---
name: verification
description: Verification protocol for LocalVault and Poteto mode. Enforces unit testing, live device/Compose preview verification, zero-network security auditing, and crypto round-trip proof before declaring tasks complete. Trigger on 'verify', 'verification', 'prove it works', or before opening PRs.
---

# Verification Protocol (LocalVault)

Tests alone are not sufficient verification. A PR is verified only when its unit, live, and perf boxes are all checked.

This skill implements the **prove-it-works** principle for LocalVault.

## Verification Checklist

### 1. Build & Static Analysis
```bash
./gradlew lintDebug assembleDebug
```
Confirm build succeeds with zero errors in touched files.

### 2. Unit & Integration Tests
```bash
./gradlew testDebugUnitTest
```
Ensure all JUnit 5 and Turbine coroutine tests pass without flaky assertions.

### 3. Security & Zero-Network Audit
Ensure no network permissions exist in `app/src/main/AndroidManifest.xml`:
```bash
grep -i "permission.INTERNET" app/src/main/AndroidManifest.xml || echo "Zero-network verified."
```

### 4. Live UI & Feature Verification
- For Compose UI components, verify rendering through Compose Previews.
- For interactive changes, deploy to connected device or emulator:
  ```bash
  ./gradlew installDebug
  ```
- Exercise the target user flow and verify that lazy decryption functions properly upon expanding cards.

### 5. Report Template
When reporting verification results, format the section as follows:
```markdown
## Verification
Tests alone are not sufficient verification. A PR is verified only when its unit, live, and perf boxes are all checked.
- **Unit.** `./gradlew testDebugUnitTest` passed with X tests.
- **Live.** Verified on Pixel emulator via `installDebug`, tested vault creation and expand/decrypt flow.
- **Security / Perf.** Confirmed zero network permissions in AndroidManifest.xml and verified lazy tuple loading.
```
