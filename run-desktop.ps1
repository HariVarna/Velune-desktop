[CmdletBinding(DefaultParameterSetName = "Run")]
param(
    [Parameter(ParameterSetName = "Build")]
    [switch]$Build,

    [Parameter(ParameterSetName = "Package")]
    [switch]$Package,

    [Parameter(ParameterSetName = "LaunchRelease")]
    [switch]$LaunchRelease,

    [Parameter(ParameterSetName = "Help")]
    [switch]$Help
)

$ErrorActionPreference = "Stop"
$RepoRoot = $PSScriptRoot
$GradleWrapper = Join-Path $RepoRoot "gradlew.bat"

function Show-Usage {
    Write-Host @"
Velune Desktop Launcher

Usage:
  .\run-desktop.ps1                 Launch Velune Desktop in development mode (:desktop:run)
  .\run-desktop.ps1 -Build          Compile and build the desktop module (:desktop:jar)
  .\run-desktop.ps1 -Package        Build the portable Windows app image (:desktop:createDistributable)
  .\run-desktop.ps1 -LaunchRelease  Launch the packaged Velune.exe with its bundled runtime
  .\run-desktop.ps1 -Help           Show this help message
"@
}

function Test-Java21 {
    param([string]$JavaExe)
    if (-not $JavaExe -or -not (Test-Path -LiteralPath $JavaExe)) {
        return $false
    }
    try {
        $prevPref = $ErrorActionPreference
        $ErrorActionPreference = "Continue"
        $out = (& $JavaExe -version 2>&1) | Out-String
        $ErrorActionPreference = $prevPref
        return ($out -match 'version\s+"21(\.|")')
    } catch {
        return $false
    }
}

function Resolve-Java21Home {
    # 1. Existing JAVA_HOME
    if ($env:JAVA_HOME) {
        $candidate = Join-Path $env:JAVA_HOME "bin\java.exe"
        if (Test-Java21 $candidate) {
            return $env:JAVA_HOME
        }
    }

    # 2. java.exe currently on PATH
    $pathJava = Get-Command "java.exe" -ErrorAction SilentlyContinue
    if ($pathJava -and (Test-Java21 $pathJava.Source)) {
        return (Split-Path (Split-Path $pathJava.Source -Parent) -Parent)
    }

    # 3. Common JDK installation and Gradle cache locations
    $searchRoots = @(
        (Join-Path $env:USERPROFILE ".gradle\jdks"),
        (Join-Path $env:USERPROFILE ".jdks"),
        "C:\Program Files\Eclipse Adoptium",
        "C:\Program Files\Java",
        "C:\Program Files\Microsoft",
        "C:\Program Files\JetBrains"
    )

    foreach ($root in $searchRoots) {
        if (-not (Test-Path -LiteralPath $root)) { continue }
        $exes = Get-ChildItem -LiteralPath $root -Recurse -Filter "java.exe" -ErrorAction SilentlyContinue
        foreach ($exe in $exes) {
            if (Test-Java21 $exe.FullName) {
                return (Split-Path (Split-Path $exe.FullName -Parent) -Parent)
            }
        }
    }

    return $null
}

function Invoke-GradleTasks {
    param([string[]]$Tasks)

    if (-not (Test-Path -LiteralPath $GradleWrapper)) {
        Write-Error "Gradle wrapper not found at '$GradleWrapper'. Run this script from the repository root."
        exit 1
    }

    $jdkHome = Resolve-Java21Home
    if (-not $jdkHome) {
        Write-Host ""
        Write-Host "ERROR: Java 21 (JDK 21) was not found." -ForegroundColor Red
        Write-Host "Install Temurin JDK 21 and re-run:"
        Write-Host "  winget install EclipseAdoptium.Temurin.21.JDK"
        Write-Host "Or download from: https://adoptium.net/temurin/releases/?version=21"
        Write-Host ""
        exit 1
    }

    $env:JAVA_HOME = $jdkHome
    $jdkBin = Join-Path $jdkHome "bin"
    if ($env:PATH -notlike "$jdkBin*") {
        $env:PATH = "$jdkBin;$env:PATH"
    }

    # Warn if ffplay is missing in dev mode
    $bundledFfplay = Join-Path $RepoRoot "desktop\appResources\windows\ffplay.exe"
    $wingetFfplay = Join-Path $env:LOCALAPPDATA "Microsoft\WinGet\Links\ffplay.exe"
    $pathFfplay = Get-Command "ffplay.exe" -ErrorAction SilentlyContinue
    if (-not (Test-Path -LiteralPath $bundledFfplay) -and -not (Test-Path -LiteralPath $wingetFfplay) -and -not $pathFfplay) {
        Write-Warning "ffplay.exe was not found. Audio playback requires FFmpeg (winget install Gyan.FFmpeg.Essentials)."
    }

    Push-Location -LiteralPath $RepoRoot
    try {
        & $GradleWrapper @Tasks
        $code = $LASTEXITCODE
        if ($code -ne 0) {
            Write-Host "Velune Desktop failed with exit code $code." -ForegroundColor Red
            exit $code
        }
    } finally {
        Pop-Location
    }
}

if ($Help) {
    Show-Usage
    exit 0
}

if ($LaunchRelease) {
    $candidates = @(
        (Join-Path $RepoRoot "desktop\build\compose\binaries\main-release\app\Velune\Velune.exe"),
        (Join-Path $RepoRoot "desktop\build\compose\binaries\main\app\Velune\Velune.exe")
    )
    $releaseExe = $candidates | Where-Object { Test-Path -LiteralPath $_ } | Select-Object -First 1
    if (-not $releaseExe) {
        Write-Host "ERROR: Packaged Velune Desktop application was not found." -ForegroundColor Red
        Write-Host "Build the Windows application image first with:"
        Write-Host "  .\run-desktop.ps1 -Package"
        exit 1
    }

    $workingDir = Split-Path $releaseExe -Parent
    Write-Host "Launching packaged Velune from '$releaseExe'..."
    $proc = Start-Process -FilePath $releaseExe -WorkingDirectory $workingDir -PassThru
    $proc.WaitForExit()
    exit $proc.ExitCode
}

if ($Build) {
    Invoke-GradleTasks -Tasks @(":desktop:jar", "--no-daemon")
    exit 0
}

if ($Package) {
    Invoke-GradleTasks -Tasks @(":desktop:createDistributable", "--no-daemon")
    Write-Host "Packaged application image ready at: $(Join-Path $RepoRoot 'desktop\build\compose\binaries\main\app\Velune\Velune.exe')"
    exit 0
}

# Default: Launch development application
Invoke-GradleTasks -Tasks @(":desktop:run")
exit 0
