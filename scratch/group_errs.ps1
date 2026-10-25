$lines = Get-Content 'C:\Users\gusta\.gemini\antigravity\brain\bf263c0c-fb4f-4dbb-a309-748a369cf583\scratch\errs_raw.txt'
$files = @{}
foreach ($line in $lines) {
    if ($line -match '^(?:\s*)(.*\\java\\)(.+\.java):(\d+): error: (.*)$') {
        $f = $Matches[2]
        $files[$f] = [int]$files[$f] + 1
    }
}
$files.GetEnumerator() | Sort-Object Value -Descending | Format-Table -AutoSize
