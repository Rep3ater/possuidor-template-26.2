$files = Get-ChildItem -Path "c:\Mods developer\possuidor-template-26.2\src\main\java" -Filter "*.java" -Recurse
foreach ($f in $files) {
    $content = [System.IO.File]::ReadAllText($f.FullName)
    if ($content.Contains("RequiemBuiltInRegistries")) {
        $content = $content.Replace("RequiemBuiltInRegistries", "RequiemRegistries")
        [System.IO.File]::WriteAllText($f.FullName, $content, (New-Object System.Text.UTF8Encoding($false)))
    }
}
Write-Host "Fixed RequiemBuiltInRegistries!"
