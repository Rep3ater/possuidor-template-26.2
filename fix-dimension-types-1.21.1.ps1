$ErrorActionPreference = 'Stop'

$projectRoot = (Get-Location).Path
$constructPath = Join-Path $projectRoot 'src\main\resources\data\requiem\dimension_type\construct_workshop.json'
$nucleusPath = Join-Path $projectRoot 'src\main\resources\data\requiem\dimension_type\nucleus.json'

foreach ($path in @($constructPath, $nucleusPath)) {
    if (-not (Test-Path -LiteralPath $path -PathType Leaf)) {
        throw "Required dimension type file not found: $path`nRun this script from the project root: $projectRoot"
    }
}

# Do not silently replace files from an unrelated project/version.
$constructOriginal = Get-Content -LiteralPath $constructPath -Raw
$nucleusOriginal = Get-Content -LiteralPath $nucleusPath -Raw
foreach ($entry in @(@{Path=$constructPath;Text=$constructOriginal}, @{Path=$nucleusPath;Text=$nucleusOriginal})) {
    try { $null = $entry.Text | ConvertFrom-Json }
    catch { throw "Invalid JSON in $($entry.Path): $($_.Exception.Message)" }
    if ($entry.Text -notmatch '"attributes"' -or $entry.Text -notmatch '"default_clock"') {
        throw "The file $($entry.Path) does not look like the expected 26.2-format dimension type. No files were changed."
    }
}

$backupDir = Join-Path $projectRoot ("_backup-dimension-types-1.21.1-{0}" -f (Get-Date -Format 'yyyyMMdd-HHmmss'))
New-Item -ItemType Directory -Path $backupDir | Out-Null
Copy-Item -LiteralPath $constructPath -Destination (Join-Path $backupDir 'construct_workshop.json')
Copy-Item -LiteralPath $nucleusPath -Destination (Join-Path $backupDir 'nucleus.json')

# Minecraft 1.21.1 uses the legacy, flat dimension_type schema. The newer
# attributes/default_clock/skybox/timelines keys belong to later versions.
# Values below preserve the original height, lighting, coordinate scale,
# infiniburn, spawn limits, bed/anchor restrictions, and intended sky preset.
$constructJson = @'
{
  "ambient_light": 1.0,
  "bed_works": false,
  "coordinate_scale": 1.0,
  "effects": "minecraft:the_end",
  "fixed_time": 6000,
  "has_ceiling": false,
  "has_raids": false,
  "has_skylight": true,
  "height": 256,
  "infiniburn": "#minecraft:infiniburn_end",
  "logical_height": 256,
  "min_y": 0,
  "monster_spawn_block_light_limit": 0,
  "monster_spawn_light_level": 0,
  "natural": false,
  "piglin_safe": false,
  "respawn_anchor_works": false,
  "ultrawarm": false
}
'@

$nucleusJson = @'
{
  "ambient_light": 0.25,
  "bed_works": false,
  "coordinate_scale": 1.0,
  "effects": "minecraft:overworld",
  "has_ceiling": false,
  "has_raids": false,
  "has_skylight": true,
  "height": 256,
  "infiniburn": "#minecraft:infiniburn_nether",
  "logical_height": 256,
  "min_y": 0,
  "monster_spawn_block_light_limit": 0,
  "monster_spawn_light_level": 0,
  "natural": false,
  "piglin_safe": false,
  "respawn_anchor_works": false,
  "ultrawarm": false
}
'@

# Validate both replacement documents before writing either one.
$constructParsed = $constructJson | ConvertFrom-Json
$nucleusParsed = $nucleusJson | ConvertFrom-Json
$required = @('ultrawarm','natural','coordinate_scale','has_skylight','has_ceiling','ambient_light','piglin_safe','bed_works','respawn_anchor_works','has_raids','logical_height','min_y','height','infiniburn','effects','monster_spawn_block_light_limit','monster_spawn_light_level')
foreach ($doc in @($constructParsed, $nucleusParsed)) {
    foreach ($key in $required) {
        if ($null -eq $doc.PSObject.Properties[$key]) { throw "Replacement JSON is missing required 1.21.1 key: $key" }
    }
}

Set-Content -LiteralPath $constructPath -Value $constructJson.Trim() -Encoding UTF8
Set-Content -LiteralPath $nucleusPath -Value $nucleusJson.Trim() -Encoding UTF8

Write-Host "Updated both dimension type JSON files for Minecraft 1.21.1." -ForegroundColor Green
Write-Host "Original files backed up to: $backupDir"
Write-Host "Removed incompatible 26.2 keys: attributes, default_clock, has_ender_dragon_fight, has_fixed_time, skybox, timelines."
Write-Host "Note: 1.21.1 dimension_type JSON cannot express the 26.2 custom visual-color and dimension ambient-sound attributes. Nucleus keeps the Overworld sky preset and basalt-deltas biome; Workshop keeps the End sky preset."

Write-Host "`nRunning Java compilation/resource processing..."
& .\gradlew.bat clean compileJava processResources
if ($LASTEXITCODE -ne 0) {
    throw "Gradle validation failed with exit code $LASTEXITCODE. The JSON edits and backups are retained."
}

Write-Host "`nNext: launch with .\gradlew.bat runClient, then test creating a NEW world." -ForegroundColor Green
