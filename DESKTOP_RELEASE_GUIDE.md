# Velune Desktop — Automated Release Guide

This guide explains how Windows desktop releases are built, verified, and published from the `HariVarna/Velune-desktop` fork.

> **Fork Notice:** Releases published from `HariVarna/Velune-desktop` are independent fork releases and are **not** official upstream releases from `nikhilvishwakarma00/Velune`.

---

## 1. Release Workflow

The desktop CI/CD pipeline is defined in [.github/workflows/desktop-release.yml](.github/workflows/desktop-release.yml) (separate from [.github/workflows/android-release.yml](.github/workflows/android-release.yml)).

### Triggers
- **Tag push (`v*`):** Pushing a tag such as `v1.1.3` (or `v1.1.3-rc1` for a prerelease) builds all Windows distributions, verifies outputs, generates `SHA256SUMS.txt`, and publishes a GitHub Release.
- **Manual (`workflow_dispatch`):** Runs the full validation, packaging, and checksum steps and uploads the outputs as **workflow artifacts** without creating a public GitHub Release.

---

## 2. Generated Release Assets

For a tag `v{VERSION}` (e.g. `v1.1.3`), the workflow produces:
- `Velune-Setup-{VERSION}.exe` — Windows EXE installer
- `Velune-{VERSION}.msi` — Windows MSI installer
- `Velune-Portable-{VERSION}.zip` — Portable Windows archive (extract and run `Velune.exe`)
- `SHA256SUMS.txt` — SHA-256 checksums for all release assets

All three distributions bundle a self-contained **JDK 21** runtime and **`ffplay.exe`**, so end users do not need Java or FFmpeg preinstalled.

---

## 3. Versioning

- Update `veluneDesktopVersion` in [gradle.properties](gradle.properties) (for example `1.1.3`) before tagging a release.
- In CI, the workflow derives `jpackage_version` (`MAJOR.MINOR.PATCH`) and `asset_version` directly from the pushed `v*` tag so the installer metadata and asset filenames stay synchronized.
- Tags containing `-` (e.g. `v1.1.3-rc1`) are automatically marked as **prereleases** on GitHub.

---

## 4. Code Signing (Optional)

- By default, Windows installers are built **unsigned** (no secrets required) and clearly labeled as unsigned in the release notes.
- To enable Authenticode signing in CI, add these repository secrets in **Settings → Secrets and variables → Actions**:
  - `WINDOWS_CERT_BASE64`: Base64-encoded `.pfx` code-signing certificate
  - `WINDOWS_CERT_PASSWORD`: Certificate password

---

## 5. How to Publish a Release

1. Update `veluneDesktopVersion` in [gradle.properties](gradle.properties) and commit your changes.
2. Create and push a version tag:
   ```bash
   git tag v1.1.3
   git push origin v1.1.3
   ```
3. Monitor **Actions → Desktop Release (Windows)** until all assets are attached to the GitHub Release.
