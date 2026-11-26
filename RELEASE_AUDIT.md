# Velune Desktop — Phase 1 Release Audit

**Repository:** `https://github.com/HariVarna/Velune-desktop`  
**Audit Date:** 2026-11-26  
**Scope:** `:desktop` Compose Multiplatform Desktop application and shared JVM modules (no implementation changes made in Phase 1).

---

## 1. Existing Architecture

### Module Layout (`settings.gradle.kts`)
- **Android App:** `:app` (`com.android.application`, Android-only)
- **Desktop App:** `:desktop` (`org.jetbrains.kotlin.jvm` + `org.jetbrains.compose`)
- **Shared Pure-Kotlin JVM Modules (`jvmToolchain(21)`):**
  - `:innertube` (YouTube Music InnerTube API + `NewPipeExtractor` + `Rhino` + `OkHttp` + `Ktor`)
  - `:lrclib` (LrcLib synced lyrics via Ktor CIO)
  - `:kugou` (KuGou lyrics via Ktor)
  - `:betterlyrics` (BetterLyrics TTML via Ktor OkHttp)
  - `:simpmusic` (SimpMusic lyrics via Ktor CIO)
  - `:canvas` (Canvas artwork/video metadata via Ktor OkHttp)
  - `:kizzy`, `:lastfm` (currently used by `:app` only)

### `:desktop` Source Structure (`desktop/src/main/kotlin/com/nikhil/yt/desktop/`)
- `Main.kt`: Compose `application {}` entry point, main `Window`, `PortableMiniPlayerWindow` host, global keyboard shortcuts (`F11`, `Esc`, `Ctrl+Arrows`, `Ctrl+Space`, `Ctrl+L`, `Ctrl+M`, `Ctrl++`/`Ctrl+-`), top search bar, navigation layout (`RAIL`, `SIDEBAR`, `TOP_TABS`), `AnimatedContent` screen router, and `NowPlayingOverlay`.
- `DesktopAppState.kt`: `DesktopAppController` managing navigation backstack, Home/Explore/Search/Detail loading, `ForYou` suggestion scoring + `YouTube.next` radio queue, playback state, artwork palette extraction, and multi-provider lyrics fetching.
- `DesktopAudioPlayer.kt`: Thread-safe audio player using `AtomicLong` generation tokens, `processLock`, `activeProcesses` tracking, `Runtime` shutdown hook, and `ffplay` subprocess playback with HTTP reconnects and stream re-resolution.
- `DesktopStorage.kt`: JSON persistence at `~/.velune-desktop/state.json` (independent of working directory).
- `DesktopLyricsEngine.kt`: Multi-provider lyrics resolver (`BetterLyrics` TTML → `SimpMusic` → `LrcLib` → `KuGou` → `YouTube.lyrics`).
- `VeluneTheme.kt`: Material 3 tonal theme engine (`com.github.Kyant0:m3color:2025.4`), `Modern` / `Retro` / `Pixelated` styles, Dark / Light / Pure Black modes, spring-animated `ColorScheme`, and `extractArtworkPalette` (`ImageIO`).
- `VeluneComponents.kt`: `VelunePlayingIndicator`, `VeluneSeekBar` (`Squiggly` / `Default` / `Slim`), `HomeShimmerSkeleton`, `VeluneThumbnail`, `VeluneGridCard`, `VeluneSongRow`.
- `NowPlayingSheet.kt`: `MiniPlayerBar` (`Floating Pill` / `Glass` / `Classic`), `PortableMiniPlayerWindow` (`Floating Card` / `Mac Dynamic Island` with corner anchoring, collapse/expand spring animation, mini lyrics, theme & icon customization), and opaque `NowPlayingOverlay`.
- `VeluneScreens.kt`: `HomeScreen` (with Random FAB), `ExploreScreen`, `SearchScreen`, `LibraryScreen`, `SettingsScreen` (including `AccentColorWheelPicker`), `AlbumDetailScreen`, `PlaylistDetailScreen`, `ArtistDetailScreen`, `BrowseDetailScreen`.
- `desktop/src/test/kotlin/com/nikhil/yt/desktop/StreamProbeTest.kt`: Live network integration test (`YouTube.search` + `resolveStream` + `ffplay`).

---

## 2. Desktop Build Configuration

- **Root `build.gradle.kts`:** Declares `kotlin.jvm`, `compose.compiler`, `jetbrains.compose` (`1.10.0`), `kotlin.serialization` (`apply false`).
- **`gradle/libs.versions.toml`:** Kotlin `2.3.10`, Compose Multiplatform `1.10.0`, Ktor `3.4.1`, Coil `3.4.0`, Coroutines Swing `1.10.2`, Gradle `9.4.0`.
- **`gradle.properties`:**
  - `org.gradle.jvmargs=-Xmx4096M -Dfile.encoding=UTF-8 -Djava.awt.headless=true -XX:+UseParallelGC` (overridden for `:desktop` runtime via `jvmArgs += "-Djava.awt.headless=false"`).
  - `org.gradle.caching=false`, `org.gradle.parallel=true`.
- **`desktop/build.gradle.kts`:**
  - `jvmToolchain(21)`
  - `mainClass = "com.nikhil.yt.desktop.MainKt"`
  - `jvmArgs += listOf("-Djava.awt.headless=false", "-Xmx1024m")`
  - **Android-only dependencies in `:desktop`:** **None.** All `:desktop` dependencies are pure JVM or Compose Multiplatform.
  - **Deprecated Gradle accessors noted in build output:** `compose.material3`, `compose.materialIconsExtended`, `compose.components.resources`.

---

## 3. Current Execution Workflow

- **`run-desktop.ps1` / `run-desktop.bat`:** Searches for `java.exe` in `JAVA_HOME`, `~/.gradle/jdks`, `~/.jdks`, `C:\Program Files\Java`, and `C:\Program Files\Eclipse Adoptium`, then runs `.\gradlew.bat :desktop:run`.
- **Runtime behavior:** Requires a full JDK 21 + Gradle + internet access on first run; audio playback requires `ffplay` on `PATH` or at `%LOCALAPPDATA%\Microsoft\WinGet\Links\ffplay.exe`.
- **Working-directory sensitivity:** App persistence (`~/.velune-desktop/state.json`) and `YouTube` API calls do **not** depend on CWD. Only `run-desktop.bat` / `run-desktop.ps1` use `$PSScriptRoot` / `%~dp0` to locate `gradlew.bat`.

---

## 4. Current Packaging Capabilities

`desktop/build.gradle.kts` currently configures:
```kotlin
nativeDistributions {
    targetFormats(TargetFormat.Msi, TargetFormat.Exe, TargetFormat.Dmg, TargetFormat.Deb)
    packageName = "Velune"
    packageVersion = "1.1.2"
    description = "Velune YouTube Music Desktop Client"
}
```
- Can invoke `jpackage` via Compose Desktop plugin to produce app images (`createDistributable`, `createReleaseDistributable`), uber JARs, and native installers (`.exe`/`.msi` on Windows with WiX Toolset, `.deb` on Linux, `.dmg` on macOS).
- Local `git tag -l` shows `v1.1.2`, `v1.1.3`.
- `.github/workflows/` currently contains only `android-release.yml` (no desktop release workflow).

---

## 5. Missing Release Requirements

1. **Application Icons:** No `.ico` (Windows), `.icns` (macOS), or `.png` (Linux / window taskbar icon) exists under `desktop/src/main/resources/` or `desktop/icons/`. Only `fastlane/metadata/android/en-US/images/icon.png` exists in the repo.
2. **Window Icon:** `Main.kt` (`Window(...)`) and `PortableMiniPlayerWindow` do not set an application/window icon.
3. **Bundled Audio Runtime (`ffplay`):** Packaged users without `ffmpeg`/`ffplay` installed globally cannot play any audio. `ffplay` must either be bundled via `compose.desktop.application.nativeDistributions.appResourcesRootDir` (using `compose.application.resources.dir` at runtime) or clearly handled with a fallback/install prompt.
4. **Windows Installer Metadata:** Missing `vendor`, `copyright`, `licenseFile`, `windows.upgradeUuid`, `windows.menuGroup`, `windows.shortcut`, `windows.dirChooser`, `windows.perUserInstall`, and `windows.iconFile`.
5. **macOS & Linux Metadata:** Missing `macOS.bundleID`, `macOS.iconFile`, `linux.iconFile`, `linux.packageName`, `linux.menuGroup`.
6. **Desktop ProGuard Rules:** `packageRelease*` / `createReleaseDistributable` tasks run `proguardReleaseJars`. Without desktop-specific ProGuard keep rules, `kotlinx.serialization`, `Ktor`, `NewPipeExtractor`, `Rhino`, `Coil3`, and `m3color` can fail at runtime in release builds.
7. **Desktop CI/Release Workflow:** No GitHub Actions workflow exists to build, test, package, and publish desktop releases.
8. **README Desktop Download/Install Section:** `README.md` only documents running locally via `run-desktop.ps1` / `run-desktop.bat`, not downloadable desktop releases.

---

## 6. Identified Bugs and Risks

1. **Packaged `ffplay` resolution:** `DesktopAudioPlayer.findFfplayExecutable()` only checks `%LOCALAPPDATA%\Microsoft\WinGet\Links\ffplay.exe` and `"ffplay"` on `PATH`. It does not check `System.getProperty("compose.application.resources.dir")` for a bundled `ffplay` binary.
2. **ProGuard stripping in `packageRelease*`:** `NewPipeExtractor` + `Rhino` + `Ktor` + `kotlinx.serialization` rely on reflection/serializers that must be kept in `proguardReleaseJars`.
3. **WiX Toolset requirement on Windows:** `packageExe` / `packageMsi` require WiX v3 (`candle.exe` / `light.exe`). Local machines without WiX can still build `createDistributable` / `createReleaseDistributable` (portable app image), while GitHub `windows-latest` runners include WiX.
4. **Live network test in `desktop/src/test`:** `StreamProbeTest.kt` hits live YouTube endpoints and launches `ffplay`, which can be flaky in CI without `ffplay` or under YouTube rate limits.
5. **JVM Runtime Modules (`jlink`):** `jpackage` bundles a minimal JRE; `java.net.http`, `jdk.crypto.ec` (TLS/HTTPS for Ktor/OkHttp/NewPipe), `java.desktop` (AWT/Swing/ImageIO), and `java.naming` must be included (e.g., `includeAllModules = true` or explicit `modules(...)` validated by `suggestRuntimeModules`).
6. **Version alignment:** `desktop/build.gradle.kts` hardcodes `packageVersion = "1.1.2"`, which should align with release tags.

---

## 7. Required Modifications (Next Phases)

1. Generate/convert `fastlane/metadata/android/en-US/images/icon.png` into desktop icons (`velune.ico`, `velune.icns`, `velune.png`) under `desktop/icons/` and wire them into `nativeDistributions` + `Main.kt` / `PortableMiniPlayerWindow` window icons.
2. Bundle or resolve `ffplay` from `compose.application.resources.dir` in `DesktopAudioPlayer.kt` (falling back to WinGet/`PATH`) and configure `appResourcesRootDir` in `desktop/build.gradle.kts`.
3. Add complete `nativeDistributions` metadata (`vendor`, `licenseFile`, `includeAllModules = true` or verified `modules(...)`, `windows`, `macOS`, `linux` blocks, and `desktop/proguard-rules.pro` for `proguardReleaseJars`).
4. Add a deterministic offline unit test in `desktop/src/test` (keeping `StreamProbeTest` tagged/gated or separate) so CI test runs are reliable.
5. Create `.github/workflows/desktop-release.yml` (preserving `android-release.yml` untouched) to build and publish Windows (`.exe`/`.msi`/portable `.zip`), Linux (`.deb`/`.tar.gz`), and macOS (`.dmg`) release assets on desktop release tags (e.g., `desktop-v*` or `v*`).
6. Update `README.md` with desktop download & installation instructions.

---

## 8. Proposed Windows Release Architecture

- **Primary Installer:** `.msi` (and/or `.exe`) built via `:desktop:packageReleaseMsi` / `:desktop:packageReleaseExe` on `windows-latest` (WiX preinstalled), with fixed `upgradeUuid`, Start Menu shortcut, desktop shortcut, and `perUserInstall = true` (no admin UAC required).
- **Portable Release:** `.zip` archive of `desktop/build/compose/binaries/main-release/app/Velune/` from `:desktop:createReleaseDistributable` so users can extract and run `Velune.exe` without installing.
- **Bundled Runtime:** Self-contained JDK 21 runtime image (`jlink`) + bundled `ffplay.exe` inside `appResources/windows/` so zero external dependencies (Java or FFmpeg) are required on end-user Windows PCs.

---

## 9. Linux and macOS Release Considerations

- **Linux (`ubuntu-latest`):**
  - Build `.deb` via `:desktop:packageReleaseDeb` and portable `.tar.gz` via `:desktop:createReleaseDistributable`.
  - Bundle `ffplay` in `appResources/linux/` (with `chmod +x` on startup or in CI) and/or document `ffmpeg` package dependency in `.deb`.
  - Set `linux.packageName = "velune"`, `linux.menuGroup = "AudioVideo"`, and `.png` icon.
- **macOS (`macos-latest`):**
  - Build `.dmg` via `:desktop:packageReleaseDmg` with `macOS.bundleID = "com.nikhil.yt.desktop"` and `.icns` icon.
  - Bundle `ffplay` in `appResources/macos/` (matching runner arch `arm64`/`x64`).
  - Unsigned `.dmg` builds trigger macOS Gatekeeper ("unidentified developer") unless Apple Developer notarization secrets are configured; document `xattr -cr /Applications/Velune.app` for unsigned fork builds.

---

## 10. Exact Gradle Tasks Available for Packaging

Verified from `.\gradlew.bat :desktop:tasks --group="compose desktop" --no-daemon`:

- **Distributable App Image (no installer tool required):**
  - `:desktop:createDistributable`
  - `:desktop:createReleaseDistributable`
- **Windows:**
  - `:desktop:packageExe` / `:desktop:packageReleaseExe`
  - `:desktop:packageMsi` / `:desktop:packageReleaseMsi`
- **macOS:**
  - `:desktop:packageDmg` / `:desktop:packageReleaseDmg`
  - `:desktop:notarizeDmg` / `:desktop:notarizeReleaseDmg`
- **Linux:**
  - `:desktop:packageDeb` / `:desktop:packageReleaseDeb`
- **Current OS / Uber JAR / Utilities:**
  - `:desktop:packageDistributionForCurrentOS` / `:desktop:packageReleaseDistributionForCurrentOS`
  - `:desktop:packageUberJarForCurrentOS` / `:desktop:packageReleaseUberJarForCurrentOS`
  - `:desktop:run` / `:desktop:runRelease` / `:desktop:runDistributable` / `:desktop:runReleaseDistributable`
  - `:desktop:proguardReleaseJars`
  - `:desktop:suggestRuntimeModules`
  - `:desktop:checkRuntime`
