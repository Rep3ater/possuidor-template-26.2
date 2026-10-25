/*
 * Requiem
 * Copyright (C) 2017-2023 Ladysnake
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses>.
 *
 * Linking this mod statically or dynamically with other
 * modules is making a combined work based on this mod.
 * Thus, the terms and conditions of the GNU General Public License cover the whole combination.
 *
 * In addition, as a special exception, the copyright holders of
 * this mod give you permission to combine this mod
 * with free software programs or libraries that are released under the GNU LGPL
 * and with code included in the standard release of Minecraft under All Rights Reserved (or
 * modified versions of such code, with unchanged license).
 * You may copy and distribute such a system following the terms of the GNU GPL for this mod
 * and the licenses of the other code concerned.
 *
 * Note that people who make modified versions of this mod are not obligated to grant
 * this special exception for their modified versions; it is their choice whether to do so.
 * The GNU General Public License gives permission to release a modified version without this exception;
 * this exception also makes it possible to release a modified version which carries forward this exception.
 */
package ladysnake.requiem.common.remnant;

import com.mojang.authlib.GameProfile;
import dev.onyxstudios.cca.api.v3.component.ComponentKey;
import dev.onyxstudios.cca.api.v3.component.ComponentProvider;
import dev.onyxstudios.cca.api.v3.entity.RespawnCopyStrategy;
import dev.onyxstudios.cca.internal.base.AbstractComponentContainer;
import io.github.ladysnake.impersonate.Impersonator;
import ladysnake.requiem.api.v1.entity.InventoryLimiter;
import ladysnake.requiem.api.v1.event.requiem.PlayerShellEvents;
import ladysnake.requiem.api.v1.event.requiem.PossessionEvents;
import ladysnake.requiem.api.v1.event.requiem.SoulboundStackCheckCallback;
import ladysnake.requiem.api.v1.remnant.PlayerSplitResult;
import ladysnake.requiem.api.v1.remnant.RemnantComponent;
import ladysnake.requiem.api.v1.remnant.SoulbindingRegistry;
import ladysnake.requiem.common.entity.PlayerShellEntity;
import ladysnake.requiem.common.entity.RequiemEntities;
import ladysnake.requiem.core.RequiemCore;
import ladysnake.requiem.core.record.EntityPositionClerk;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.resources.Identifier;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.util.ProblemReporter;

import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public final class PlayerSplitter {
    public static final Identifier BODY_IMPERSONATION = RequiemCore.id("body_impersonation");

    /**
     * @see RemnantComponent#splitPlayer(boolean)
     */
    public static PlayerSplitResult doSplit(ServerPlayer whole) {
        PlayerShellEntity shell = createShell(whole);
        Entity mount = whole.getVehicle();
        int experience = whole.totalExperience;
        EntityPositionClerk.transferRecord(whole, shell);
        ServerPlayer soul = performRespawn(whole);
        RemnantComponent.get(soul).setVagrant(true);
        // Respawning in some conditions can place us away from the shell
        soul.setShiftKeyDown(whole.isShiftKeyDown());
        soul.connection.teleport(shell.getX(), shell.getY(), shell.getZ(), shell.getYRot(), shell.getXRot());
        soul.giveExperiencePoints(experience);
        transferSoulboundItems(shell, soul);
        soul.level().addFreshEntity(shell);
        if (mount != null) shell.startRiding(mount);
        PlayerBodyTracker.get(soul).setAnchor(EntityPositionClerk.get(shell).getOrCreateRecord());
        PlayerShellEvents.PLAYER_SPLIT.invoker().onPlayerSplit(whole, soul, shell);
        return new PlayerSplitResult(soul, shell);
    }

    private static void transferSoulboundItems(PlayerShellEntity shell, ServerPlayer soul) {
        // Inverted because we are doing things backwards (moving items back to player)
        boolean transferAll = !PossessionEvents.INVENTORY_TRANSFER_CHECK.invoker().shouldTransfer(soul, shell).get();

        for (int i = 0; i < soul.getInventory().getContainerSize(); i++) {
            if (transferAll || SoulboundStackCheckCallback.EVENT.invoker().isSoulbound(shell.getInventory(), shell.getInventory().getItem(i), i)) {
                soul.getInventory().setItem(i, shell.getInventory().removeItem(i, shell.getInventory().getItem(i).getCount()));
            }
        }
    }

    public static PlayerShellEntity createShell(ServerPlayer whole) {
        PlayerShellEntity shell = new PlayerShellEntity(RequiemEntities.PLAYER_SHELL, (ServerLevel) whole.level());
        shell.setPos(whole.getX(), whole.getY(), whole.getZ());
        shell.setYRot(whole.getYRot());
        shell.setXRot(whole.getXRot());
        shell.setGameMode(whole.gameMode.getGameModeForPlayer());  // use same gamemode for deserialization
        shell.storePlayerData(whole, computeCopyNbt(whole));
        shell.setYHeadRot(whole.getYHeadRot());
        shell.setYBodyRot(whole.yBodyRot);
        GameType shellGm = whole.gameMode.isSurvival() ? whole.gameMode.getGameModeForPlayer() : GameType.SURVIVAL;
        shell.setGameMode(shellGm);
        // setGameMode has no effect when the gamemode stays the same, so we make sure abilities work as intended
        shellGm.updatePlayerAbilities(shell.getAbilities());
        RemnantComponent.get(shell).become(RemnantTypes.MORTAL, true);
        InventoryLimiter.instance().disable(shell);
        for (MobEffectInstance effect : List.copyOf(shell.getActiveEffects())) {
            if (SoulbindingRegistry.instance().isSoulbound(effect.getEffect().value())) {
                shell.removeEffect(effect.getEffect());
            }
        }
        PlayerShellEvents.DATA_TRANSFER.invoker().transferData(whole, shell, false);
        return shell;
    }

    public static void doMerge(PlayerShellEntity shell, ServerPlayer soul) {
        Entity mount = shell.getVehicle();
        shell.stopRiding();
        mergeInventories(shell, soul);
        soul.getInventory().dropAll();  // ensure modded inventories do not get voided, hopefully
        // Note: the teleport request must be before deserialization, as it only encodes the required relative movement
        soul.connection.teleport(shell.getX(), shell.getY(), shell.getZ(), shell.getYRot(), shell.getXRot());
        // override common data that may have been altered during this shell's existence
        performNbtCopy(computeCopyNbt(shell), soul);
        soul.giveExperiencePoints(shell.totalExperience);
        EntityPositionClerk.transferRecord(shell, soul);
        PlayerShellEvents.DATA_TRANSFER.invoker().transferData(shell, soul, true);

        soul.connection.send(new ClientboundSetEntityDataPacket(soul.getId(), soul.getEntityData().getNonDefaultValues()));

        shell.remove(Entity.RemovalReason.DISCARDED);
        if (mount != null) soul.startRiding(mount);

        GameProfile shellProfile = shell.getDisplayProfile();
        if (shellProfile != null && !Objects.equals(shellProfile.id(), soul.getUUID())) {
            Impersonator.get(soul).impersonate(BODY_IMPERSONATION, shellProfile);
        }

        Objects.requireNonNull(soul.level().getServer()).getPlayerList().sendPlayerPermissionLevel(soul);
        PlayerShellEvents.PLAYER_MERGED.invoker().onPlayerMerge(soul, shell, shell.getDisplayProfile());
    }

    private static void mergeInventories(PlayerShellEntity shell, ServerPlayer soul) {
        Inventory shellInventory = shell.getInventory();
        Inventory soulInventory = soul.getInventory();

        for (int i = 0; i < soulInventory.getContainerSize(); i++) {
            ItemStack stack = soulInventory.removeItem(i, soulInventory.getItem(i).getCount());

            if (!stack.isEmpty()) {
                if (shellInventory.getItem(i).isEmpty()) {
                    shellInventory.setItem(i, stack);
                } else {
                    soul.drop(stack, false);
                }
            }
        }
    }

    public static ServerPlayer performRespawn(ServerPlayer player) {
        //Setup location for dummy
        GameRules rules = player.level().getGameRules();
        boolean keepInv = rules.get(GameRules.KEEP_INVENTORY);
        ServerPlayer.RespawnConfig config = player.getRespawnConfig();
        player.setRespawnPosition(null, false);

        try {
            rules.set(GameRules.KEEP_INVENTORY, false, player.level().getServer());
            ServerPlayer clone = player.level().getServer().getPlayerList().respawn(player, false, Entity.RemovalReason.DISCARDED);
            clone.setRespawnPosition(config, false);
            player.connection.player = clone;
            return clone;
        } finally {
            player.setRespawnPosition(config, false);
            rules.set(GameRules.KEEP_INVENTORY, keepInv, player.level().getServer());
        }
    }

    private static CompoundTag computeCopyNbt(Entity template) {
        //Player keeps everything that goes through death
        TagValueOutput out = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, template.registryAccess());
        template.saveWithoutId(out);
        CompoundTag templateNbt = out.buildResult();
        deduplicateVanillaData(templateNbt);
        deduplicateComponents(templateNbt, ComponentProvider.fromEntity(template).getComponentContainer().keys());
        return templateNbt;
    }

    private static void deduplicateComponents(CompoundTag leftoverData, Set<ComponentKey<?>> keys) {
        leftoverData.getCompound(AbstractComponentContainer.NBT_KEY).ifPresent(leftoverComponents -> {
            for (ComponentKey<?> key : keys) {
                String keyId = key.getId().toString();
                if (RespawnCopyStrategy.get(key) == RespawnCopyStrategy.ALWAYS_COPY) {
                    // avoid duplicating soulbound data
                    leftoverComponents.remove(keyId);
                }
            }
        });
    }

    private static void deduplicateVanillaData(CompoundTag leftoverData) {
        leftoverData.remove("UUID");
        leftoverData.remove("SpawnX");
        leftoverData.remove("SpawnY");
        leftoverData.remove("SpawnZ");
        leftoverData.remove("SpawnForced");
        leftoverData.remove("SpawnAngle");
        leftoverData.remove("SpawnDimension");
        leftoverData.remove("EnderItems");
        leftoverData.remove("abilities");
        leftoverData.remove("playerGameType");
        leftoverData.remove("previousPlayerGameType");
        leftoverData.remove("seenCredits");
        leftoverData.remove("XpP");
        leftoverData.remove("XpLevel");
        leftoverData.remove("XpTotal");
    }

    public static void performNbtCopy(CompoundTag from, Entity to) {
        TagValueOutput out = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, to.registryAccess());
        to.saveWithoutId(out);
        CompoundTag serialized = out.buildResult();
        serialized.merge(from);
        to.load(TagValueInput.create(ProblemReporter.DISCARDING, to.registryAccess(), serialized));
    }
}
