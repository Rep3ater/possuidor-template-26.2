param(
    [string]$ProjectRoot = (Get-Location).Path
)
$ErrorActionPreference = 'Stop'
$root = (Resolve-Path -LiteralPath $ProjectRoot).Path
$spirit = Join-Path $root 'src\main\java\ladysnake\requiem\common\locki\LockiSpiritSystem.java'
if (-not (Test-Path -LiteralPath $spirit)) {
    throw "Could not find LockiSpiritSystem.java under: $root"
}

# Fix the Optional<Double> conversion that fails on the user's build.
$text = [System.IO.File]::ReadAllText($spirit)
$old = 'float original = (float) data.getDouble(SAVED_FLYING_SPEED).orElse(0.05);'
$new = 'float original = data.getDouble(SAVED_FLYING_SPEED).orElse(0.05D).floatValue();'
if ($text.Contains($old)) {
    $backup = "$spirit.before-build-recovery.bak"
    if (-not (Test-Path -LiteralPath $backup)) { Copy-Item -LiteralPath $spirit -Destination $backup }
    $text = $text.Replace($old, $new)
    [System.IO.File]::WriteAllText($spirit, $text, [System.Text.UTF8Encoding]::new($false))
    Write-Host 'Fixed saved flying-speed conversion.' -ForegroundColor Green
} elseif ($text.Contains($new)) {
    Write-Host 'Saved flying-speed conversion is already fixed.' -ForegroundColor DarkGreen
} else {
    throw 'Could not find the expected flying-speed line. No source was changed.'
}

# The refined project archive does not contain/register LockiDebugCommand.
# If a local copy exists with no references from other Java files, preserve it
# but move it out of javac's *.java source set to unblock testing.
$debug = Join-Path $root 'src\main\java\ladysnake\requiem\common\command\LockiDebugCommand.java'
if (Test-Path -LiteralPath $debug) {
    $javaFiles = Get-ChildItem -LiteralPath (Join-Path $root 'src\main\java') -Filter '*.java' -Recurse |
        Where-Object { $_.FullName -ne $debug }
    $refs = $javaFiles | Select-String -Pattern '\bLockiDebugCommand\b' -List
    if ($refs) {
        Write-Warning 'LockiDebugCommand is referenced by other Java files; leaving it in place. Its API migration must be fixed instead.'
    } else {
        $disabled = "$debug.disabled"
        if (Test-Path -LiteralPath $disabled) {
            throw "Both LockiDebugCommand.java and its preserved .disabled copy exist. Resolve manually: $disabled"
        }
        Move-Item -LiteralPath $debug -Destination $disabled
        Write-Host 'Preserved the unregistered debug command as LockiDebugCommand.java.disabled.' -ForegroundColor Yellow
    }
} else {
    Write-Host 'No extra LockiDebugCommand.java found; nothing to disable.'
}

Write-Host "`nRecovery edits complete. From the project root, run: .\gradlew.bat compileJava" -ForegroundColor Cyan
Write-Host 'This is a compile triage step, not proof of a successful full build.' -ForegroundColor Yellow
