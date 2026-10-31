$ErrorActionPreference = 'Stop'

# Restore the exact 1.21.1 master snapshot that existed before this session.
$TargetCommit = 'c1a1ae00811c940eca8eaeffe8e0dea97bb24e1d'
$ExpectedOriginSuffix = 'Rep3ater/possuidor-template-26.2'

$RepoRoot = (Get-Location).Path
if (-not (Test-Path (Join-Path $RepoRoot '.git'))) {
    throw "No .git directory found in '$RepoRoot'. Run this script from the repository root."
}
if (-not (Test-Path (Join-Path $RepoRoot 'gradle.properties')) -or
    -not (Test-Path (Join-Path $RepoRoot 'build.gradle'))) {
    throw 'This does not look like the Requiem Gradle project root.'
}

$RemoteUrl = (& git -C $RepoRoot remote get-url origin 2>$null | Select-Object -First 1)
if ($LASTEXITCODE -ne 0 -or [string]::IsNullOrWhiteSpace($RemoteUrl) -or
    $RemoteUrl -notmatch [regex]::Escape($ExpectedOriginSuffix)) {
    throw "The origin remote is not the expected repository ($ExpectedOriginSuffix). Found: '$RemoteUrl'. No changes made."
}

$Stamp = Get-Date -Format 'yyyyMMdd-HHmmss'
$ProjectName = Split-Path $RepoRoot -Leaf
$BackupRoot = Join-Path (Split-Path $RepoRoot -Parent) ($ProjectName + '-before-pre-session-1.21.1-restore-' + $Stamp)
$BackupBranch = 'backup/local-before-pre-session-1.21.1-restore-' + $Stamp

$CurrentHead = (& git -C $RepoRoot rev-parse HEAD).Trim()
if ($LASTEXITCODE -ne 0 -or [string]::IsNullOrWhiteSpace($CurrentHead)) {
    throw 'Could not read the current Git commit. No repository reset was attempted.'
}

Write-Host "Creating a safety copy of your current project at:`n$BackupRoot" -ForegroundColor Cyan
New-Item -ItemType Directory -Path $BackupRoot -Force | Out-Null
# Preserve tracked edits, untracked files, and local configuration. Build caches are
# excluded because they can be regenerated; the .git database is backed up via a branch.
& robocopy $RepoRoot $BackupRoot /E /COPY:DAT /DCOPY:DAT /R:1 /W:1 /XD (Join-Path $RepoRoot '.git') (Join-Path $RepoRoot '.gradle') (Join-Path $RepoRoot 'build')
$RoboCode = $LASTEXITCODE
if ($RoboCode -ge 8) {
    throw "Safety copy failed (Robocopy exit code $RoboCode). The repository was not reset."
}

& git -C $RepoRoot branch $BackupBranch $CurrentHead
if ($LASTEXITCODE -ne 0) {
    throw "Could not create safety branch '$BackupBranch'. No repository reset was attempted."
}

Write-Host 'Fetching origin/master...'
& git -C $RepoRoot fetch origin '+refs/heads/master:refs/remotes/origin/master'
if ($LASTEXITCODE -ne 0) { throw 'Git fetch failed. The local branch was not reset.' }

$FetchedMaster = (& git -C $RepoRoot rev-parse refs/remotes/origin/master).Trim()
if ($LASTEXITCODE -ne 0 -or $FetchedMaster -ne $TargetCommit) {
    throw "Safety check failed: origin/master is '$FetchedMaster', not the expected 1.21.1 commit '$TargetCommit'. No reset performed."
}

$CurrentBranch = (& git -C $RepoRoot branch --show-current).Trim()
if ($CurrentBranch -ne 'master') {
    & git -C $RepoRoot show-ref --verify --quiet refs/heads/master
    if ($LASTEXITCODE -eq 0) {
        & git -C $RepoRoot switch --force master
    } else {
        & git -C $RepoRoot switch -c master --track origin/master
    }
    if ($LASTEXITCODE -ne 0) { throw 'Could not switch to master. Your safety copy and backup branch are preserved.' }
}

Write-Host 'Restoring local master to the pre-session 1.21.1 port snapshot...'
& git -C $RepoRoot reset --hard $TargetCommit
if ($LASTEXITCODE -ne 0) { throw 'Git reset failed. Use the safety copy to recover the previous working tree.' }

$RestoredProperties = Get-Content (Join-Path $RepoRoot 'gradle.properties') -Raw
if ($RestoredProperties -notmatch '(?m)^minecraft_version=1\.21\.1$' -or
    $RestoredProperties -notmatch '(?m)^neo_version=21\.1\.172$') {
    throw 'Post-reset verification failed: gradle.properties does not identify Minecraft 1.21.1 / NeoForge 21.1.172.'
}

Write-Host ''
Write-Host 'RESTORE COMPLETE' -ForegroundColor Green
Write-Host "Branch: master"
Write-Host "Commit: $TargetCommit"
Write-Host 'Target: Minecraft 1.21.1 / NeoForge 21.1.172 / Java 21 / Gradle 8.12'
Write-Host "Safety copy: $BackupRoot"
Write-Host "Safety branch: $BackupBranch"
Write-Host 'No git clean was run. Untracked and ignored files were not deleted.'
Write-Host 'A clean master contains the pre-session port only; session-only tracked edits are retained in the safety copy, not reapplied.'
