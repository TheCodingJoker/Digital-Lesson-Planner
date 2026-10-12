param(
    [string]$JavaHome = $env:JAVA_HOME,
    [string]$JavaFxHome = $env:JAVAFX_HOME,
    [string]$InnoSetupCompiler = ""
)

$ErrorActionPreference = "Stop"

$projectDir = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$downloadDir = Join-Path $HOME "Downloads"
$outputName = "DigitalLessonPlanner-Setup.exe"
$javaHomeCandidates = @(
    $JavaHome,
    "C:\Program Files\Eclipse Adoptium\jdk-23.0.2.7-hotspot",
    "C:\Program Files\Java\jdk-23"
) | Where-Object { $_ }

if (-not $JavaHome) {
    $JavaHome = $javaHomeCandidates | Where-Object { Test-Path (Join-Path $_ "bin\jpackage.exe") } | Select-Object -First 1
}
if (-not $JavaHome -or -not (Test-Path (Join-Path $JavaHome "bin\jpackage.exe"))) {
    throw "JDK 23 or newer with jpackage is required. Set JAVA_HOME or pass -JavaHome."
}
$javaVersion = & (Join-Path $JavaHome "bin\jlink.exe") --version
if ($LASTEXITCODE -ne 0 -or $javaVersion -notmatch '^(\d+)') {
    throw "Unable to determine the Java version from $JavaHome."
}
if ([int]$Matches[1] -lt 23) {
    throw "JDK 23 or newer is required to build this project; found Java $($Matches[1])."
}

if (-not $JavaFxHome) {
    $JavaFxHome = "C:\Program Files\Java\javafx-sdk-25.0.3"
}
if (-not (Test-Path (Join-Path $JavaFxHome "lib\javafx.controls.jar"))) {
    throw "JavaFX SDK not found. Set JAVAFX_HOME or pass -JavaFxHome."
}

if (-not $InnoSetupCompiler) {
    $programFilesX86 = [Environment]::GetFolderPath("ProgramFilesX86")
    $compilerCandidates = @(
        (Join-Path $programFilesX86 "Inno Setup 6\ISCC.exe"),
        (Join-Path $env:ProgramFiles "Inno Setup 6\ISCC.exe"),
        (Join-Path $env:LOCALAPPDATA "Programs\Inno Setup 6\ISCC.exe")
    ) | Where-Object { $_ -and (Test-Path $_) }
    $InnoSetupCompiler = $compilerCandidates | Select-Object -First 1
}
if (-not $InnoSetupCompiler -or -not (Test-Path $InnoSetupCompiler)) {
    throw "Inno Setup 6 compiler not found. Install Inno Setup 6 or pass -InnoSetupCompiler."
}

$ant = Get-Command ant.bat -ErrorAction SilentlyContinue
if ($ant) {
    $antPath = $ant.Source
} else {
    $antCandidates = @(
        "C:\Program Files\NetBeans-20\netbeans\extide\ant\bin\ant.bat",
        "C:\Program Files\NetBeans-21\netbeans\extide\ant\bin\ant.bat"
    )
    $antPath = $antCandidates | Where-Object { Test-Path $_ } | Select-Object -First 1
}
if (-not $antPath) {
    throw "Apache Ant is required to build the app. Install Ant or use NetBeans."
}

$env:JAVA_HOME = (Resolve-Path $JavaHome).Path
$env:Path = "$env:JAVA_HOME\bin;$env:Path"

$antArgs = @(
    "-Dplatforms.default_platform.home=$env:JAVA_HOME",
    "-Dplatforms.default_platform.bootclasspath=$env:JAVA_HOME\lib\modules",
    "-Dplatforms.default_platform.compile=$env:JAVA_HOME\bin\javac.exe",
    "-Dplatforms.default_platform.javac=$env:JAVA_HOME\bin\javac.exe",
    "-Dplatforms.default_platform.java=$env:JAVA_HOME\bin\java.exe",
    "-Dplatforms.default_platform.javadoc=$env:JAVA_HOME\bin\javadoc.exe",
    "-Djavafx.sdk.home=$JavaFxHome",
    "jar"
)
Push-Location $projectDir
try {
    & $antPath @antArgs
    if ($LASTEXITCODE -ne 0) {
        throw "Ant build failed with exit code $LASTEXITCODE."
    }
} finally {
    Pop-Location
}

$installerWorkDir = Join-Path $projectDir "build\installer"
$payloadDir = Join-Path $installerWorkDir "payload"
$classpathDir = Join-Path $payloadDir "lib"
$javafxLibDir = Join-Path $payloadDir "javafx\lib"
$javafxBinDir = Join-Path $payloadDir "javafx\bin"
$runtimeImageDir = Join-Path $payloadDir "runtime"

if (Test-Path $payloadDir) {
    $resolvedPayloadDir = (Resolve-Path $payloadDir).Path
    if (-not $resolvedPayloadDir.StartsWith((Join-Path $projectDir "build"), [StringComparison]::OrdinalIgnoreCase)) {
        throw "Refusing to remove generated files outside the project build folder: $resolvedPayloadDir"
    }
    Remove-Item -LiteralPath $resolvedPayloadDir -Recurse -Force
}
New-Item -ItemType Directory -Path $payloadDir, $classpathDir, $javafxLibDir, $javafxBinDir | Out-Null
Copy-Item (Join-Path $projectDir "dist\DLP.jar") $payloadDir
$dependencyJars = Get-ChildItem (Join-Path $projectDir "dist\lib") -Filter "*.jar" -File |
    Where-Object { $_.Name -notmatch "^javafx-|^javafx\." -and $_.Name -ne "javafx-swt.jar" }
foreach ($jar in $dependencyJars) {
    Copy-Item -LiteralPath $jar.FullName -Destination $classpathDir
}
Get-ChildItem (Join-Path $JavaFxHome "lib") -Filter "javafx*.jar" -File |
    Where-Object { $_.Name -notmatch "javafx\.(media|swing|web)\.jar" } |
    ForEach-Object { Copy-Item -LiteralPath $_.FullName -Destination $javafxLibDir }
Copy-Item (Join-Path $JavaFxHome "lib\jdk.jsobject.jar") $javafxLibDir -ErrorAction SilentlyContinue
Get-ChildItem (Join-Path $JavaFxHome "bin") -Filter "*.dll" -File |
    Where-Object { $_.Name -notmatch "^(jfxwebkit|jfxmedia|fxplugins|gstreamer-lite|glib-lite)\.dll$" } |
    ForEach-Object { Copy-Item -LiteralPath $_.FullName -Destination $javafxBinDir }
Copy-Item (Join-Path $PSScriptRoot "LaunchDLP.vbs") $payloadDir

$modulePath = "$env:JAVA_HOME\jmods;$JavaFxHome\lib"
$jlinkArgs = @(
    "--module-path", $modulePath,
    "--add-modules", "java.se,javafx.controls,javafx.fxml",
    "--strip-debug",
    "--no-header-files",
    "--no-man-pages",
    "--compress=2",
    "--output", $runtimeImageDir
)
& (Join-Path $env:JAVA_HOME "bin\jlink.exe") @jlinkArgs
if ($LASTEXITCODE -ne 0) {
    throw "jlink failed with exit code $LASTEXITCODE."
}

$installerPath = Join-Path $projectDir $outputName
& $InnoSetupCompiler "/O$projectDir" "/F$([IO.Path]::GetFileNameWithoutExtension($outputName))" (Join-Path $PSScriptRoot "DigitalLessonPlanner.iss")
if ($LASTEXITCODE -ne 0) {
    throw "Inno Setup failed with exit code $LASTEXITCODE."
}
if (-not (Test-Path $installerPath)) {
    throw "Inno Setup completed without producing $installerPath."
}

New-Item -ItemType Directory -Path $downloadDir -Force | Out-Null
Copy-Item -LiteralPath $installerPath -Destination (Join-Path $downloadDir $outputName) -Force

Write-Host "Installer created at:"
Write-Host "  $installerPath"
Write-Host "  $(Join-Path $downloadDir $outputName)"
