$ErrorActionPreference = "Stop"
$GradleZipUrl = "https://services.gradle.org/distributions/gradle-8.9-bin.zip"
$GradleZip = "$PSScriptRoot\gradle-8.9-bin.zip"
$GradleHome = "$PSScriptRoot\gradle-8.9"
Invoke-WebRequest -Uri $GradleZipUrl -OutFile $GradleZip
Expand-Archive -Path $GradleZip -DestinationPath $PSScriptRoot -Force
Remove-Item $GradleZip -Force
& "$GradleHome\bin\gradle.bat" -p "$PSScriptRoot" wrapper --gradle-version 8.9 --no-daemon
& "$PSScriptRoot\gradlew.bat" clean assembleDebug --no-daemon
