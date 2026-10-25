$utf8NoBom = New-Object System.Text.UTF8Encoding($false)
$files = Get-ChildItem -Path "c:\Mods developer\possuidor-template-26.2\src\main\java" -Filter "*.java" -Recurse
foreach ($f in $files) {
    $bytes = [System.IO.File]::ReadAllBytes($f.FullName)
    if ($bytes.Length -ge 3 -and $bytes[0] -eq 0xEF -and $bytes[1] -eq 0xBB -and $bytes[2] -eq 0xBF) {
        $cleanBytes = [byte[]]::new($bytes.Length - 3)
        [System.Array]::Copy($bytes, 3, $cleanBytes, 0, $cleanBytes.Length)
        [System.IO.File]::WriteAllBytes($f.FullName, $cleanBytes)
    }
}
Write-Host "BOM stripped successfully from all files!"
