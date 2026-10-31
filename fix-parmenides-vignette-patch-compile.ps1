$ErrorActionPreference = 'Stop'

# The previous vignette-removal patch restored RequiemClient.java to the clean
# 1.21.1 client implementation, but left two calls in GameRendererMixin.java to
# helper methods that belonged to the removed experimental overlay implementation.
# Restore the original incorporeal post-effect branch without changing other mixins.

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

$relativePath = 'src/main/java/ladysnake/requiem/mixin/client/GameRendererMixin.java'
$target = Join-Path $root ($relativePath -replace '/', [IO.Path]::DirectorySeparatorChar)
if (-not (Test-Path -LiteralPath $target -PathType Leaf)) {
    throw "Expected source file was not found: $target"
}

$text = [IO.File]::ReadAllText($target)
$before = $text

$conditionPattern = 'mc\.player\s*!=\s*null\s*&&\s*RequiemClient\.isOutOfBodyVisualsActive\(mc\.player\)'
$effectPattern = 'RequiemClient\.applyPostEffect\(RequiemClient\.outOfBodyPostEffect\(mc\.player\)\)\s*;'

$conditionMatches = [regex]::Matches($text, $conditionPattern).Count
$effectMatches = [regex]::Matches($text, $effectPattern).Count

if ($conditionMatches -eq 0 -and $effectMatches -eq 0) {
    if ($text -match 'isOutOfBodyVisualsActive|outOfBodyPostEffect') {
        throw 'The obsolete helper calls are present but formatted differently. No files were changed; inspect GameRendererMixin.java manually.'
    }
    Write-Host 'GameRendererMixin.java already has no references to the removed vignette helpers.' -ForegroundColor Green
} elseif ($conditionMatches -ne 1 -or $effectMatches -ne 1) {
    throw "Expected exactly one matching condition and effect call, found condition=$conditionMatches effect=$effectMatches. No files were changed."
} else {
    # Make sure the vanilla Requiem incorporeal effect API is imported.
    $import = 'import ladysnake.requiem.api.v1.remnant.RemnantComponent;'
    if ($text -notmatch '(?m)^import ladysnake\.requiem\.api\.v1\.remnant\.RemnantComponent;\s*$') {
        $packageMatch = [regex]::Match($text, '(?m)^package [^;]+;\s*')
        if (-not $packageMatch.Success) {
            throw 'Could not find the Java package declaration. No files were changed.'
        }
        $text = $text.Insert($packageMatch.Index + $packageMatch.Length, "`r`n$import`r`n")
    }

    $text = [regex]::Replace(
        $text,
        $conditionPattern,
        'mc.player != null && RemnantComponent.isIncorporeal(mc.player)',
        [System.Text.RegularExpressions.RegexOptions]::None
    )
    $text = [regex]::Replace(
        $text,
        $effectPattern,
        'RequiemClient.applyPostEffect(RequiemClient.SPECTRE_POST_EFFECT);',
        [System.Text.RegularExpressions.RegexOptions]::None
    )

    if ($text -match 'isOutOfBodyVisualsActive|outOfBodyPostEffect') {
        throw 'Verification failed: obsolete helper calls remain. No file was written.'
    }

    $stamp = Get-Date -Format 'yyyyMMdd-HHmmss'
    $backup = "$target.bak-$stamp"
    Copy-Item -LiteralPath $target -Destination $backup -Force
    $encoding = New-Object System.Text.UTF8Encoding($false)
    [IO.File]::WriteAllText($target, $text, $encoding)

    Write-Host 'Patched GameRendererMixin.java to use the original 1.21.1 incorporeal post-effect path.' -ForegroundColor Green
    Write-Host "Backup: $backup"
}

$clientPath = Join-Path $root 'src/main/java/ladysnake/requiem/client/RequiemClient.java'
if (-not (Test-Path -LiteralPath $clientPath -PathType Leaf)) {
    throw 'RequiemClient.java is missing; cannot verify the client source.'
}
$clientText = [IO.File]::ReadAllText($clientPath)
if ($clientText -match 'isOutOfBodyVisualsActive|outOfBodyPostEffect') {
    throw 'Unexpected vignette helper methods remain in RequiemClient.java; inspect before compiling.'
}

Write-Host ''
Write-Host 'Compiling Java sources...' -ForegroundColor Cyan
Push-Location $root
try {
    & .\gradlew.bat compileJava
    if ($LASTEXITCODE -ne 0) {
        throw "compileJava failed with exit code $LASTEXITCODE. Review the compiler output above."
    }
} finally {
    Pop-Location
}

Write-Host ''
Write-Host 'BUILD SUCCESSFUL: compileJava passed.' -ForegroundColor Green
Write-Host 'The GUI vignette remains removed. Parmenides projection/rendering files were not modified.'
