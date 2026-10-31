$ErrorActionPreference = 'Stop'
$root = (Get-Location).Path

$accessor = Join-Path $root 'src\main\java\ladysnake\requiem\core\mixin\access\LivingEntityAccessor.java'
$caller   = Join-Path $root 'src\main\java\ladysnake\requiem\core\possession\PossessionComponentImpl.java'

foreach ($file in @($accessor, $caller)) {
    if (-not (Test-Path -LiteralPath $file)) {
        throw "Required source file not found: $file`nRun this script from the project root."
    }
}

$backupDir = Join-Path $root ( '_backup-livingentity-drop-equipment-' + (Get-Date -Format 'yyyyMMdd-HHmmss') )
New-Item -ItemType Directory -Path $backupDir -Force | Out-Null
Copy-Item -LiteralPath $accessor -Destination (Join-Path $backupDir 'LivingEntityAccessor.java')
Copy-Item -LiteralPath $caller -Destination (Join-Path $backupDir 'PossessionComponentImpl.java')

$accessorText = [System.IO.File]::ReadAllText($accessor)
$oldAccessor = @'
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(LivingEntity.class)
public interface LivingEntityAccessor {
    @Invoker("dropEquipment")
    void requiem$invokeDropEquipment(ServerLevel serverLevel);
}
'@
$newAccessor = @'
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(LivingEntity.class)
public interface LivingEntityAccessor {
    @Invoker("dropEquipment")
    void requiem$invokeDropEquipment();
}
'@

if ($accessorText.Contains('requiem$invokeDropEquipment(ServerLevel serverLevel)')) {
    $accessorText = $accessorText.Replace($oldAccessor, $newAccessor)
    if ($accessorText.Contains('requiem$invokeDropEquipment(ServerLevel serverLevel)')) {
        # Fallback for harmless formatting differences while still requiring a unique method declaration.
        $accessorText = [regex]::Replace(
            $accessorText,
            '(?m)^import net\.minecraft\.server\.level\.ServerLevel;\r?\n',
            ''
        )
        $accessorText = [regex]::Replace(
            $accessorText,
            'requiem\$invokeDropEquipment\(ServerLevel\s+serverLevel\)',
            'requiem$invokeDropEquipment()'
        )
    }
} elseif (-not $accessorText.Contains('requiem$invokeDropEquipment()')) {
    throw 'LivingEntityAccessor has an unexpected declaration. No source edits were applied.'
}

$callerText = [System.IO.File]::ReadAllText($caller)
$oldCall = @'
            if (player.level() instanceof ServerLevel serverLevel) {
                ((LivingEntityAccessor) player).requiem$invokeDropEquipment(serverLevel);
            }
'@
$newCall = @'
            if (player.level() instanceof ServerLevel) {
                ((LivingEntityAccessor) player).requiem$invokeDropEquipment();
            }
'@
if ($callerText.Contains('requiem$invokeDropEquipment(serverLevel)')) {
    $callerText = $callerText.Replace($oldCall, $newCall)
    if ($callerText.Contains('requiem$invokeDropEquipment(serverLevel)')) {
        $callerText = [regex]::Replace(
            $callerText,
            '(?s)if\s*\(player\.level\(\)\s+instanceof\s+ServerLevel\s+serverLevel\)\s*\{\s*\(\(LivingEntityAccessor\)\s*player\)\.requiem\$invokeDropEquipment\(serverLevel\);\s*\}',
            'if (player.level() instanceof ServerLevel) {' + "`r`n" + '                ((LivingEntityAccessor) player).requiem$invokeDropEquipment();' + "`r`n" + '            }'
        )
    }
} elseif (-not $callerText.Contains('requiem$invokeDropEquipment()')) {
    throw 'PossessionComponentImpl has an unexpected accessor call. No source edits were applied.'
}

if ($accessorText.Contains('requiem$invokeDropEquipment(ServerLevel') -or $callerText.Contains('requiem$invokeDropEquipment(serverLevel)')) {
    throw 'Old ServerLevel accessor signature remains; refusing to write partial changes.'
}

[System.IO.File]::WriteAllText($accessor, $accessorText, [System.Text.UTF8Encoding]::new($false))
[System.IO.File]::WriteAllText($caller, $callerText, [System.Text.UTF8Encoding]::new($false))

Write-Host 'Updated LivingEntityAccessor to invoke dropEquipment() with no arguments.' -ForegroundColor Green
Write-Host 'Updated possession dissociation call to match the 1.21.1 signature.' -ForegroundColor Green
Write-Host "Backup: $backupDir"
Write-Host ''
Write-Host 'Running clean Java compilation...' -ForegroundColor Cyan
& .\gradlew.bat clean compileJava
if ($LASTEXITCODE -ne 0) {
    throw "compileJava failed with exit code $LASTEXITCODE. Backup is available at: $backupDir"
}
Write-Host ''
Write-Host 'compileJava PASSED. Next run: .\gradlew.bat runClient' -ForegroundColor Green
