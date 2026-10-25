$files = Get-ChildItem -Path "src\main\java\ladysnake\requiem\core" -Recurse -Filter "*.java"
$utf8NoBom = New-Object System.Text.UTF8Encoding($false)
$fixed = 0

foreach ($f in $files) {
    $content = [System.IO.File]::ReadAllText($f.FullName, [System.Text.Encoding]::UTF8)
    if ($content -match '(?s)(import\s+net\.minecraft\.[^;]+;\s+)+(package\s+ladysnake\.requiem\.[^;]+;)') {
        $importPart = $matches[1]
        $packagePart = $matches[2]
        $content = $content.Replace($matches[0], $packagePart + "`r`n`r`n" + $importPart)
        [System.IO.File]::WriteAllText($f.FullName, $content, $utf8NoBom)
        $fixed++
    }
}

Write-Host "Fixed package order in $fixed files."
