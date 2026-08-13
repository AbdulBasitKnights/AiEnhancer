param([Parameter(ValueFromRemainingArguments = $true)] $GradleArgs)

$env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"

Set-Location 'c:\Users\Terafort\StudioProjects\Ai-Enhancer'
Write-Output ("JAVA: " + (& java -version 2>&1 | Select-Object -First 1))
Write-Output ("ARGS: " + ($GradleArgs -join ' '))
& .\gradlew.bat @GradleArgs
Write-Output ("GRADLE_EXIT=" + $LASTEXITCODE)
exit $LASTEXITCODE
