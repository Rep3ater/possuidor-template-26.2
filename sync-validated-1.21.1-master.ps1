# Safely align this working copy with the repository's validated 1.21.1 / NeoForge 21.1.172 master.
# Run from the project root. Existing tracked source/configuration is backed up first.

$ErrorActionPreference = 'Stop'
$Root = (Get-Location).Path
$ExpectedRemotePattern = 'github\.com[:/]Rep3ater/possuidor-template-26\.2(?:\.git)?$'

function Invoke-Git {
    param([Parameter(Mandatory = $true)][string[]]$GitArgs)
    & git -C $Root @GitArgs
    if ($LASTEXITCODE -ne 0) {
        throw "Git command failed: git $($GitArgs -join ' ')"
    }
}

$gitRootOutput = & git -C $Root rev-parse --show-toplevel 2>$null
if ($LASTEXITCODE -ne 0 -or [string]::IsNullOrWhiteSpace(($gitRootOutput -join ''))) {
    throw "This folder is not a Git checkout. Open the cloned Rep3ater/possuidor-template-26.2 repository folder and run the script there. No files were changed."
}
$GitRoot = ([string]($gitRootOutput -join '')).Trim()
if ([IO.Path]::GetFullPath($GitRoot).TrimEnd('\') -ne [IO.Path]::GetFullPath($Root).TrimEnd('\')) {
    throw "Run this script from the repository root. Detected Git root: $GitRoot"
}

$originOutput = & git -C $Root remote get-url origin 2>$null
if ($LASTEXITCODE -ne 0) {
    throw 'No Git remote named origin was found. No files were changed.'
}
$Origin = ([string]($originOutput -join '')).Trim()
if ($Origin -notmatch $ExpectedRemotePattern) {
    throw "Unexpected origin '$Origin'. Expected the Rep3ater/possuidor-template-26.2 repository. No files were changed."
}

$Stamp = Get-Date -Format 'yyyyMMdd-HHmmss'
$Backup = Join-Path $Root "_backup-before-validated-1.21.1-$Stamp"
New-Item -ItemType Directory -Path $Backup -Force | Out-Null

# Back up all files that this script may overwrite. The backup is outside Git's tracked paths.
$BackupPaths = @(
    'src',
    'build.gradle',
    'gradle.properties',
    'settings.gradle',
    'gradle',
    'gradlew',
    'gradlew.bat'
)
foreach ($RelativePath in $BackupPaths) {
    $SourcePath = Join-Path $Root $RelativePath
    if (Test-Path -LiteralPath $SourcePath) {
        $BackupPath = Join-Path $Backup $RelativePath
        $BackupParent = Split-Path -Parent $BackupPath
        New-Item -ItemType Directory -Path $BackupParent -Force | Out-Null
        Copy-Item -LiteralPath $SourcePath -Destination $BackupPath -Recurse -Force
    }
}

& git -C $Root status --short | Out-File -FilePath (Join-Path $Backup 'git-status-before.txt') -Encoding utf8
& git -C $Root diff --binary HEAD | Out-File -FilePath (Join-Path $Backup 'tracked-working-tree-diff.patch') -Encoding utf8

Write-Host "Backup created: $Backup" -ForegroundColor Cyan
Write-Host 'Fetching the validated master branch...'
Invoke-Git -GitArgs @('fetch', 'origin', 'master')

$Properties = (& git -C $Root show 'origin/master:gradle.properties' | Out-String)
if ($LASTEXITCODE -ne 0 -or $Properties -notmatch '(?m)^minecraft_version=1\.21\.1\s*$' -or $Properties -notmatch '(?m)^neo_version=21\.1\.172\s*$') {
    throw 'origin/master did not contain the expected Minecraft 1.21.1 / NeoForge 21.1.172 target. Backup is safe; no source files were restored.'
}

$BuildGradle = (& git -C $Root show 'origin/master:build.gradle' | Out-String)
if ($LASTEXITCODE -ne 0 -or $BuildGradle -notmatch "net\.neoforged\.moddev' version '2\.0\.107'") {
    throw 'origin/master build.gradle did not match the known validated ModDevGradle 2.0.107 setup. Backup is safe; no source files were restored.'
}

Write-Host 'Restoring tracked source, resources, Gradle scripts and wrapper from origin/master...' -ForegroundColor Yellow
Invoke-Git -GitArgs @(
    'restore', '--source=origin/master', '--worktree', '--',
    'src', 'build.gradle', 'gradle.properties', 'settings.gradle',
    'gradle', 'gradlew', 'gradlew.bat'
)

Write-Host ''
Write-Host 'Restored target configuration:' -ForegroundColor Green
Select-String -Path (Join-Path $Root 'gradle.properties') -Pattern '^(minecraft_version|neo_version|mod_version)='
Write-Host 'Build plugin:' -ForegroundColor Green
Select-String -Path (Join-Path $Root 'build.gradle') -Pattern "net\.neoforged\.moddev"

Write-Host ''
Write-Host 'Running clean build against the validated 1.21.1 source tree...' -ForegroundColor Yellow
& (Join-Path $Root 'gradlew.bat') --no-daemon clean build
$BuildExit = $LASTEXITCODE

if ($BuildExit -eq 0) {
    Write-Host ''
    Write-Host 'SUCCESS: clean build completed.' -ForegroundColor Green
    Write-Host 'To launch Minecraft, run: .\gradlew runClient'
} else {
    Write-Host ''
    Write-Host "BUILD FAILED (exit code $BuildExit). Send the new output; it will now be from the 1.21.1 source baseline." -ForegroundColor Red
}
Write-Host "Original files remain backed up at: $Backup" -ForegroundColor Cyan
exit $BuildExit
