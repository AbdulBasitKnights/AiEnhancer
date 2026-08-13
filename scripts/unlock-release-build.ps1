$ErrorActionPreference = "Stop"

$projectRoot = Split-Path -Parent $PSScriptRoot
Set-Location $projectRoot

$jbrCandidates = @(
    "$env:LOCALAPPDATA\Programs\Android\Android Studio\jbr",
    "$env:ProgramFiles\Android\Android Studio\jbr",
    "$env:ProgramFiles\JetBrains\Android Studio\jbr"
)

foreach ($jbr in $jbrCandidates) {
    if (Test-Path "$jbr\bin\java.exe") {
        $env:JAVA_HOME = $jbr
        $env:PATH = "$jbr\bin;$env:PATH"
        break
    }
}

if (-not $env:JAVA_HOME) {
    Write-Error "JAVA_HOME not found. Install Android Studio JBR or set JAVA_HOME."
}

Write-Host "Stopping Gradle daemons..."
& .\gradlew.bat --stop | Out-Null
Start-Sleep -Seconds 2

Write-Host "Stopping Gradle worker processes..."
Get-CimInstance Win32_Process -Filter "Name='java.exe'" |
    Where-Object { $_.CommandLine -match "GradleWorkerMain|GradleDaemon" } |
    ForEach-Object {
        Stop-Process -Id $_.ProcessId -Force -ErrorAction SilentlyContinue
    }

Start-Sleep -Seconds 2

$lockedPath = Join-Path $projectRoot "app\build\intermediates\compile_and_runtime_not_namespaced_r_class_jar"
if (Test-Path $lockedPath) {
    Write-Host "Removing locked release resource intermediates..."
    Remove-Item -LiteralPath $lockedPath -Recurse -Force
}

Write-Host "Done. Run release build with:"
Write-Host "  .\gradlew.bat :app:assembleRelease --no-daemon"
