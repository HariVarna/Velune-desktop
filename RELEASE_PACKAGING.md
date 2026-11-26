# Velune Desktop — Production Packaging Guide

This document describes the production Windows desktop packaging setup for `:desktop` (`com.nikhil.yt.desktop.MainKt`) using Compose Multiplatform native distributions and a bundled JDK 21 runtime.

---

## 1. Configuration Overview

- **Version source:** `veluneDesktopVersion` in [gradle.properties](gradle.properties) (currently `1.1.2`), consumed by [desktop/build.gradle.kts](desktop/build.gradle.kts).
- **Application metadata:**
  - `packageName = "Velune"`
  - `mainClass = "com.nikhil.yt.desktop.MainKt"`
  - `vendor = "HariVarna (Fork of Velune by Nikhil)"`
  - `licenseFile = LICENSE` (GPL-3.0)
- **Icons:**
  - Windows installer/executable icon: `desktop/icons/velune.ico`
  - Linux icon: `desktop/icons/velune.png`
  - Runtime window/taskbar icon: `desktop/src/main/resources/velune.png` (loaded in `Main.kt` and `PortableMiniPlayerWindow`)
- **Windows installer options:**
  - Start Menu entry (`menu = true`, `menuGroup = "Velune"`)
  - Desktop shortcut (`shortcut = true`)
  - Per-user install without admin UAC (`perUserInstall = true`, `dirChooser = true`)
  - Stable `upgradeUuid = "8f4b2c1a-7d3e-4e9a-9b2f-6c1d8e5a3f01"`
- **Per-user data directory** ([DesktopStorage.kt](desktop/src/main/kotlin/com/nikhil/yt/desktop/DesktopStorage.kt)):
  - Windows: `%LOCALAPPDATA%\Velune\state.json`
  - macOS: `~/Library/Application Support/Velune/state.json`
  - Linux: `$XDG_DATA_HOME/Velune/state.json` (or `~/.local/share/Velune/state.json`)
  - Automatically migrates legacy `~/.velune-desktop/state.json` if present.

---

## 2. Launcher & Build Commands

### One-Command Launcher (`run-desktop.ps1` / `run-desktop.bat`)
```powershell
.\run-desktop.ps1                 # Launch development app (:desktop:run)
.\run-desktop.ps1 -Build          # Build desktop JAR (:desktop:jar)
.\run-desktop.ps1 -Package        # Build Windows portable app image (:desktop:createDistributable)
.\run-desktop.ps1 -LaunchRelease  # Run packaged Velune.exe directly (no Gradle)
.\run-desktop.ps1 -Help           # Show usage help
```

### Direct Gradle Commands

Set `JAVA_HOME` to JDK 21 if not already on `PATH`:

```powershell
$env:JAVA_HOME = "C:\Users\KiTE\.gradle\jdks\eclipse_adoptium-21-amd64-windows.2"
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
```

### Portable Application Image
```powershell
.\gradlew.bat :desktop:createDistributable --no-daemon
# Or ProGuard-minified release variant:
.\gradlew.bat :desktop:createReleaseDistributable --no-daemon
```

### Windows Installers (MSI & EXE)
```powershell
.\gradlew.bat :desktop:packageMsi --no-daemon
.\gradlew.bat :desktop:packageExe --no-daemon
# Or ProGuard-minified release variants:
.\gradlew.bat :desktop:packageReleaseMsi --no-daemon
.\gradlew.bat :desktop:packageReleaseExe --no-daemon
```

*(Other platforms: `:desktop:packageDmg` / `:desktop:packageReleaseDmg` on macOS, `:desktop:packageDeb` / `:desktop:packageReleaseDeb` on Linux.)*

---

## 3. Output Directories

- **Portable app image:**
  - `desktop/build/compose/binaries/main/app/Velune/` (contains `Velune.exe`, `runtime/`, and `app/`)
  - Release variant: `desktop/build/compose/binaries/main-release/app/Velune/`
- **Windows MSI installer:**
  - `desktop/build/compose/binaries/main/msi/Velune-1.1.2.msi` (verified)
  - Release variant: `desktop/build/compose/binaries/main-release/msi/`
- **Windows EXE installer:**
  - `desktop/build/compose/binaries/main/exe/`
  - Release variant: `desktop/build/compose/binaries/main-release/exe/`

---

## 4. Runtime & Native Component Bundling

- **Bundled JVM Runtime:** `includeAllModules = true` instructs `jlink` to package a complete self-contained JDK 21 runtime inside `runtime/`. End users do **not** need Java, JDK 21, Kotlin, Gradle, or Android Studio installed.
- **Bundled `ffplay` Audio Engine:**
  - `desktop/build.gradle.kts` sets `appResourcesRootDir` to `desktop/appResources` (gitignored so large binaries are not committed to Git).
  - When `desktop/appResources/windows/ffplay.exe` is present during packaging, Compose Desktop copies it into `Velune/app/resources/ffplay.exe`.
  - `DesktopAudioPlayer.findFfplayExecutable()` resolves `ffplay.exe` from `compose.application.resources.dir` first, falling back to WinGet or system `PATH`.
- **ProGuard Configuration:** [desktop/proguard-rules.pro](desktop/proguard-rules.pro) is wired into `buildTypes.release.proguard` for `proguardReleaseJars`.

---

## 5. Verification Performed

- Built `:desktop:createDistributable`: `desktop/build/compose/binaries/main/app/Velune/Velune.exe` and `app/resources/ffplay.exe` verified present (`BUILD SUCCESSFUL`).
- Launched `Velune.exe` from `%USERPROFILE%` (outside the repository directory): process remained running (`HasExited=False`, `Responding=True`).
- Built `:desktop:packageMsi`: `desktop/build/compose/binaries/main/msi/Velune-1.1.2.msi` generated (`BUILD SUCCESSFUL`).

---

## 6. Known Limitations

1. **Unsigned Windows Binaries:** The generated `.exe` / `.msi` are not Authenticode code-signed unless a Windows code-signing certificate is configured; Windows SmartScreen may show an "Unknown publisher" prompt on first launch.
2. **Gitignored `ffplay.exe` (`~99.5 MB`):** `desktop/appResources/` is excluded from Git because `ffplay.exe` is ~99.5 MB (near GitHub's 100 MB file limit). Local packagers or CI workflows must stage `ffplay` into `desktop/appResources/<os>/` before building distributables for bundled audio playback.
3. **macOS `.icns` icon:** `desktop/icons/velune.ico` and `velune.png` are generated; a `.icns` icon should be generated on macOS (or in CI with `iconutil`) before publishing macOS `.dmg` builds.
