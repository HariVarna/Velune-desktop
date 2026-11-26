
<div align="center">

 <img src="https://raw.githubusercontent.com/HariVarna/Velune-desktop/main/fastlane/metadata/android/en-US/images/icon.png" width="110" />

</div>

# 🌌 Velune
<div align="center">

<pre>
██╗   ██╗███████╗██╗     ██╗   ██╗███╗   ██╗███████╗
██║   ██║██╔════╝██║     ██║   ██║████╗  ██║██╔════╝
██║   ██║█████╗  ██║     ██║   ██║██╔██╗ ██║█████╗
╚██╗ ██╔╝██╔══╝  ██║     ██║   ██║██║╚██╗██║██╔══╝
 ╚████╔╝ ███████╗███████╗╚██████╔╝██║ ╚████║███████╗
  ╚═══╝  ╚══════╝╚══════╝ ╚═════╝ ╚═╝  ╚═══╝╚══════╝
</pre>

</div>

### 🎧 The YouTube Music app you always wanted

🚫 No Ads • 💰 No Subscription • ⚡ Full Control  

<div align="center">

![Release](https://img.shields.io/github/v/release/HariVarna/Velune-desktop?style=for-the-badge&logo=github&logoColor=white&labelColor=18181B&color=3B82F6)
![Downloads](https://img.shields.io/github/downloads/HariVarna/Velune-desktop/total?style=for-the-badge&logo=github&logoColor=white&labelColor=18181B&color=10B981)
![Stars](https://img.shields.io/github/stars/HariVarna/Velune-desktop?style=for-the-badge&logo=github&logoColor=white&labelColor=18181B&color=F59E0B)
![License](https://img.shields.io/github/license/HariVarna/Velune-desktop?style=for-the-badge&labelColor=18181B&color=EF4444)

<br>

![Kotlin](https://img.shields.io/badge/Kotlin-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)
![Jetpack Compose](https://img.shields.io/badge/Compose-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white)
![Node.js](https://img.shields.io/badge/Node.js-339933?style=for-the-badge&logo=nodedotjs&logoColor=white)
![Architecture](https://img.shields.io/badge/Architecture-MVVM-52525B?style=for-the-badge)

</div>


<br>
<hr>

⚠️ Fork & Upstream Notice

This repository (`HariVarna/Velune-desktop`) is an independent **fork** of [Velune](https://github.com/nikhilvishwakarma00/Velune) by Nikhil Vishwakarma, adding a Compose Multiplatform Desktop client alongside the Android app.

Releases published from `HariVarna/Velune-desktop` are **fork releases** and are **not** official upstream Velune releases. For official upstream Android releases, visit https://github.com/nikhilvishwakarma00/Velune.

<hr>
<br>

## 📥 Download Now


<div align="center">

<h2>Fork Release</h2>

<table>
  <thead>
    <tr>
      <th align="center">Obtainium (Fork)</th>
      <th align="center">GitHub Releases (Fork)</th>
    </tr>
  </thead>
  <tbody>
    <tr>
      <td align="center">
        <a href="https://apps.obtainium.imranr.dev/redirect?r=obtainium://add/https://github.com/HariVarna/Velune-desktop">
          <img src="https://raw.githubusercontent.com/ImranR98/Obtainium/main/assets/graphics/badge_obtainium.png" height="50" alt="Get Fork on Obtainium">
        </a>
      </td>
      <td align="center">
        <a href="https://github.com/HariVarna/Velune-desktop/releases/latest">
          <img src="assets/badge_github.png" height="50" alt="Get Fork on GitHub">
        </a>
      </td>
    </tr>
  </tbody>
</table>

</div>


## 🚀 Why Velune?

Velune is not just another music player — it's a **complete reimagination of YouTube Music**.

* ⚡ **Faster** than the official app
* 🎵 Built for audiophiles & power users
* 📦 True offline-first experience
* 🎨 Stunning **Material You** interface

> 💡 This is YouTube Music — *but unlocked*

---

## 🎯 Highlight Feature

### 🎤 Real-Time Synced Lyrics

Experience lyrics like never before:

- Word-by-word sync  
- Smooth animations  
- Translation support  
- Fully immersive playback  

---

## 📸 Preview

<div align="center">

<img src="screenshots/Home.png" width="140"/>
<img src="screenshots/Now Playing.png" width="140"/>
<img src="screenshots/History.png" width="140"/>
<img src="screenshots/Stats.png" width="140"/>

<br/>

<img src="screenshots/Library.png" width="140"/>
<img src="screenshots/Settings.png" width="140"/>
<img src="screenshots/About.png" width="140"/>

</div>



---

## ✨ Features

### 🎵 Core Experience
- Ad-Free Playback  
- Full Library Sync  
- Offline Caching (Encrypted)  
- Background Playback  

### 🔊 Audio Engine
- Gapless Playback  
- Crossfade Engine  
- Silence Skipping  
- Loudness Normalization (EBU R128)  
- Tempo & Pitch Control  
- System EQ Integration  

### 🎨 UI & Discovery
- Material You (Dynamic Colors)  
- Synced Lyrics + Translation  
- Discord Rich Presence  
- Personalized Home Feed  
- Year in Review Stats  
- Custom Animated Loader  

---

## 🧠 Architecture

Built using **modern Android engineering principles**:

- MVVM + Clean Architecture  
- Unidirectional Data Flow (UDF)  
- Modular & scalable codebase  

---

## 🛠 Tech Stack

| Layer | Stack |
|------|------|
| Language | Kotlin |
| UI | Jetpack Compose + Material 3 |
| Audio | Media3 / ExoPlayer |
| DI | Hilt |
| Database | Room (Encrypted) |
| Networking | Ktor + Retrofit |
| Async | Coroutines + Flow |
| Build | Gradle KTS |

---

## 📂 Project Structure

```bash
velune/
├── app/
├── desktop/
├── innertube/
├── lrclib/
├── kizzy/
├── canvas/
├── lastfm/
├── kugou/
```

Getting Started \
Requirements\
Android Studio Ladybug+\
JDK 21
Android SDK 36 \
Run Locally\
Bash
```
git clone https://github.com/HariVarna/Velune-desktop.git
cd Velune-desktop
```
Open in Android Studio → Sync → Run ▶ (or run `./run-desktop.ps1` / `run-desktop.bat` for the Desktop client)

---

## 📦 Release Guide (Fork)

> **Note:** Releases built from `HariVarna/Velune-desktop` are **fork releases** and are **not** official releases from the upstream [nikhilvishwakarma00/Velune](https://github.com/nikhilvishwakarma00/Velune) repository.

### Prerequisites
- **JDK 21** and **Android SDK 36** (`compileSdk = 36`, `targetSdk = 36`, `minSdk = 26`).
- Before tagging or building a release, update [app/build.gradle.kts](app/build.gradle.kts):
  - Increment `versionCode` (required by Android for upgrades).
  - Update `versionName` (e.g., `"1.1.3-fork"` — avoid trailing whitespace).

### Build a Signed Release APK Locally
1. Place your existing release keystore at `app/keystore/release.keystore` (ignored by `.gitignore`; never commit it).
2. Set your signing environment variables in your shell (never commit secret values):
   ```powershell
   $env:STORE_PASSWORD = "<your-store-password>"
   $env:KEY_ALIAS = "<your-key-alias>"
   $env:KEY_PASSWORD = "<your-key-password>"
   ```
3. Run validation and build the release APKs:
   ```powershell
   .\gradlew.bat :app:testUniversalDebugUnitTest :app:lintUniversalRelease --no-daemon
   .\gradlew.bat :app:assembleRelease --no-daemon
   ```
   *(Or build a single flavor such as `:app:assembleUniversalRelease` or `:app:assembleArm64Release`.)*

### Publish a GitHub Release
1. In **Settings → Secrets and variables → Actions**, configure the required repository secrets:
   - `KEYSTORE_BASE64` (base64-encoded contents of your `.keystore`/`.jks` file)
   - `STORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`
   - Optional `BuildConfig` secrets: `LASTFM_API_KEY`, `LASTFM_SECRET`, `TOGETHER_BEARER_TOKEN`
2. Push a version tag matching `v*` (for example `v1.1.3`, or `v1.1.3-rc1` to mark as a pre-release):
   ```bash
   git tag v1.1.3
   git push origin v1.1.3
   ```
3. The [Android Release workflow](.github/workflows/android-release.yml) validates, builds, signs, verifies, and publishes these assets on the GitHub Release:
   - `Velune-<tag>-universal.apk`
   - `Velune-<tag>-arm64-v8a.apk`
   - `Velune-<tag>-armeabi-v7a.apk`
   - `Velune-<tag>-x86.apk`
   - `Velune-<tag>-x86_64.apk`

*(You can also run **Actions → Android Release → Run workflow** to dry-run and download APK artifacts without publishing a GitHub Release.)*

-------------------------------------------------------------------------

## 💬 Community

Have a feature request, found a bug,
or just want to share your favorite music setups? Join the official Velune community!

* *Discord:* [Join the Velune Server](https://discord.gg/cJNHTdpP6H)
* *GitHub Discussions:* [Open a Discussion](https://github.com/nikhilvishwakarma00/Velune/discussions)



--------------------------------------------------------------------------------------------------------------


🙌 Credits\
Huge respect to these projects:

Archivetune -base framework

Metrolist

InnerTune

Kizzy

SimpMusic

BetterLyrics

⚖️ Legal
Velune is an independent client and is not affiliated with YouTube or Google.\
Please support artists through official platforms ❤️\
Licensed under GPL-3.0
 

💙 Built with passion by Nikhil\
⭐ Star the repo if Velune impressed you\
🚀 Help it reach more people

## 🔗 Star History

<div align="center">
  <a href="https://star-history.com/HariVarna/Velune-desktop&Date">
    <picture>
      <source media="(prefers-color-scheme: dark)" srcset="https://api.star-history.com/svg?repos=HariVarna/Velune-desktop&type=Date&theme=dark" />
      <source media="(prefers-color-scheme: light)" srcset="https://api.star-history.com/svg?repos=HariVarna/Velune-desktop&type=Date" />
      <img alt="Star History Chart" src="https://api.star-history.com/svg?repos=HariVarna/Velune-desktop&type=Date" />
    </picture>
  </a>

  <br/>
  <br/><br/>
</div>
 
