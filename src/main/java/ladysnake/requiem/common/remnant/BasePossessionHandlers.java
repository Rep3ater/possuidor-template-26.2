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

import dev.onyxstudios.cca.api.v3.entity.TrackingStartCallback;
import ladysnake.requiem.Requiem;
import ladysnake.requiem.api.v1.entity.ExternalJumpingMount;
import ladysnake.requiem.api.v1.event.minecraft.JumpingMountEvents;
import ladysnake.requiem.api.v1.event.minecraft.MobConversionCallback;
import ladysnake.requiem.api.v1.event.requiem.PossessionEvents;
import ladysnake.requiem.api.v1.event.requiem.PossessionStartCallback;
import ladysnake.requiem.api.v1.possession.Possessable;
import ladysnake.requiem.api.v1.possession.PossessedData;
import ladysnake.requiem.api.v1.possession.PossessionComponent;
import ladysnake.requiem.common.advancement.criterion.RequiemCriteria;
import ladysnake.requiem.common.entity.effect.AttritionStatusEffect;
import ladysnake.requiem.common.gamerule.RequiemGamerules;
import ladysnake.requiem.common.tag.RequiemEntityTypeTags;
import ladysnake.requiem.core.entity.SoulHolderComponent;
import ladysnake.requiem.core.possession.PossessedDataBase;
import ladysnake.requiem.core.tag.RequiemCoreTags;
import ladysnake.requiem.mixin.common.access.EndermanEntityAccessor;
import net.fabricmc.fabric.api.util.TriState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.protocol.game.ClientboundGameEventPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.Level;

public final class BasePossessionHandlers {

    public static void register() {
        TrackingStartCallback.EVENT.register((player, tracked) -> {
            if (tracked instanceof Possessable) {
                // Synchronize possessed entities with their possessor / other players
                Player possessor = ((Possessable) tracked).getPossessor();
                if (possessor != null) {
                    PossessionComponent.KEY.sync(possessor);
                }
            }
        });
        PossessionEvents.INVENTORY_TRANSFER_CHECK.register(
            (possessor, host) -> possessor.level().getGameRules().get(RequiemGamerules.POSSESSION_KEEP_INVENTORY).shouldTransfer(host.isAlive()) ? TriState.TRUE : TriState.DEFAULT
        );
        PossessionEvents.HOST_DEATH.register((player, host, deathCause) -> {
            if (player.level().getLevelData().isHardcore()) {
                player.hurt(player.damageSources().fellOutOfWorld(), Float.MAX_VALUE);
            } else {
                AttritionStatusEffect.apply(player);
            }
        });
        PossessionStartCallback.EVENT.register(Requiem.id("blacklist"), (target, possessor, simulate) -> {
            if (target.getType().builtInRegistryHolder().is(RequiemCoreTags.Entity.POSSESSION_BLACKLIST)) {
                return PossessionStartCallback.Result.DENY;
            }
            return PossessionStartCallback.Result.PASS;
        });
        // Locki is a special spirit with an intentionally narrow host list. Other players keep
        // Requiem's normal possession rules; only entities tagged "locki" receive this restriction.
        PossessionStartCallback.EVENT.register(Requiem.id("locki_host_filter"), (target, possessor, simulate) -> {
            if (!possessor.entityTags().contains("locki")) return PossessionStartCallback.Result.PASS;
            boolean construct = target instanceof ladysnake.requiem.common.construct.entity.RoboticConstructEntity;
            String path = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(target.getType()).getPath();
            boolean treeOrPlant = target.getType().builtInRegistryHolder().is(ladysnake.requiem.common.tag.RequiemEntityTypeTags.LOCKI_POSSESSABLE_PLANTS)
                || path.contains("tree") || path.contains("plant") || path.contains("root") || path.contains("sapling");
            return construct || treeOrPlant ? PossessionStartCallback.Result.ALLOW : PossessionStartCallback.Result.DENY;
        });
        PossessionEvents.POST_RESURRECTION.register(RequiemCriteria.PLAYER_RESURRECTED_AS_ENTITY::handle);
        PossessionStartCallback.EVENT.register(Requiem.id("base_mobs"), (target, possessor, simulate) -> {
            if (target.getType().builtInRegistryHolder().is(RequiemEntityTypeTags.POSSESSABLES)) {
                return PossessionStartCallback.Result.ALLOW;
            }
            return PossessionStartCallback.Result.PASS;
        });
        PossessionStartCallback.EVENT.register(Requiem.id("converted_mobs"), (target, possessor, simulate) -> {
            if (PossessedData.KEY.get(target).wasConvertedUnderPossession()) {
                return PossessionStartCallback.Result.ALLOW;
            }
            return PossessionStartCallback.Result.PASS;
        });
        PossessionStartCallback.EVENT.register(Requiem.id("soulless"), (target, possessor, simulate) -> {
            if (SoulHolderComponent.isSoulless(target)) {
                return PossessionStartCallback.Result.ALLOW;
            }
            return PossessionStartCallback.Result.PASS;
        });
        JumpingMountEvents.MOUNT_CHECK.register(ExternalJumpingMount.KEY::getNullable);
        MobConversionCallback.EVENT.register(PossessedDataBase::onMobConverted);
        MobConversionCallback.EVENT.register((original, converted) -> dropArmorIfBanned(converted));
        MobConversionCallback.EVENT.register(SoulHolderComponent::onMobConverted);
    }

    public static void performEndermanSoulAction(Mob target, Player possessor) {
        Entity tpDest;
        if (possessor.level().dimension() != Level.END) {
            // Retry a few times
            for (int i = 0; i < 20; i++) {
                if (((EndermanEntityAccessor) target).requiem$invokeTeleport()) {
                    possessor.level().playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.ENDERMAN_TELEPORT, target.getSoundSource(), 1.0F, 1.0F);
                    break;
                }
            }
            tpDest = target;
        } else {
            possessor.level().playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.ENDERMAN_TELEPORT, target.getSoundSource(), 1.0F, 1.0F);
            ServerLevel destination = possessor.level().getServer() != null ? possessor.level().getServer().getLevel(Level.OVERWORLD) : null;
            if (destination != null) {
                possessor.teleportTo(destination, possessor.getX(), possessor.getY(), possessor.getZ(), java.util.Set.of(), possessor.getYRot(), possessor.getXRot(), true);
                ((ServerPlayer) possessor).connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.WIN_GAME, 0.0F));
                target.teleportTo(destination, possessor.getX(), possessor.getY(), possessor.getZ(), java.util.Set.of(), target.getYRot(), target.getXRot(), true);
            }
            tpDest = target;
        }
        if (tpDest != null) {
            possessor.teleportTo(tpDest.getX(), tpDest.getY(), tpDest.getZ());
        }
    }

    public static void dropArmorIfBanned(LivingEntity converted) {
        if (converted.getType().builtInRegistryHolder().is(RequiemCoreTags.Entity.ARMOR_BANNED)) {
            for (EquipmentSlot slot : EquipmentSlot.values()) {
                if (slot.isArmor()) {
                    ItemStack equippedStack = converted.getItemBySlot(slot);
                    if (converted.level() instanceof ServerLevel serverLevel) {
                        converted.spawnAtLocation(serverLevel, equippedStack.copy());
                    }
                    equippedStack.setCount(0);
                }
            }
        }
    }
}
