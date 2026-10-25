$srcDir = "C:\Users\gusta\.gemini\antigravity\brain\bf263c0c-fb4f-4dbb-a309-748a369cf583\scratch\requiem_src\Requiem-1.19\requiem-core\src\main\java"
$destDir = "src\main\java"
$utf8NoBom = New-Object System.Text.UTF8Encoding($false)

$files = Get-ChildItem -Path $srcDir -Recurse -Filter "*.java"
$count = 0

foreach ($f in $files) {
    $relPath = $f.FullName.Substring($srcDir.Length + 1)
    $targetPath = Join-Path $destDir $relPath
    $targetParent = Split-Path -Parent $targetPath
    if (-not (Test-Path $targetParent)) {
        New-Item -ItemType Directory -Path $targetParent -Force | Out-Null
    }

    $content = [System.IO.File]::ReadAllText($f.FullName, [System.Text.Encoding]::UTF8)

    # Apply mappings
    $content = $content.Replace("net.minecraft.util.Identifier", "net.minecraft.resources.Identifier")
    $content = $content.Replace("net.minecraft.util.registry.RegistryKey", "net.minecraft.resources.ResourceKey")
    $content = $content.Replace("net.minecraft.util.registry.Registry", "net.minecraft.core.Registry")
    $content = $content.Replace("net.minecraft.util.registry.SimpleRegistry", "net.minecraft.core.MappedRegistry")
    $content = $content.Replace("net.minecraft.entity.player.PlayerEntity", "net.minecraft.world.entity.player.Player")
    $content = $content.Replace("net.minecraft.server.network.ServerPlayerEntity", "net.minecraft.server.level.ServerPlayer")
    $content = $content.Replace("net.minecraft.entity.LivingEntity", "net.minecraft.world.entity.LivingEntity")
    $content = $content.Replace("net.minecraft.entity.Entity", "net.minecraft.world.entity.Entity")
    $content = $content.Replace("net.minecraft.entity.mob.MobEntity", "net.minecraft.world.entity.Mob")
    $content = $content.Replace("net.minecraft.entity.EntityType", "net.minecraft.world.entity.EntityType")
    $content = $content.Replace("net.minecraft.world.World", "net.minecraft.world.level.Level")
    $content = $content.Replace("net.minecraft.server.world.ServerWorld", "net.minecraft.server.level.ServerLevel")
    $content = $content.Replace("net.minecraft.util.math.BlockPos", "net.minecraft.core.BlockPos")
    $content = $content.Replace("net.minecraft.util.math.Vec3d", "net.minecraft.world.phys.Vec3")
    $content = $content.Replace("net.minecraft.util.math.Box", "net.minecraft.world.phys.AABB")
    $content = $content.Replace("net.minecraft.util.math.Direction", "net.minecraft.core.Direction")
    $content = $content.Replace("net.minecraft.util.Hand", "net.minecraft.world.InteractionHand")
    $content = $content.Replace("net.minecraft.util.ActionResult", "net.minecraft.world.InteractionResult")
    $content = $content.Replace("net.minecraft.util.TypedActionResult", "net.minecraft.world.InteractionResult")
    $content = $content.Replace("net.minecraft.text.Text", "net.minecraft.network.chat.Component")
    $content = $content.Replace("net.minecraft.text.TranslatableTextContent", "net.minecraft.network.chat.contents.TranslatableContents")
    $content = $content.Replace("net.minecraft.item.ItemStack", "net.minecraft.world.item.ItemStack")
    $content = $content.Replace("net.minecraft.item.Item", "net.minecraft.world.item.Item")
    $content = $content.Replace("net.minecraft.item.Items", "net.minecraft.world.item.Items")
    $content = $content.Replace("net.minecraft.nbt.NbtCompound", "net.minecraft.nbt.CompoundTag")
    $content = $content.Replace("net.minecraft.nbt.NbtElement", "net.minecraft.nbt.Tag")
    $content = $content.Replace("net.minecraft.nbt.NbtList", "net.minecraft.nbt.ListTag")
    $content = $content.Replace("net.minecraft.network.PacketByteBuf", "net.minecraft.network.RegistryFriendlyByteBuf")
    $content = $content.Replace("net.minecraft.entity.damage.DamageSource", "net.minecraft.world.damagesource.DamageSource")
    $content = $content.Replace("net.minecraft.entity.effect.StatusEffect", "net.minecraft.world.effect.MobEffect")
    $content = $content.Replace("net.minecraft.entity.effect.StatusEffectInstance", "net.minecraft.world.effect.MobEffectInstance")
    $content = $content.Replace("net.minecraft.entity.attribute.EntityAttributes", "net.minecraft.world.entity.ai.attributes.Attributes")
    $content = $content.Replace("net.minecraft.sound.SoundEvent", "net.minecraft.sounds.SoundEvent")
    $content = $content.Replace("net.minecraft.sound.SoundEvents", "net.minecraft.sounds.SoundEvents")
    $content = $content.Replace("net.minecraft.sound.SoundCategory", "net.minecraft.sounds.SoundSource")
    $content = $content.Replace("net.minecraft.util.math.MathHelper", "net.minecraft.util.Mth")
    $content = $content.Replace("net.minecraft.entity.EquipmentSlot", "net.minecraft.world.entity.EquipmentSlot")
    $content = $content.Replace("net.minecraft.entity.player.PlayerInventory", "net.minecraft.world.entity.player.Inventory")
    $content = $content.Replace("net.minecraft.resource.ResourceManager", "net.minecraft.server.packs.resources.ResourceManager")

    # Word replacements
    $content = [System.Text.RegularExpressions.Regex]::Replace($content, '\bPlayerEntity\b', 'Player')
    $content = [System.Text.RegularExpressions.Regex]::Replace($content, '\bServerPlayerEntity\b', 'ServerPlayer')
    $content = [System.Text.RegularExpressions.Regex]::Replace($content, '\bMobEntity\b', 'Mob')
    $content = [System.Text.RegularExpressions.Regex]::Replace($content, '\bServerWorld\b', 'ServerLevel')
    $content = [System.Text.RegularExpressions.Regex]::Replace($content, '\bVec3d\b', 'Vec3')
    $content = [System.Text.RegularExpressions.Regex]::Replace($content, '\bNbtCompound\b', 'CompoundTag')
    $content = [System.Text.RegularExpressions.Regex]::Replace($content, '\bNbtElement\b', 'Tag')
    $content = [System.Text.RegularExpressions.Regex]::Replace($content, '\bNbtList\b', 'ListTag')
    $content = [System.Text.RegularExpressions.Regex]::Replace($content, '\bPacketByteBuf\b', 'RegistryFriendlyByteBuf')
    $content = [System.Text.RegularExpressions.Regex]::Replace($content, '\bStatusEffect\b', 'MobEffect')
    $content = [System.Text.RegularExpressions.Regex]::Replace($content, '\bStatusEffectInstance\b', 'MobEffectInstance')
    $content = [System.Text.RegularExpressions.Regex]::Replace($content, '\bEntityAttributes\b', 'Attributes')
    $content = [System.Text.RegularExpressions.Regex]::Replace($content, '\bSoundCategory\b', 'SoundSource')
    $content = [System.Text.RegularExpressions.Regex]::Replace($content, '\bMathHelper\b', 'Mth')
    $content = [System.Text.RegularExpressions.Regex]::Replace($content, '\bPlayerInventory\b', 'Inventory')
    $content = [System.Text.RegularExpressions.Regex]::Replace($content, '\bRegistryKey\b', 'ResourceKey')
    $content = [System.Text.RegularExpressions.Regex]::Replace($content, '\bText\.translatable\b', 'Component.translatable')
    $content = [System.Text.RegularExpressions.Regex]::Replace($content, '\bText\.literal\b', 'Component.literal')
    $content = [System.Text.RegularExpressions.Regex]::Replace($content, '\bText\b', 'Component')

    [System.IO.File]::WriteAllText($targetPath, $content, $utf8NoBom)
    $count++
}

Write-Host "Migrated $count core files without BOM."
