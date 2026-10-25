$files = Get-ChildItem -Path "src\main\java" -Filter "*.java" -Recurse
$utf8NoBom = New-Object System.Text.UTF8Encoding($false)
$count = 0
foreach ($file in $files) {
    $bytes = [System.IO.File]::ReadAllBytes($file.FullName)
    if ($bytes.Length -ge 3 -and $bytes[0] -eq 0xEF -and $bytes[1] -eq 0xBB -and $bytes[2] -eq 0xBF) {
        $text = [System.Text.Encoding]::UTF8.GetString($bytes, 3, $bytes.Length - 3)
        [System.IO.File]::WriteAllText($file.FullName, $text, $utf8NoBom)
        $count++
    }
}
Write-Host "Stripped BOM from $count files."
