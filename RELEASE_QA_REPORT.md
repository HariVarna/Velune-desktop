# Velune Desktop — Phase 5 Production Release QA Report

**Repository:** `https://github.com/HariVarna/Velune-desktop`  
**QA Date:** 2026-11-26  
**Target Version:** `1.1.2` (`veluneDesktopVersion=1.1.2`)

---

## 1. Test Cases & Results

| ID | Test Area | Scope Verified | Status |
|---|---|---|---|
| **T1** | Development Launch (`.\run-desktop.ps1`) | `-Help`, `-Build` (`:desktop:jar`), and default dev launch (`:desktop:run`) | **PASSED** |
| **T2** | Portable Application Image | `:desktop:createDistributable`, copied to `%TEMP%\Velune-QA-Portable\Velune`, launched outside repo | **PASSED** |
| **T3** | Windows Installers (EXE & MSI) | `:desktop:packageExe` & `:desktop:packageMsi` generation + SHA-256 hashes | **PARTIAL** (Build passed; interactive GUI install/shortcut/uninstall not run in headless session) |
| **T4** | Clean Environment Execution | Launched portable `Velune.exe` with `JAVA_HOME=""`, `ANDROID_HOME=""`, `PATH="C:\Windows\System32;C:\Windows"` | **PASSED** (`HasExited=False`, `Responding=True`, `BUNDLED_FFPLAY=True`, `BUNDLED_RUNTIME=True`) |
| **T5** | GitHub Actions Release Pipeline | `.github/workflows/desktop-release.yml` structure & local `DesktopPackagingTest` | **PARTIAL** (YAML & local tests passed; remote GitHub Actions run not triggered—awaiting user push/tag) |
| **T6** | Regression & Android Preservation | `.github/workflows/android-release.yml` unchanged, `DesktopPackagingTest` passed | **PASSED** |

---

## 2. Passed Tests (Observed)

1. **`.\run-desktop.ps1 -Help` & `.\run-desktop.ps1 -Build`:** Printed usage help and built `:desktop:jar` (`BUILD SUCCESSFUL`).
2. **`.\run-desktop.ps1` (Development Mode):** Launched `:desktop:run` with `java.exe` running and `Responding=True`.
3. **Offline Unit Tests (`DesktopPackagingTest`):** Verified bundled `velune.png` icon resource, default `DesktopPersistedState`, and `formatTimeMs` (`BUILD SUCCESSFUL`).
4. **Portable Image & Clean Environment (`T2` + `T4`):**
   - Copied `desktop/build/compose/binaries/main/app/Velune` to `%TEMP%\Velune-QA-Portable\Velune`.
   - Verified `runtime\bin\server\jvm.dll` and `app\resources\ffplay.exe` are bundled inside the image.
   - Launched `Velune.exe` with `JAVA_HOME=""`, `ANDROID_HOME=""`, and minimal system `PATH`: remained running (`HasExited=False`, `Responding=True`).
5. **Installer Packaging (`T3` build):**
   - `:desktop:packageExe` → `desktop/build/compose/binaries/main/exe/Velune-1.1.2.exe` (`BUILD SUCCESSFUL`).
   - `:desktop:packageMsi` → `desktop/build/compose/binaries/main/msi/Velune-1.1.2.msi` (`BUILD SUCCESSFUL`).
6. **Regression (`T6`):**
   - `git status -s .github/workflows/android-release.yml` returned empty (Android workflow intact).

---

## 3. Not Executed / Pending Interactive or Remote Verification

- **T3 (Interactive GUI Install / Uninstall / Shortcuts):** Both `Velune-1.1.2.exe` and `Velune-1.1.2.msi` built cleanly, but interactive GUI installation, Start Menu/Desktop shortcut clicks, upgrade over a previous MSI, and uninstallation were not executed in this headless session.
- **T5 (Remote GitHub Actions Execution):** Remote `workflow_dispatch` and `v*` tag release creation on GitHub were **not** triggered automatically (per release policy requiring user approval before pushing tags).

---

## 4. Issues Fixed Across Phases 1–4

1. **Audio Playback & Process Lifecycle:** Eliminated overlapping `ffplay` processes (`AtomicLong` generation token + `processLock`), added JVM shutdown hook + descendant cleanup on window close, debounced volume restarts, and added HTTP reconnect + stream re-resolution so tracks play to completion.
2. **Packaged `ffplay.exe` Resolution:** Updated `DesktopAudioPlayer.findFfplayExecutable()` to resolve `ffplay.exe` from `compose.application.resources.dir` first.
3. **Per-User AppData Storage:** Updated `DesktopStorage` to store `state.json` in `%LOCALAPPDATA%\Velune\state.json` (with automatic migration from `~/.velune-desktop/state.json`).
4. **Icons & Metadata:** Generated `velune.ico`, `velune.png`, and `desktop/src/main/resources/velune.png`, wired window icons, and configured `vendor`, `licenseFile`, `includeAllModules = true`, `upgradeUuid`, and shortcuts in `desktop/build.gradle.kts`.
5. **Deterministic CI Tests:** Added `DesktopPackagingTest.kt` so CI does not rely on live YouTube network calls.

---

## 5. Remaining Limitations

1. **Unsigned Windows Installers:** Without `WINDOWS_CERT_BASE64` / `WINDOWS_CERT_PASSWORD` secrets configured, `.exe` and `.msi` installers are unsigned and Windows SmartScreen may show an "Unknown publisher" prompt.
2. **Gitignored `desktop/appResources/`:** `ffplay.exe` (~99.5 MB) is staged locally/in CI rather than committed to Git.
3. **Local Android SDK:** This local machine lacks Android SDK 36 platforms/build-tools, so Android APK builds must run on GitHub Actions (`android-release.yml`) or a machine with SDK 36.

---

## 6. Build Artifact Locations (Local QA Build)

- **Portable App Image:** `desktop/build/compose/binaries/main/app/Velune/Velune.exe`
- **Windows EXE Installer:** `desktop/build/compose/binaries/main/exe/Velune-1.1.2.exe`  
  SHA-256: `e51e6353394858eff356b084699491275f419ff00aff44d81812fde39fe81e84`
- **Windows MSI Installer:** `desktop/build/compose/binaries/main/msi/Velune-1.1.2.msi`  
  SHA-256: `c9dc34fd56ac37085d8e54159aa80ddef8b464bdc24a8dbe4a633235f5b8a4d5`

---

## 7. Final Release Readiness Status

**Status: READY FOR MANUAL CI DRY-RUN (`workflow_dispatch`) & INSTALLER SMOKE-TEST**

- All local builds, clean-environment portable execution, bundled runtime/FFmpeg checks, offline tests, and regression checks **passed**.
- Recommended next steps before publishing a public tag:
  1. Run `Velune-1.1.2.msi` or `Velune-1.1.2.exe` once interactively on Windows to confirm Start Menu/Desktop shortcuts and uninstall.
  2. Commit and push to GitHub, run **Actions → Desktop Release (Windows) → Run workflow** (`workflow_dispatch`) to verify CI artifact generation and `SHA256SUMS.txt`.
  3. When satisfied, push a `v*` tag (e.g., `git tag v1.1.3 && git push origin v1.1.3`) to publish the GitHub Release.
