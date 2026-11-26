# Velune Desktop — Windows x64 Release Guide

This document describes how to build, verify, and install the **Windows x64 (AMD64 / x86_64)** release of Velune Desktop (`com.nikhil.yt.desktop.MainKt`).

---

## 1. Prerequisites

- **Operating System:** Windows x64 (`AMD64`).
- **JDK:** 64-bit (`amd64`) **JDK 21** (e.g., Eclipse Temurin 21).
- **Audio Resource (`ffplay.exe`):** Place 64-bit `ffplay.exe` at `desktop/appResources/windows/ffplay.exe` (gitignored) so it is bundled into the installer and portable image.
- **WiX Toolset:** Automatically downloaded/cached by Compose Multiplatform (`downloadWix` / `unzipWix`) when building `.exe` and `.msi`.
- End users installing the generated `.exe`, `.msi`, or portable `.zip` do **not** need Java, JDK, Gradle, Kotlin, Android Studio, or FFmpeg installed.

---

## 2. Exact Packaging Tasks & Build Command

### Build All Windows x64 Release Artifacts (Recommended)
```powershell
$env:JAVA_HOME = "C:\Users\KiTE\.gradle\jdks\eclipse_adoptium-21-amd64-windows.2"
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
.\gradlew.bat :desktop:packageWindowsX64Release --no-daemon
```

### Individual Gradle Tasks (`:desktop`)
- `:desktop:verifyWindowsX64Environment` — Verifies Windows OS + `amd64`/`x86_64` JDK 21
- `:desktop:packageExe` — Builds Windows x64 EXE setup installer
- `:desktop:packageMsi` — Builds Windows x64 MSI installer
- `:desktop:createDistributable` — Builds Windows x64 portable app image with bundled JDK 21 runtime
- `:desktop:packageWindowsX64PortableZip` — Archives the portable app image into a `.zip`
- `:desktop:packageWindowsX64Release` — Runs all of the above and outputs cleanly named x64 artifacts

*(Version is sourced from `veluneDesktopVersion` in [gradle.properties](gradle.properties), or overridden with `-PveluneDesktopVersion=1.1.3`.)*

---

## 3. Generated Artifact Locations

All final Windows x64 release artifacts are written to `desktop/build/releases/windows-x64/`:
- **EXE Setup Installer:** `desktop/build/releases/windows-x64/Velune-Setup-{VERSION}-x64.exe`
- **MSI Installer:** `desktop/build/releases/windows-x64/Velune-{VERSION}-x64.msi`
- **Portable ZIP:** `desktop/build/releases/windows-x64/Velune-Portable-{VERSION}-x64.zip`
- **Portable App Image:** `desktop/build/compose/binaries/main/app/Velune/Velune.exe`

---

## 4. Architecture Verification Procedure

Verify PE Machine headers (`0x8664` = `x64 (AMD64)`) on the installer, launcher, bundled JVM, and `ffplay.exe`:

```powershell
python -c "
import struct
def pe_arch(path):
    with open(path, 'rb') as f: data = f.read(4096)
    pe = struct.unpack_from('<I', data, 0x3C)[0]
    m = struct.unpack_from('<H', data, pe + 4)[0]
    return {0x8664: 'x64 (AMD64)', 0x014c: 'x86 (32-bit)', 0xAA64: 'ARM64'}.get(m, hex(m))
for p in [
    r'desktop\build\releases\windows-x64\Velune-Setup-1.1.2-x64.exe',
    r'desktop\build\compose\binaries\main\app\Velune\Velune.exe',
    r'desktop\build\compose\binaries\main\app\Velune\runtime\bin\server\jvm.dll',
    r'desktop\build\compose\binaries\main\app\Velune\app\resources\ffplay.exe',
]:
    print(p, '->', pe_arch(p))
"
```

---

## 5. Installation & Testing Instructions

1. **EXE / MSI Installer:**
   - Double-click `Velune-Setup-{VERSION}-x64.exe` (or `Velune-{VERSION}-x64.msi`).
   - Install per-user (no admin UAC required), verify the **Velune** Start Menu entry and Desktop shortcut, launch Velune, verify playback/search/data persistence (`%LOCALAPPDATA%\Velune\state.json`), and test uninstall via **Settings → Apps**.
2. **Portable ZIP:**
   - Extract `Velune-Portable-{VERSION}-x64.zip` anywhere outside the repository and run `Velune\Velune.exe`.

---

## 6. Known Limitations

- **Unsigned Installers:** Without an Authenticode code-signing certificate, Windows SmartScreen may show an "Unknown publisher" warning on first run.
- **Interactive GUI Installer Step:** In headless CI/CLI environments, interactive GUI installation, Start Menu/Desktop shortcut clicks, and interactive uninstall should be smoke-tested manually on a Windows x64 machine before public release.
