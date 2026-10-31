$ErrorActionPreference = 'Stop'

$projectRoot = (Get-Location).Path
$sourcePath = Join-Path $projectRoot 'src\main\java\ladysnake\requiem\mixin\client\LockiCameraMixin.java'
$gradlePath = Join-Path $projectRoot 'gradlew.bat'

if (-not (Test-Path -LiteralPath $sourcePath)) {
    throw "LockiCameraMixin.java was not found. Run this script from the project root: $projectRoot"
}
if (-not (Test-Path -LiteralPath $gradlePath)) {
    throw "gradlew.bat was not found. Run this script from the project root: $projectRoot"
}

$source = [System.IO.File]::ReadAllText($sourcePath)

# This is the exact 1.21.1 camera hook. Later Minecraft versions changed Camera.update
# to accept DeltaTracker; 1.21.1 instead calls Camera.setup(BlockGetter, Entity, ...).
$oldImport = 'import net.minecraft.client.DeltaTracker;'
$newImport = 'import net.minecraft.world.level.BlockGetter;'
if ($source.Contains($oldImport)) {
    $source = $source.Replace($oldImport, $newImport)
} elseif (-not $source.Contains($newImport)) {
    throw 'Could not identify the DeltaTracker import or an existing BlockGetter import. No changes were made.'
}

$oldHook = '@Inject(method = "update", at = @At("TAIL"))' + "`r`n" +
           '    private void requiem$lockiCamera(DeltaTracker deltaTracker, CallbackInfo ci) {'
$newHook = '@Inject(method = "setup", at = @At("TAIL"))' + "`r`n" +
           '    private void requiem$lockiCamera(BlockGetter level, net.minecraft.world.entity.Entity entity, boolean detached, boolean thirdPersonReverse, float partialTick, CallbackInfo ci) {'

if ($source.Contains($oldHook)) {
    $source = $source.Replace($oldHook, $newHook)
} else {
    # Handle LF-only source files without making broad, unsafe substitutions.
    $oldHookLf = '@Inject(method = "update", at = @At("TAIL"))' + "`n" +
                 '    private void requiem$lockiCamera(DeltaTracker deltaTracker, CallbackInfo ci) {'
    $newHookLf = '@Inject(method = "setup", at = @At("TAIL"))' + "`n" +
                 '    private void requiem$lockiCamera(BlockGetter level, net.minecraft.world.entity.Entity entity, boolean detached, boolean thirdPersonReverse, float partialTick, CallbackInfo ci) {'
    if ($source.Contains($oldHookLf)) {
        $source = $source.Replace($oldHookLf, $newHookLf)
    } elseif (-not ($source.Contains('method = "setup"') -and $source.Contains('BlockGetter level'))) {
        throw 'Could not find the expected Camera.update(DeltaTracker) hook. No changes were written.'
    }
}

if ($source.Contains('DeltaTracker') -or -not $source.Contains('method = "setup"') -or -not $source.Contains('BlockGetter level')) {
    throw 'Camera hook validation failed. No changes were written.'
}

$backupDir = Join-Path $projectRoot ('_backup-locki-camera-1.21.1-' + (Get-Date -Format 'yyyyMMdd-HHmmss'))
New-Item -ItemType Directory -Path $backupDir | Out-Null
Copy-Item -LiteralPath $sourcePath -Destination (Join-Path $backupDir 'LockiCameraMixin.java')

$utf8NoBom = New-Object System.Text.UTF8Encoding($false)
[System.IO.File]::WriteAllText($sourcePath, $source, $utf8NoBom)
Write-Host "Updated: $sourcePath" -ForegroundColor Green
Write-Host "Backup:  $backupDir" -ForegroundColor DarkCyan
Write-Host 'Camera.setup(BlockGetter, Entity, boolean, boolean, float) hook installed.' -ForegroundColor Green

Write-Host "`nRunning clean compileJava..." -ForegroundColor Cyan
& $gradlePath clean compileJava
if ($LASTEXITCODE -ne 0) {
    throw "compileJava failed with exit code $LASTEXITCODE. The source backup is at: $backupDir"
}
Write-Host "`ncompileJava PASSED. Next run: .\gradlew.bat runClient" -ForegroundColor Green
