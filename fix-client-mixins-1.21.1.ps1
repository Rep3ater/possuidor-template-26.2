$ErrorActionPreference = 'Stop'

$root = (Get-Location).Path
if (-not (Test-Path (Join-Path $root 'gradlew.bat'))) {
    throw "Run this script from the Requiem project root (gradlew.bat was not found): $root"
}

$javaRoot = Join-Path $root 'src\main\java\ladysnake\requiem\mixin\client'
$resourceRoot = Join-Path $root 'src\main\resources'
$rendererMixin = Join-Path $javaRoot 'LivingEntityRendererMixin.java'
$gameAccessor = Join-Path $javaRoot 'GameRendererAccessor.java'
$entityAccessor = Join-Path $javaRoot 'EntityRendererAccessor.java'
$mixinConfig = Join-Path $resourceRoot 'mixins.requiem.client.json'

foreach ($required in @($rendererMixin, $gameAccessor, $mixinConfig)) {
    if (-not (Test-Path $required)) {
        throw "Required project file not found: $required"
    }
}

$stamp = Get-Date -Format 'yyyyMMdd-HHmmss'
$backupRoot = Join-Path $root "_backup-client-mixins-1.21.1-$stamp"
New-Item -ItemType Directory -Path $backupRoot -Force | Out-Null

$filesToBackup = @(
    @{ Path = $rendererMixin; Relative = 'src\main\java\ladysnake\requiem\mixin\client\LivingEntityRendererMixin.java' },
    @{ Path = $gameAccessor; Relative = 'src\main\java\ladysnake\requiem\mixin\client\GameRendererAccessor.java' },
    @{ Path = $mixinConfig; Relative = 'src\main\resources\mixins.requiem.client.json' }
)
if (Test-Path $entityAccessor) {
    $filesToBackup += @{ Path = $entityAccessor; Relative = 'src\main\java\ladysnake\requiem\mixin\client\EntityRendererAccessor.java' }
}
foreach ($entry in $filesToBackup) {
    $destination = Join-Path $backupRoot $entry.Relative
    New-Item -ItemType Directory -Path (Split-Path $destination -Parent) -Force | Out-Null
    Copy-Item -LiteralPath $entry.Path -Destination $destination -Force
}

$utf8NoBom = New-Object System.Text.UTF8Encoding($false)

$rendererMixinSource = @'
package ladysnake.requiem.mixin.client;

import ladysnake.requiem.api.v1.remnant.RemnantComponent;
import ladysnake.requiem.common.sharedbody.SharedBodyClientState;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Makes ghost and shared-body entities translucent using the 1.21.1 renderer hierarchy.
 */
@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin<T extends LivingEntity, M extends net.minecraft.client.model.EntityModel<T>> {
    @Inject(method = "getRenderType", at = @At("HEAD"), cancellable = true)
    private void requiem$translucentSharedBody(T entity, boolean bodyVisible, boolean translucent, boolean glowing, CallbackInfoReturnable<RenderType> cir) {
        if (glowing) return;
        if (SharedBodyClientState.isControlled(entity.getId()) || RemnantComponent.isIncorporeal(entity)) {
            ResourceLocation texture = ((EntityRendererAccessor) (Object) this).requiem$getTextureLocation(entity);
            cir.setReturnValue(RenderType.entityTranslucentCull(texture));
        }
    }
}
'@

$entityAccessorSource = @'
package ladysnake.requiem.mixin.client;

import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Invokes the texture lookup declared on EntityRenderer and inherited by LivingEntityRenderer. */
@Mixin(EntityRenderer.class)
public interface EntityRendererAccessor {
    @Invoker("getTextureLocation")
    ResourceLocation requiem$getTextureLocation(Entity entity);
}
'@

$gameAccessorSource = @'
package ladysnake.requiem.mixin.client;

import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(GameRenderer.class)
public interface GameRendererAccessor {
    // 1.21.1 replaced the old setPostEffect method with loadEffect.
    @Invoker("loadEffect")
    void requiem$setPostEffect(ResourceLocation id);
}
'@

[System.IO.File]::WriteAllText($rendererMixin, $rendererMixinSource, $utf8NoBom)
[System.IO.File]::WriteAllText($entityAccessor, $entityAccessorSource, $utf8NoBom)
[System.IO.File]::WriteAllText($gameAccessor, $gameAccessorSource, $utf8NoBom)

$configText = [System.IO.File]::ReadAllText($mixinConfig)
if ($configText -notmatch '"EntityRendererAccessor"') {
    $configPattern = [regex]'("LivingEntityRendererMixin"\s*,)'
    $configText = $configPattern.Replace(
        $configText,
        '"EntityRendererAccessor",' + [Environment]::NewLine + '    $1',
        1
    )
    if ($configText -notmatch '"EntityRendererAccessor"') {
        throw 'Could not add EntityRendererAccessor to mixins.requiem.client.json; no changes were rolled back automatically. Backup is available at: ' + $backupRoot
    }
}
[System.IO.File]::WriteAllText($mixinConfig, $configText, $utf8NoBom)

# Validate the mixin list and the obsolete target before compiling.
$parsedConfig = Get-Content -LiteralPath $mixinConfig -Raw | ConvertFrom-Json
if ($parsedConfig.client -notcontains 'EntityRendererAccessor') {
    throw 'EntityRendererAccessor is missing from the client mixin list.'
}
if ((Get-Content -LiteralPath $gameAccessor -Raw) -notmatch '@Invoker\("loadEffect"\)') {
    throw 'GameRendererAccessor was not updated to the 1.21.1 loadEffect method.'
}
if ((Get-Content -LiteralPath $rendererMixin -Raw) -match '@Shadow') {
    throw 'LivingEntityRendererMixin still contains an invalid inherited-method shadow.'
}

Write-Host "Patched client mixins. Backup: $backupRoot" -ForegroundColor Cyan
Write-Host 'Running clean compileJava...' -ForegroundColor Cyan
& .\gradlew.bat clean compileJava
if ($LASTEXITCODE -ne 0) {
    throw "compileJava failed with exit code $LASTEXITCODE. Review the compiler output; backup: $backupRoot"
}

Write-Host ''
Write-Host 'BUILD SUCCESSFUL: compileJava passed.' -ForegroundColor Green
Write-Host 'Next test: .\gradlew.bat runClient' -ForegroundColor Yellow
