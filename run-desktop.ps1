$ErrorActionPreference = "Stop"

if (-not $env:JAVA_HOME -or -not (Test-Path "$env:JAVA_HOME\bin\java.exe")) {
    $candidates = Get-ChildItem "$env:USERPROFILE\.gradle\jdks", "$env:USERPROFILE\.jdks", "C:\Program Files\Java", "C:\Program Files\Eclipse Adoptium" -ErrorAction SilentlyContinue |
        ForEach-Object { Get-ChildItem $_.FullName -Recurse -Filter "java.exe" -ErrorAction SilentlyContinue } |
        Select-Object -First 1
    if ($candidates) {
        $env:JAVA_HOME = Split-Path (Split-Path $candidates.FullName -Parent) -Parent
        $env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
    }
}

& "$PSScriptRoot\gradlew.bat" :desktop:run
