$ErrorActionPreference = 'Stop'

# Run this script from the root of possuidor-template-26.2 - Copy - Copy.
$root = (Get-Location).Path
$propertiesPath = Join-Path $root 'gradle.properties'
$wrapperPath = Join-Path $root 'gradlew.bat'
$buildPath = Join-Path $root 'build.gradle'

if (-not (Test-Path $propertiesPath)) {
    throw "gradle.properties was not found in $root. Run this script from the project root."
}
if (-not (Test-Path $wrapperPath)) {
    throw "gradlew.bat was not found in $root. This does not look like the Gradle project root."
}
if (-not (Test-Path $buildPath)) {
    throw "build.gradle was not found in $root."
}

# Keep a backup so the target can be restored easily.
$stamp = Get-Date -Format 'yyyyMMdd-HHmmss'
$backupPath = "$propertiesPath.before-1.21.1-$stamp.bak"
Copy-Item -LiteralPath $propertiesPath -Destination $backupPath

$content = Get-Content -LiteralPath $propertiesPath -Raw
$values = [ordered]@{
    'minecraft_version'       = '1.21.1'
    'minecraft_version_range' = '[1.21.1,1.22)'
    'neo_version'             = '21.1.172'
}

foreach ($entry in $values.GetEnumerator()) {
    $pattern = '(?m)^\s*' + [regex]::Escape($entry.Key) + '\s*=.*$'
    $replacement = $entry.Key + '=' + $entry.Value
    if ([regex]::IsMatch($content, $pattern)) {
        $content = [regex]::Replace($content, $pattern, $replacement)
    } else {
        $content = $content.TrimEnd() + "`r`n" + $replacement + "`r`n"
    }
}

# Write UTF-8 without a BOM, which Gradle reads consistently.
$utf8NoBom = New-Object System.Text.UTF8Encoding($false)
[System.IO.File]::WriteAllText($propertiesPath, $content, $utf8NoBom)

# Fix the exhaustive EquipmentSlot switch: this source declares a SADDLE node
# but omitted the corresponding enum branch.
$nodesPath = Join-Path $root 'src\main\java\io\github\ladysnake\locki\DefaultInventoryNodes.java'
if (Test-Path $nodesPath) {
    $nodes = Get-Content -LiteralPath $nodesPath -Raw
    if ($nodes -notmatch '(?m)^\s*case\s+SADDLE\s*->') {
        $nodes = $nodes.Replace('            case BODY -> BODY;', '            case BODY -> BODY;' + "`r`n            case SADDLE -> SADDLE;")
        [System.IO.File]::WriteAllText($nodesPath, $nodes, $utf8NoBom)
        Write-Host 'Patched DefaultInventoryNodes.java: added the SADDLE switch branch.'
    }
}

# The 1.21.1 target in the repository uses Java 21 and ModDevGradle 2.0.107.
$build = Get-Content -LiteralPath $buildPath -Raw
if ($build -notmatch 'net\.neoforged\.moddev' -or $build -notmatch '2\.0\.107') {
    Write-Warning 'build.gradle does not appear to use ModDevGradle 2.0.107 as on the 1.21.1 master branch. Review build.gradle before compiling.'
}
if ($build -notmatch 'JavaLanguageVersion\.of\(21\)') {
    Write-Warning 'build.gradle does not explicitly select Java 21. The 1.21.1 target requires Java 21.'
}

Write-Host "Updated target to Minecraft 1.21.1 / NeoForge 21.1.172."
Write-Host "Backup: $backupPath"

# Stop Gradle and remove this project's stale configuration cache.
& $wrapperPath --stop
$cachePath = Join-Path $root '.gradle\configuration-cache'
if (Test-Path $cachePath) {
    Remove-Item -LiteralPath $cachePath -Recurse -Force
}

Write-Host ''
Write-Host 'Running a clean Java compile against the 1.21.1 dependency set...'
& $wrapperPath --no-configuration-cache --refresh-dependencies clean compileJava
$buildExitCode = $LASTEXITCODE
if ($buildExitCode -ne 0) {
    Write-Host ''
    Write-Host "compileJava still has errors (exit code $buildExitCode). The new output is now the relevant 1.21.1 error list."
    exit $buildExitCode
}

Write-Host ''
Write-Host 'compileJava succeeded. You can now run: .\gradlew runclient'
