$ErrorActionPreference = 'Stop'

# Restores only RequiemClient.java to the known pre-vignette 1.21.1 version.
# ParmenidesManager and the entity/block rendering mixins are left untouched.
$baselineCommit = 'c1a1ae00811c940eca8eaeffe8e0dea97bb24e1d'
$relativePath = 'src/main/java/ladysnake/requiem/client/RequiemClient.java'

function Invoke-GitText([string[]]$GitArgs) {
    $output = & git @GitArgs 2>&1
    if ($LASTEXITCODE -ne 0) {
        throw "Git command failed: git $($GitArgs -join ' ')`n$output"
    }
    return ($output -join [Environment]::NewLine)
}

$root = (Invoke-GitText @('rev-parse', '--show-toplevel')).Trim()
if (-not $root -or -not (Test-Path -LiteralPath $root -PathType Container)) {
    throw 'Could not identify the project Git root. Run this script from inside the mod repository.'
}

$target = Join-Path $root ($relativePath -replace '/', [IO.Path]::DirectorySeparatorChar)
if (-not (Test-Path -LiteralPath $target -PathType Leaf)) {
    throw "Expected client source file was not found: $target"
}

# Make sure the exact pre-session baseline object is available locally.
& git -C $root cat-file -e "$baselineCommit`:$relativePath" 2>$null
if ($LASTEXITCODE -ne 0) {
    Write-Host 'The baseline commit is not present locally. Fetching master from origin...'
    & git -C $root fetch origin master
    if ($LASTEXITCODE -ne 0) {
        throw 'Could not fetch origin/master. No files were changed.'
    }
    & git -C $root cat-file -e "$baselineCommit`:$relativePath" 2>$null
    if ($LASTEXITCODE -ne 0) {
        throw "The baseline file is unavailable at commit $baselineCommit. No files were changed."
    }
}

$baseline = & git -C $root show "$baselineCommit`:$relativePath"
if ($LASTEXITCODE -ne 0 -or -not $baseline) {
    throw 'Could not read the baseline RequiemClient.java. No files were changed.'
}
$baselineText = $baseline -join [Environment]::NewLine
$currentText = [IO.File]::ReadAllText($target)

if ($currentText -ceq $baselineText) {
    Write-Host 'RequiemClient.java already matches the pre-session 1.21.1 baseline. No change needed.' -ForegroundColor Green
    exit 0
}

# Create a timestamped backup of the exact file before replacing it.
$stamp = Get-Date -Format 'yyyyMMdd-HHmmss'
$backupRoot = Join-Path $root "_backup-before-removing-parmenides-vignette-$stamp"
$backupFile = Join-Path $backupRoot ($relativePath -replace '/', [IO.Path]::DirectorySeparatorChar)
New-Item -ItemType Directory -Path (Split-Path -Parent $backupFile) -Force | Out-Null
Copy-Item -LiteralPath $target -Destination $backupFile -Force

# Restore the whole client class to its known pre-session state. This removes the
# GUI-drawn cyan vignette without touching ParmenidesManager or the renderer mixins.
$encoding = New-Object System.Text.UTF8Encoding($false)
[IO.File]::WriteAllText($target, $baselineText + [Environment]::NewLine, $encoding)

$restoredText = [IO.File]::ReadAllText($target)
if ($restoredText -notmatch 'onRenderGuiPost' -or $restoredText -match '(?i)vignette') {
    Copy-Item -LiteralPath $backupFile -Destination $target -Force
    throw 'Post-restore verification failed. Original RequiemClient.java was restored from backup.'
}

Write-Host ''
Write-Host 'Removed the GUI vignette by restoring only RequiemClient.java.' -ForegroundColor Green
Write-Host "Project: $root"
Write-Host "Backup:  $backupFile"
Write-Host "Baseline: $baselineCommit"
Write-Host ''
Write-Host 'ParmenidesManager.java and LivingEntityRendererMixin.java / BlockRenderDispatcherMixin.java were not modified by this script.'
Write-Host 'The existing RenderGuiEvent hook for ordinary incorporeal targeting icons remains in place.'
Write-Host ''
Write-Host 'Review the exact change with:'
Write-Host "git -C `"$root`" diff -- `"$relativePath`""
Write-Host ''
Write-Host 'Then compile with:'
Write-Host ' .\gradlew.bat compileJava'
