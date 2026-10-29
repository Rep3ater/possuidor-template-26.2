#!/usr/bin/env python3
"""Apply all Locki system fixes for the Requiem mod in possuidor-template-26.2."""

import os
import zipfile
import shutil
import subprocess
import sys

proj = "/c/Mods developer/possUIDor-template-26.2"
modified = []

def read_file(path):
    with open(path, 'r', encoding='utf-8') as f:
        return f.read()

def write_file(path, content):
    with open(path, 'w', encoding='utf-8') as f:
        f.write(content)

def patch_file(path, old, new, label):
    """Replace old string with new in file. Returns True if changed."""
    with open(path, 'r', encoding='utf-8') as f:
        content = f.read()
    if old not in content:
        print(f"  [ERROR] {label}: pattern not found!")
        return False
    content = content.replace(old, new)
    write_file(path, content)
    modified.append(path)
    print(f"  [OK] {label}")
    return True

# ============================================================
# 1. LockiRootNetwork.java - Roots Mapping 5 Blocks Too Low
# ============================================================
print("\n=== 1. LockiRootNetwork.java (Roots mapping) ===")
path = os.path.join(proj, "src/main/java/ladysnake/requiem/common/locki/LockiRootNetwork.java")
content = read_file(path)

# Fix 1a: anchor() - raise tree anchor height
old_anchor = "return new Vec3(pos.getX() + 0.5, pos.getY() + 0.035, pos.getZ() + 0.5);"
new_anchor = "return new Vec3(pos.getX() + 0.5, pos.getY() + (tree ? 1.0 : 0.05), pos.getZ() + 0.5);"
if old_anchor in content:
    content = content.replace(old_anchor, new_anchor)
    write_file(path, content)
    modified.append(path)
    print("  [OK] anchor() height raised: y + 0.035 -> y + (tree ? 1.0 : 0.05)")
else:
    print("  [ERROR] anchor() pattern not found")

# Fix 1b: undergroundAnchor() - shallow depth
old_underground = """    private static Vec3 undergroundAnchor(PlantNode node) {
        Vec3 surface = node.anchor();
        long seed = Math.abs(node.pos().getX() * 31L + node.pos().getZ() * 17L + node.pos().getY() * 13L);
        double depth = 2.2 + (seed % 17L) * 0.16;
        return new Vec3(surface.x, surface.y - depth, surface.z);
    }"""
new_underground = """    private static Vec3 undergroundAnchor(PlantNode node) {
        Vec3 surface = node.anchor();
        double depth = node.tree() ? 0.4 : 0.2;
        return new Vec3(surface.x, surface.y - depth, surface.z);
    }"""
if old_underground in content:
    content = content.replace(old_underground, new_underground)
    write_file(path, content)
    modified.append(path)
    print("  [OK] undergroundAnchor() depth: node.tree() ? 0.4 : 0.2")
else:
    print("  [ERROR] undergroundAnchor() pattern not found")

# ============================================================
# 2. LockiSpiritSystem.java - Multiple fixes
# ============================================================
print("\n=== 2. LockiSpiritSystem.java (Apple body, exit) ===")
path = os.path.join(proj, "src/main/java/ladysnake/requiem/common/locki/LockiSpiritSystem.java")
content = read_file(path)

# Fix 2a: handlePlantChoice() - prevent apple flying away
old_apple_pos = "ItemEntity appleEntity = new ItemEntity(level, fruitPos.getX() + 0.5, fruitPos.getY() + 0.35, fruitPos.getZ() + 0.5, apple);"
new_apple_pos = "ItemEntity appleEntity = new ItemEntity(level, fruitPos.getX() + 0.5, fruitPos.getY() + 0.1, fruitPos.getZ() + 0.5, apple);"
if old_apple_pos in content:
    content = content.replace(old_apple_pos, new_apple_pos)
    write_file(path, content)
    modified.append(path)
    print("  [OK] Apple spawn Y position: +0.35 -> +0.1")
else:
    print("  [ERROR] apple entity position pattern not found")

# Fix 2b: Set player.noPhysics = true when entering apple mode
old_nophysics = "        player.setInvisible(true);\n        player.noPhysics = false;"
new_nophysics = "        player.setInvisible(true);\n        player.noPhysics = true;"
if old_nophysics in content:
    content = content.replace(old_nophysics, new_nophysics)
    write_file(path, content)
    modified.append(path)
    print("  [OK] noPhysics set to true when entering apple mode")
else:
    print("  [ERROR] noPhysics pattern not found")

# Fix 2c: Player setDeltaMovement and setPos to apple position
old_setpos = "        player.setPos(appleEntity.getX(), appleEntity.getY(), appleEntity.getZ());"
new_setpos = """        player.setDeltaMovement(Vec3.ZERO);
        player.setPos(appleEntity.getX(), appleEntity.getY(), appleEntity.getZ());"""
if old_setpos in content:
    content = content.replace(old_setpos, new_setpos)
    write_file(path, content)
    modified.append(path)
    print("  [OK] player deltaMovement zeroed and setPos to apple pos")
else:
    print("  [ERROR] setPos pattern not found")

# Fix 2d: releaseApple() - teleport player to apple position, become mortal
old_release_return = """        setMode(player, MODE_SPIRIT);
        resetPhysicalState(player);
        syncState(player);
    }
}
"""
new_release_return = """        // Teleport player directly to the apple's world position
        player.teleportTo((ServerLevel) player.level(), x, y, z, Set.of(), player.getYRot(), player.getXRot(), false);
        // Transition Locki from incorporeal wandering soul back to a living mortal player
        ladysnake.requiem.api.v1.remnant.RemnantComponent remnant = ladysnake.requiem.common.RequiemComponents.REMNANT.get(player);
        if (remnant != null) {
            remnant.become(ladysnake.requiem.common.remnant.RemnantTypes.MORTAL);
        }
        // Exit Locki mode completely
        setMode(player, MODE_NONE);
        player.removeTag(TAG);
        player.setInvisible(false);
        player.noPhysics = false;
        resetPhysicalState(player);
        syncState(player);
    }
}
"""
# Replace the tail of the file
lines = content.rstrip().split('\n')
# Find the line with "setMode(player, MODE_SPIRIT);" at the end of releaseApple
idx = None
for i in range(len(lines)-1, -1, -1):
    if lines[i].strip() == "setMode(player, MODE_SPIRIT);" and i > 0 and "resetPhysicalState" in lines[i-1]:
        idx = i
        break

if idx is not None:
    # Replace from that line onwards with new code + closing brace
    new_lines = lines[:idx]
    new_lines.append("")
    new_lines.append("// Teleport player directly to the apple's world position")
    new_lines.append("        player.teleportTo((ServerLevel) player.level(), x, y, z, Set.of(), player.getYRot(), player.getXRot(), false);")
    new_lines.append("// Transition Locki from incorporeal wandering soul back to a living mortal player")
    new_lines.append("        ladysnake.requiem.api.v1.remnant.RemnantComponent remnant = ladysnake.requiem.common.RequiemComponents.REMNANT.get(player);")
    new_lines.append("        if (remnant != null) {")
    new_lines.append("            remnant.become(ladysnake.requiem.common.remnant.RemnantTypes.MORTAL);")
    new_lines.append("        }")
    new_lines.append("// Exit Locki mode completely")
    new_lines.append("        setMode(player, MODE_NONE);")
    new_lines.append("        player.removeTag(TAG);")
    new_lines.append("        player.setInvisible(false);")
    new_lines.append("        player.noPhysics = false;")
    new_lines.append("        resetPhysicalState(player);")
    new_lines.append("        syncState(player);")
    new_lines.append("    }")
    new_lines.append("}")
    content = '\n'.join(new_lines)
    write_file(path, content)
    modified.append(path)
    print("  [OK] releaseApple() now teleports player to apple pos, becomes mortal, MODE_NONE")
else:
    print("  [ERROR] releaseApple return pattern not found")

# Fix 2e: makeAppleStack() - remove hardcoded custom name
old_customname = "        stack.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME, net.minecraft.network.chat.Component.literal(\"Locki's living apple\"));"
if old_customname in content:
    content = content.replace(old_customname, "// DELETE or COMMENT OUT this line so it falls back to the clean lang key: // " + old_customname)
    write_file(path, content)
    modified.append(path)
    print("  [OK] CustomName hardcoded line commented out in makeAppleStack()")
else:
    print("  [ERROR] CUSTOM_NAME pattern not found")

# ============================================================
# 3. GameRendererMixin.java - Astral shader shutdown
# ============================================================
print("\n=== 3. GameRendererMixin.java (Astral shader shutdown) ===")
path = os.path.join(proj, "src/main/java/ladysnake/requiem/mixin/client/GameRendererMixin.java")
content = read_file(path)

old_shutdown = """    @Inject(method = "checkEntityPostEffect", at = @At("HEAD"), cancellable = true)
    private void requiem$checkEntityPostEffect(@Nullable Entity cameraEntity, CallbackInfo ci) {"""
new_shutdown = """    @Inject(method = "render", at = @At("HEAD"))
    private void requiem$shutdownLockiShader(net.minecraft.client.DeltaTracker deltaTracker, boolean renderLevel, CallbackInfo ci) {
        net.minecraft.resources.Identifier target = ladysnake.requiem.client.locki.LockiClientState.desiredPostEffect();
        if (target == null) {
            net.minecraft.resources.Identifier current = ((GameRendererAccessor) this).requiem$getPostEffect();
            if (ladysnake.requiem.client.RequiemClient.LOCKI_SPIRIT_POST_EFFECT.equals(current)
                    || ladysnake.requiem.client.RequiemClient.LOCKI_APPLE_POST_EFFECT.equals(current)) {
                ((GameRendererAccessor) this).requiem$shutdownPostEffect();
            }
        }
    }

    @Inject(method = "checkEntityPostEffect", at = @At("HEAD"), cancellable = true)
    private void requiem$checkEntityPostEffect(@Nullable Entity cameraEntity, CallbackInfo ci) {"""
if old_shutdown in content:
    content = content.replace(old_shutdown, new_shutdown)
    write_file(path, content)
    modified.append(path)
    print("  [OK] Added requiem$shutdownLockiShader inject at HEAD of render()")
else:
    print("  [ERROR] checkEntityPostEffect pattern not found")

# ============================================================
# 4. LockiClientState.java - Apple name + camera fix
# ============================================================
print("\n=== 4. LockiClientState.java (Camera fix + apple mode) ===")
path = os.path.join(proj, "src/main/java/ladysnake/requiem/client/locki/LockiClientState.java")
content = read_file(path)

# Fix 4a: camera - always use player as camera entity
old_camera = """        } else if ("apple".equals(mode)) {
            player.setPose(Pose.STANDING);
            setSwimmingPose = false;
            Entity cameraTarget = mc.level.getEntity(cameraEntityId);
            if (cameraTarget == null) cameraTarget = player;
            mc.setCameraEntity(cameraTarget);
            if (cameraMode == 2) {"""
new_camera = """        } else if ("apple".equals(mode)) {
            player.setPose(Pose.STANDING);
            setSwimmingPose = false;
            // Always keep the camera entity as the player to avoid mouse-look lock
            mc.setCameraEntity(player);
            if (cameraMode == 2) {"""
if old_camera in content:
    content = content.replace(old_camera, new_camera)
    write_file(path, content)
    modified.append(path)
    print("  [OK] Apple mode camera now always uses player entity")
else:
    print("  [ERROR] apple camera pattern not found")
    idx = content.find("mc.setCameraEntity(cameraTarget)")
    if idx >= 0:
        print(f"  [INFO] Found at idx {idx}: ...{content[idx:idx+120]}")

# Fix 4b: mode transitions to "none" - clear cache and reset state
old_accept_end = """        if ("conversation".equals(mode) && !"conversation".equals(previousMode)) {
            mc.setScreenAndShow(new LockiPlantDialogueScreen());
        } else if (!"conversation".equals(mode) && mc.gui.screen() instanceof LockiPlantDialogueScreen) {
            mc.gui.setScreen(null);
        }
    }"""
new_accept_end = """        if ("conversation".equals(mode) && !"conversation".equals(previousMode)) {
            mc.setScreenAndShow(new LockiPlantDialogueScreen());
        } else if (!"conversation".equals(mode) && mc.gui.screen() instanceof LockiPlantDialogueScreen) {
            mc.gui.setScreen(null);
        }
        if ("none".equals(mode)) {
            // Mode transitioned to none: clear all cached state
            cachedSegments = List.of();
            cachedPlants = List.of();
            lastNetworkBuild = Long.MIN_VALUE;
            previousMode = mode;
        }
    }"""
if old_accept_end in content:
    content = content.replace(old_accept_end, new_accept_end)
    write_file(path, content)
    modified.append(path)
    print("  [OK] Mode transition to 'none' now clears camera cache and settings")
else:
    print("  [ERROR] accept() end pattern not found")
    idx = content.find("mc.gui.setScreen(null)")
    if idx >= 0:
        print(f"  [INFO] Found at idx {idx}: ...{content[idx:idx+150]}")

# ============================================================
# 5. PlayerShellEntityRenderer.java - Skin from impersonated profile
# ============================================================
print("\n=== 5. PlayerShellEntityRenderer.java (Skin swapping) ===")
path = os.path.join(proj, "src/main/java/ladysnake/requiem/client/render/entity/PlayerShellEntityRenderer.java")
content = read_file(path)

old_extract = """    @Override
    public void extractRenderState(AbstractClientPlayer entity, AvatarRenderState state, float partialTicks) {
        if (entity.getSkin().model() == PlayerModelType.SLIM) {
            this.model = this.slimModel;
        } else {
            this.model = this.defaultModel;
        }
        super.extractRenderState(entity, state, partialTicks);
    }"""
new_extract = """    @Override
    public void extractRenderState(AbstractClientPlayer entity, AvatarRenderState state, float partialTicks) {
        // Use the impersonated profile's skin, not the entity's stored skin
        net.minecraft.client.resources.SkinManager skinManager = net.minecraft.client.Minecraft.getInstance().getSkinManager();
        com.mojang.authlib.GameProfile profile = entity.getGameProfile();
        if (profile != null) {
            try {
                skinManager.getInsecureSkin(profile);
            } catch (Exception ignored) {}
        }
        if (entity.getSkin().model() == PlayerModelType.SLIM) {
            this.model = this.slimModel;
        } else {
            this.model = this.defaultModel;
        }
        super.extractRenderState(entity, state, partialTicks);
    }"""
if old_extract in content:
    content = content.replace(old_extract, new_extract)
    write_file(path, content)
    modified.append(path)
    print("  [OK] PlayerShellEntityRenderer uses impersonated profile skin")
else:
    print("  [ERROR] extractRenderState pattern not found")

# ============================================================
# 6. LivingEntityRendererMixin.java - Hide astral Locki
# ============================================================
print("\n=== 6. LivingEntityRendererMixin.java (Hide astral Locki) ===")
path = os.path.join(proj, "src/main/java/ladysnake/requiem/mixin/client/LivingEntityRendererMixin.java")
content = read_file(path)

old_render = """    @Inject(method = "getModelTint", at = @At("RETURN"), cancellable = true)"""
new_render = """    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void requiem$hideAstralLocki(net.minecraft.world.entity.LivingEntity entity, float entityYaw, float partialTicks, com.mojang.blaze3d.vertex.PoseStack poseStack, net.minecraft.client.renderer.MultiBufferSource bufferSource, int packedLight, CallbackInfo ci) {
        if (entity instanceof net.minecraft.world.entity.player.Player player) {
            String lockiMode = player.getPersistentData().getString("requiem:locki_mode").orElse("");
            if ("spirit".equals(lockiMode) || "roots".equals(lockiMode)) {
                // Hide Locki when not the local player
                if (player != net.minecraft.client.Minecraft.getInstance().player) {
                    ci.cancel();
                }
            }
        }
    }

    @Inject(method = "getModelTint", at = @At("RETURN"), cancellable = true)"""
if old_render in content:
    content = content.replace(old_render, new_render)
    write_file(path, content)
    modified.append(path)
    print("  [OK] Added requiem$hideAstralLocki inject at HEAD of render()")
else:
    print("  [ERROR] getModelTint pattern not found")

# ============================================================
# 7. Requiem.java - Register LockiDebugCommand
# ============================================================
print("\n=== 7. Requiem.java (Register /locki commands) ===")
path = os.path.join(proj, "src/main/java/ladysnake/requiem/Requiem.java")
content = read_file(path)

old_cmd_reg = """    private void onRegisterCommands(RegisterCommandsEvent event) {
        RequiemCommand.register(event.getDispatcher());
    }"""
new_cmd_reg = """    private void onRegisterCommands(RegisterCommandsEvent event) {
        RequiemCommand.register(event.getDispatcher());
        ladysnake.requiem.common.command.LockiDebugCommand.lockiSubcommand().register(event.getDispatcher());
    }"""
if old_cmd_reg in content:
    content = content.replace(old_cmd_reg, new_cmd_reg)
    write_file(path, content)
    modified.append(path)
    print("  [OK] LockiDebugCommand registered in onRegisterCommands")
else:
    print("  [ERROR] onRegisterCommands pattern not found")

# Rename LockiDebugCommand.java.disabled -> LockiDebugCommand.java
cmd_path = os.path.join(proj, "src/main/java/ladysnake/requiem/common/command/LockiDebugCommand.java.disabled")
backup_path = os.path.join(proj, "src/main/java/ladysnake/requiem/common/command/LockiDebugCommand.java.backup")
target_path = os.path.join(proj, "src/main/java/ladysnake/requiem/common/command/LockiDebugCommand.java")
if os.path.exists(cmd_path) and not os.path.exists(target_path):
    shutil.copy2(cmd_path, target_path)
    os.remove(cmd_path)
    modified.append("LockiDebugCommand.java.disabled -> LockiDebugCommand.java (renamed)")
    print("  [OK] Renamed LockiDebugCommand.java.disabled -> LockiDebugCommand.java")
elif os.path.exists(target_path):
    print("  [INFO] LockiDebugCommand.java already exists")
else:
    print("  [ERROR] LockiDebugCommand.java.disabled not found")

# ============================================================
# 8. locki_apple.json - Use vanilla apple texture
# ============================================================
print("\n=== 8. locki_apple.json (Vanilla texture) ===")
path = os.path.join(proj, "src/main/resources/assets/requiem/models/item/locki_apple.json")
content = read_file(path)

old_model = """{
  "parent": "minecraft:item/generated",
  "textures": {
    "layer0": "requiem:item/locki_apple"
  }
}"""
new_model = """{
  "parent": "minecraft:item/generated",
  "textures": {
    "layer0": "minecraft:item/apple"
  }
}"""
if old_model in content:
    content = content.replace(old_model, new_model)
    write_file(path, content)
    modified.append(path)
    print("  [OK] locki_apple.json now uses minecraft:item/apple")
else:
    print("  [ERROR] locki_apple.json pattern not found")

# ============================================================
# 9. en_us.json - Ensure proper localization key
# ============================================================
print("\n=== 9. en_us.json (Localization) ===")
path = os.path.join(proj, "src/main/resources/assets/requiem/lang/en_us.json")
content = read_file(path)

old_name = '"item.requiem.locki_apple": "Locki\u2019s living apple",'
new_name = '"item.requiem.locki_apple": "Living Apple",'
content = content.replace(old_name, new_name)
write_file(path, content)
modified.append(path)
print("  [OK] locki_apple localization -> \"Living Apple\"")

# ============================================================
# Summary
# ============================================================
print("\n" + "=" * 60)
print(f"SUMMARY: {len(modified)} files modified")
for f in modified:
    print(f"  - {f}")
print("=" * 60)
