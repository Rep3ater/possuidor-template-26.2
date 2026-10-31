$ErrorActionPreference = 'Stop'
$root = (Get-Location).Path

if (-not (Test-Path (Join-Path $root 'build.gradle')) -or -not (Test-Path (Join-Path $root 'gradle.properties'))) {
    throw 'Run this script from the Requiem project root (the folder containing build.gradle and gradle.properties).'
}

$files = @(
    'src/main/java/ladysnake/requiem/common/entity/RequiemEntities.java',
    'src/main/java/ladysnake/requiem/common/ghost/ParmenidesManager.java',
    'src/main/java/ladysnake/requiem/data/RequiemDataGeneration.java'
)
foreach ($file in $files) {
    if (-not (Test-Path (Join-Path $root $file))) { throw "Required file is missing: $file" }
}

$stamp = Get-Date -Format 'yyyyMMdd-HHmmss'
$backup = Join-Path $root "_backup-parmenides-1.21.1-fixes-$stamp"
New-Item -ItemType Directory -Path $backup | Out-Null
foreach ($file in $files) {
    $destination = Join-Path $backup $file
    New-Item -ItemType Directory -Force -Path (Split-Path $destination) | Out-Null
    Copy-Item (Join-Path $root $file) $destination
}
Write-Host "Backup created: $backup"

function Replace-Once([string]$Text, [string]$Old, [string]$New, [string]$Label) {
    $count = ([regex]::Matches($Text, [regex]::Escape($Old))).Count
    if ($count -eq 0) {
        if ($Text.Contains($New)) { return $Text }
        throw "Could not find expected source pattern for: $Label"
    }
    if ($count -ne 1) { throw "Expected exactly one occurrence for '$Label', found $count" }
    return $Text.Replace($Old, $New)
}

# 1) Register the Parmenides projection entity on the restored 1.21.1 entity registry.
$path = Join-Path $root $files[0]
$src = [IO.File]::ReadAllText($path)
if (-not $src.Contains('import ladysnake.requiem.common.ghost.ParmenidesProjectionEntity;')) {
    $src = Replace-Once $src 'import baritone.api.fakeplayer.FakePlayers;' "import baritone.api.fakeplayer.FakePlayers;`r`nimport ladysnake.requiem.common.ghost.ParmenidesProjectionEntity;" 'Parmenides entity import'
}
if (-not $src.Contains('PARMENIDES_PROJECTION')) {
    $anchorPattern = '(?m)^    public static final EntityType<MorticianEntity> MORTICIAN ='
    $anchorCount = ([regex]::Matches($src, $anchorPattern)).Count
    if ($anchorCount -ne 1) { throw "Expected one Mortician entity anchor, found $anchorCount" }
    $field = @'
    public static final EntityType<ParmenidesProjectionEntity> PARMENIDES_PROJECTION = EntityType.Builder.<ParmenidesProjectionEntity>of(
            FakePlayers.entityFactory(ParmenidesProjectionEntity::new), MobCategory.MISC)
        .sized(0.6f, 1.8f)
        .clientTrackingRange(10)
        .updateInterval(1)
        .noSummon()
        .build(Requiem.id("parmenides_projection").toString());

'@
    $src = [regex]::Replace($src, $anchorPattern, [System.Text.RegularExpressions.MatchEvaluator]{ param($m) $field + $m.Value }, 1)
    $src = Replace-Once $src '        helper.register(Requiem.id("player_shell"), PLAYER_SHELL);' "        helper.register(Requiem.id(`"player_shell`"), PLAYER_SHELL);`r`n        helper.register(Requiem.id(`"parmenides_projection`"), PARMENIDES_PROJECTION);" 'Parmenides entity registration'
}
[IO.File]::WriteAllText($path, $src, [Text.UTF8Encoding]::new($false))

# 2) 1.21.1 ServerPlayer.teleportTo has seven parameters here, not the extra trailing boolean.
$path = Join-Path $root $files[1]
$src = [IO.File]::ReadAllText($path)
$src = Replace-Once $src 'player.teleportTo(target, player.getX(), player.getY(), player.getZ(), Set.of(), player.getYRot(), player.getXRot(), true);' 'player.teleportTo(target, player.getX(), player.getY(), player.getZ(), Set.of(), player.getYRot(), player.getXRot());' '1.21.1 dimension teleport signature'
[IO.File]::WriteAllText($path, $src, [Text.UTF8Encoding]::new($false))

# 3) NeoForge 1.21.1 uses GatherDataEvent itself and gates server providers through includeServer().
$path = Join-Path $root $files[2]
$src = [IO.File]::ReadAllText($path)
$src = Replace-Once $src 'public static void gatherServerData(GatherDataEvent.Server event) {' 'public static void gatherServerData(GatherDataEvent event) {' '1.21.1 GatherDataEvent type'
$src = Replace-Once $src '        event.addProvider(new EntityTypeTagsProvider(generator.getPackOutput(), event.getLookupProvider(), Requiem.MOD_ID) {' '        generator.addProvider(event.includeServer(), new EntityTypeTagsProvider(generator.getPackOutput(), event.getLookupProvider(), Requiem.MOD_ID, event.getExistingFileHelper()) {' '1.21.1 EntityTypeTagsProvider registration'
[IO.File]::WriteAllText($path, $src, [Text.UTF8Encoding]::new($false))

Write-Host ''
Write-Host 'Applied three targeted 1.21.1 compatibility fixes:' -ForegroundColor Green
Write-Host ' - Registered requiem:parmenides_projection'
Write-Host ' - Corrected ServerPlayer dimension teleport signature'
Write-Host ' - Migrated data generation to GatherDataEvent + includeServer()'
Write-Host ''
Write-Host 'Now running clean compile...'
& .\gradlew.bat clean compileJava --no-daemon
$exit = $LASTEXITCODE
if ($exit -ne 0) {
    Write-Host "Compile still fails (exit $exit). The edited files are backed up at: $backup" -ForegroundColor Yellow
    exit $exit
}
Write-Host 'compileJava PASSED.' -ForegroundColor Green
