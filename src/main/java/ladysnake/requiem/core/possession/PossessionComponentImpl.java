/*
 * Requiem
 * Copyright (C) 2017-2023 Ladysnake
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU License
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
package ladysnake.requiem.core.possession;

import com.demonwav.mcdev.annotations.CheckEnv;
import com.demonwav.mcdev.annotations.Env;
import ladysnake.requiem.api.v1.entity.MovementAlterer;
import ladysnake.requiem.api.v1.entity.MovementConfig;
import ladysnake.requiem.api.v1.entity.MovementRegistry;
import ladysnake.requiem.api.v1.event.requiem.PossessionEvents;
import ladysnake.requiem.api.v1.event.requiem.PossessionStartCallback;
import ladysnake.requiem.api.v1.event.requiem.PossessionStateChangeCallback;
import ladysnake.requiem.api.v1.possession.Possessable;
import ladysnake.requiem.api.v1.possession.PossessedData;
import ladysnake.requiem.api.v1.possession.PossessionComponent;
import ladysnake.requiem.api.v1.remnant.AttritionFocus;
import ladysnake.requiem.api.v1.remnant.RemnantComponent;
import ladysnake.requiem.api.v1.entity.MovementConfig;
import ladysnake.requiem.api.v1.entity.MovementRegistry;
import ladysnake.requiem.api.v1.remnant.SoulbindingRegistry;
import ladysnake.requiem.core.movement.PlayerMovementAlterer;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import ladysnake.requiem.core.RequiemCore;
import io.github.ladysnake.pal.Pal;
import io.github.ladysnake.pal.VanillaAbilities;
import ladysnake.requiem.core.RequiemCoreNetworking;
import ladysnake.requiem.core.mixin.access.LivingEntityAccessor;
import ladysnake.requiem.core.movement.SerializableMovementConfig;
import ladysnake.requiem.core.remnant.MutableRemnantState;
import ladysnake.requiem.core.tag.RequiemCoreTags;
import ladysnake.requiem.core.util.InventoryHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundUpdateAttributesPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;

public final class PossessionComponentImpl implements PossessionComponent {
    private static final List<Holder<Attribute>> SYNCED_ATTRIBUTES = List.of(
        Attributes.MAX_HEALTH,
        Attributes.MOVEMENT_SPEED,
        Attributes.ATTACK_DAMAGE,
        Attributes.ARMOR,
        Attributes.ARMOR_TOUGHNESS,
        Attributes.KNOCKBACK_RESISTANCE,
        Attributes.STEP_HEIGHT,
        Attributes.SCALE,
        Attributes.GRAVITY,
        Attributes.SAFE_FALL_DISTANCE,
        Attributes.FALL_DAMAGE_MULTIPLIER,
        Attributes.JUMP_STRENGTH,
        Attributes.WATER_MOVEMENT_EFFICIENCY
    );

    private final Player player;
    @Nullable
    private Mob possessed;
    private int conversionTimer;

    public PossessionComponentImpl(Player player) {
        this.player = player;
    }

    private boolean isReadyForPossession() {
        return player.level().isClientSide() || (!player.isSpectator() && RemnantComponent.get(this.player).isIncorporeal());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean startPossessing(final Mob host, boolean simulate) {
        // Check that the player can initiate possession
        if (!isReadyForPossession()) {
            return false;
        }

        PossessionStartCallback.Result result = PossessionStartCallback.EVENT.invoker().onPossessionAttempted(host, this.player, simulate);
        if (result != PossessionStartCallback.Result.ALLOW) {
            return result.isSuccess();
        }

        Possessable possessable = (host instanceof Possessable p) ? p : new FallbackPossessable(host);
        // Check that the mob can be possessed
        if (!possessable.canBePossessedBy(player)) {
            return false;
        }
        if (!simulate) {
            startPossessing0(host, possessable);
        }
        return true;
    }

    private void startPossessing0(Mob host, Possessable possessable) {
        possessable.setPossessor(null);
        // Transfer inventory and mount
        if (!player.level().isClientSide()) {
            if (host.getType().builtInRegistryHolder().is(RequiemCoreTags.Entity.INVENTORY_CARRIERS)) {
                PossessedData.KEY.get(host).moveItems(player.getInventory(), false);
            }
            if (host.getType().builtInRegistryHolder().is(RequiemCoreTags.Entity.ITEM_USERS)) {
                InventoryHelper.transferEquipment(host, player);
            }
            for (MobEffectInstance effect : player.getActiveEffects()) {
                if (SoulbindingRegistry.instance().isSoulbound(effect.getEffect().value())) {
                    host.addEffect(new MobEffectInstance(effect));
                }
            }
            for (MobEffectInstance effect : host.getActiveEffects()) {
                player.addEffect(new MobEffectInstance(effect));
            }
            Entity ridden = ((Entity) possessable).getVehicle();
            if (ridden != null) {
                ((Mob) possessable).stopRiding();
                player.startRiding(ridden);
            }
            if (host.getType().builtInRegistryHolder().is(RequiemCoreTags.Entity.EATERS)) {
                CompoundTag hungerData = PossessedData.KEY.get(host).getHungerData();
                player.getFoodData().setFoodLevel((hungerData.contains("foodLevel") ? hungerData.getInt("foodLevel") : 20));
                player.getFoodData().setSaturation((hungerData.contains("foodSaturationLevel") ? hungerData.getFloat("foodSaturationLevel") : 5.0F));
            }

            host.setTarget(null);
        }
        // Actually set the possessed entity
        this.possessed = host;
        possessable.setPossessor(this.player);
        PossessionComponent.KEY.sync(this.player);
        // Update some attributes
        this.player.copyPosition(host);
        this.player.setYRot(host.getYRot());
        this.player.setXRot(host.getXRot());
        this.player.refreshDimensions(); // update size
        MovementAlterer.get(this.player).setConfig(MovementRegistry.get(this.player.level()).getEntityMovementConfig(host.getType()));

        if (!this.player.level().isClientSide()) {
            Pal.revokeAbility(this.player, VanillaAbilities.INVULNERABLE, MutableRemnantState.SOUL_STATE);
            this.player.getAbilities().invulnerable = this.player.isCreative();

            boolean hostFlies = canHostFly(host);
            if (!hostFlies && !this.player.isCreative() && !this.player.isSpectator()) {
                Pal.revokeAbility(this.player, VanillaAbilities.ALLOW_FLYING, MutableRemnantState.SOUL_STATE);
                Pal.revokeAbility(this.player, VanillaAbilities.ALLOW_FLYING, PlayerMovementAlterer.MOVEMENT_ALTERER_ABILITIES);
                this.player.getAbilities().mayfly = false;
                this.player.getAbilities().flying = false;
            } else if (hostFlies) {
                this.player.getAbilities().mayfly = true;
            }

            for (Holder<Attribute> attr : SYNCED_ATTRIBUTES) {
                var playerInst = this.player.getAttribute(attr);
                if (playerInst != null && host.getAttributes().hasAttribute(attr)) {
                    if (attr.equals(Attributes.MOVEMENT_SPEED)) {
                        double hostSpeed = host.getAttributeValue(Attributes.MOVEMENT_SPEED);
                        playerInst.setBaseValue(0.1D * (hostSpeed / 0.25D));
                    } else {
                        playerInst.setBaseValue(host.getAttributeBaseValue(attr));
                    }
                }
            }
            this.player.setHealth(Math.min(host.getHealth(), host.getMaxHealth()));
            if (this.player instanceof ServerPlayer sp) {
                sp.onUpdateAbilities();
                sp.connection.send(new ClientboundUpdateAttributesPacket(this.player.getId(), this.player.getAttributes().getSyncableAttributes()));
            }
        }

        // Make the mob react a bit
        host.playAmbientSound();

        //Set persistent
        host.setPersistenceRequired();

        // Fire event
        PossessionStateChangeCallback.EVENT.invoker().onPossessionStateChange(this.player, host);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void stopPossessing() {
        this.stopPossessing(!this.player.isCreative());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void stopPossessing(boolean transfer) {
        LivingEntity host = this.getHost();
        if (host != null) {
            this.resetState();
            if (host instanceof Possessable p) {
                p.setPossessor(null);
            }

            if (player instanceof ServerPlayer serverPlayer) {
                Entity ridden = player.getVehicle();
                if (ridden != null) {
                    player.stopRiding();
                    host.startRiding(ridden);
                }

                if (transfer) {
                    dropEquipment(host, serverPlayer);
                }

                if (host.getType().builtInRegistryHolder().is(RequiemCoreTags.Entity.EATERS)) {
                    CompoundTag hungerData = PossessedData.KEY.get(host).getHungerData();
                    hungerData.putInt("foodLevel", player.getFoodData().getFoodLevel());
                    hungerData.putFloat("foodSaturationLevel", player.getFoodData().getSaturationLevel());
                }

                // move soulbound effects from the host to the soul
                // careful with ConcurrentModificationException
                for (MobEffectInstance effect : host.getActiveEffects().toArray(new MobEffectInstance[0])) {
                    if (SoulbindingRegistry.instance().isSoulbound(effect.getEffect().value())) {
                        host.removeEffect(effect.getEffect());
                        player.addEffect(new MobEffectInstance(effect));
                    }
                }

                host.setSprinting(false);
            }
        }
    }

    public static void dropEquipment(LivingEntity possessed, ServerPlayer player) {
        if (PossessionEvents.INVENTORY_TRANSFER_CHECK.invoker().shouldTransfer(player, possessed).get()) {
            if (possessed.getType().builtInRegistryHolder().is(RequiemCoreTags.Entity.ITEM_USERS)) {
                InventoryHelper.transferEquipment(player, possessed);
            }
            if (possessed.getType().builtInRegistryHolder().is(RequiemCoreTags.Entity.INVENTORY_CARRIERS)) {
                PossessedData.KEY.get(possessed).moveItems(player.getInventory(), true);
            }
            if (player.level() instanceof ServerLevel serverLevel) {
                ((LivingEntityAccessor) player).requiem$invokeDropEquipment(serverLevel);
            }
        }
        player.removeAllEffects();
        if (player.level().getLevelData().isHardcore()) {
            AttritionFocus.KEY.get(possessed).applyAttrition(player);
        }
        PossessionEvents.DISSOCIATION_CLEANUP.invoker().cleanUpAfterDissociation(player, possessed);
    }

    @Override
    public void writeSyncPacket(RegistryFriendlyByteBuf buf, ServerPlayer recipient) {
        buf.writeInt(this.possessed == null ? -1 : this.possessed.getId());
    }

    @Override
    public void applySyncPacket(RegistryFriendlyByteBuf buf) {
        int possessedId = buf.readInt();
        Entity entity = player.level().getEntity(possessedId);

        if (entity instanceof Mob mob) {
            this.startPossessing(mob);
            updateCamera(this.player, entity);
        } else {
            this.stopPossessing();
            updateCamera(this.player, this.player);
        }
    }

    @CheckEnv(Env.CLIENT)
    private static void updateCamera(Player player, Entity cameraEntity) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.getCameraType().isFirstPerson() && player == mc.player) {
            mc.gameRenderer.checkEntityPostEffect(cameraEntity);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public @CheckForNull Mob getHost() {
        if (!this.isPossessionOngoing()) {
            return null;
        }

        if (this.possessed.isRemoved()) {
            UUID possessedUuid = this.possessed.getUUID();
            RequiemCore.LOGGER.debug("{}: this player's possessed entity has disappeared", this.player);
            this.resetState();
            // Attempt to find an equivalent entity using the UUID
            if (this.player.level() instanceof ServerLevel serverLevel) {
                Entity host = serverLevel.getEntity(possessedUuid);
                if (host instanceof Mob mob && host instanceof Possessable) {
                    this.startPossessing(mob);
                }
            }
        }
        return this.possessed;
    }

    private void resetState() {
        this.possessed = null;
        this.conversionTimer = 0;
        MovementAlterer.get(this.player).setConfig(RemnantComponent.get(this.player).isVagrant() ? SerializableMovementConfig.SOUL : null);
        this.player.refreshDimensions(); // update size
        this.player.setAirSupply(this.player.getMaxAirSupply());
        if (!this.player.level().isClientSide()) {
            boolean incorporeal = RemnantComponent.isIncorporeal(this.player);
            if (incorporeal) {
                Pal.grantAbility(this.player, VanillaAbilities.INVULNERABLE, MutableRemnantState.SOUL_STATE);
                this.player.getAbilities().mayfly = true;
                this.player.getAbilities().flying = true;
            } else {
                Pal.revokeAbility(this.player, VanillaAbilities.INVULNERABLE, MutableRemnantState.SOUL_STATE);
                Pal.revokeAbility(this.player, VanillaAbilities.ALLOW_FLYING, MutableRemnantState.SOUL_STATE);
                Pal.revokeAbility(this.player, VanillaAbilities.ALLOW_FLYING, PlayerMovementAlterer.MOVEMENT_ALTERER_ABILITIES);
                if (!this.player.isCreative() && !this.player.isSpectator()) {
                    this.player.getAbilities().mayfly = false;
                    this.player.getAbilities().flying = false;
                }
            }
            for (Holder<Attribute> attr : SYNCED_ATTRIBUTES) {
                var playerInst = this.player.getAttribute(attr);
                if (playerInst != null) {
                    playerInst.setBaseValue(getDefaultPlayerAttributeValue(attr));
                }
            }
            this.player.setHealth(Math.min(this.player.getHealth(), 20.0F));
            if (this.player instanceof ServerPlayer sp) {
                sp.onUpdateAbilities();
            }
        }
        PossessionComponent.KEY.sync(this.player);
        PossessionStateChangeCallback.EVENT.invoker().onPossessionStateChange(this.player, null);
        RequiemCoreNetworking.sendToAllTrackingIncluding(player, new ClientboundUpdateAttributesPacket(player.getId(), player.getAttributes().getSyncableAttributes()));
    }

    public static double getDefaultPlayerAttributeValue(Holder<Attribute> attr) {
        if (attr.equals(Attributes.MOVEMENT_SPEED)) {
            return 0.1D;
        } else if (attr.equals(Attributes.ATTACK_DAMAGE)) {
            return 1.0D;
        } else if (attr.equals(Attributes.MAX_HEALTH)) {
            return 20.0D;
        } else if (attr.equals(Attributes.STEP_HEIGHT)) {
            return 0.6D;
        } else if (attr.equals(Attributes.SCALE)) {
            return 1.0D;
        } else if (attr.equals(Attributes.GRAVITY)) {
            return 0.08D;
        } else if (attr.equals(Attributes.SAFE_FALL_DISTANCE)) {
            return 3.0D;
        } else if (attr.equals(Attributes.FALL_DAMAGE_MULTIPLIER)) {
            return 1.0D;
        } else if (attr.equals(Attributes.JUMP_STRENGTH)) {
            return 0.42D;
        } else if (attr.equals(Attributes.ARMOR) || attr.equals(Attributes.ARMOR_TOUGHNESS) || attr.equals(Attributes.KNOCKBACK_RESISTANCE) || attr.equals(Attributes.WATER_MOVEMENT_EFFICIENCY)) {
            return 0.0D;
        }
        return attr.value().getDefaultValue();
    }

    public static boolean canHostFly(@Nullable Mob host) {
        if (host == null) {
            return false;
        }
        if (host.isNoGravity()
            || host.getType() == EntityType.GHAST
            || host.getType() == EntityType.PHANTOM
            || host.getType() == EntityType.BAT
            || host.getType() == EntityType.ALLAY
            || host.getType() == EntityType.BEE
            || host.getMoveControl() instanceof FlyingMoveControl) {
            return true;
        }
        MovementConfig config = MovementRegistry.get(host.level()).getEntityMovementConfig(host.getType());
        MovementConfig.MovementMode mode = config.getFlightMode();
        return mode == MovementConfig.MovementMode.ENABLED || mode == MovementConfig.MovementMode.FORCED;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isPossessionOngoing() {
        return this.possessed != null;
    }

    @Override
    public boolean canBeCured(ItemStack cure) {
        Mob possessedEntity = this.getHost();
        return possessedEntity != null
            && cure.is(RequiemCoreTags.Item.UNDEAD_CURES)
            && possessedEntity.hasEffect(MobEffects.WEAKNESS)
            && RemnantComponent.get(this.player).canCurePossessed(possessedEntity);
    }

    @Override
    public void startCuring() {
        if (!this.player.level().isClientSide()) {
            RandomSource rand = this.player.getRandom();
            this.conversionTimer = rand.nextInt(1201) + 2400;  // a bit shorter than villager
            this.player.removeEffect(MobEffects.WEAKNESS);
            this.player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, conversionTimer, 0));
            this.player.level().playSound(null, this.player.getX() + 0.5D, this.player.getY() + 0.5D, this.player.getZ() + 0.5D, SoundEvents.ZOMBIE_VILLAGER_CURE, this.player.getSoundSource(), 1.0F + rand.nextFloat(), rand.nextFloat() * 0.7F + 0.3F);
        }
    }

    @Override
    public boolean isCuring() {
        return this.conversionTimer > 0;
    }

    @Override
    public void serverTick() {
        if (this.player.isSpectator()) {
            this.stopPossessing();
        }
        if (this.isCuring()) {
            if (!this.isPossessionOngoing()) this.conversionTimer = 0;
            else this.conversionTimer--;

            if (this.conversionTimer == 0) {
                this.finishCuring();
            }
        }
    }

    private void finishCuring() {
        Mob possessedEntity = this.getHost();

        if (possessedEntity != null) {
            RemnantComponent.get(this.player).curePossessed(possessedEntity);
        }
    }

    @Override
    public void writeToNbt(CompoundTag tag) {
        tag.putInt("conversionTimer", this.conversionTimer);
    }

    @Override
    public void readFromNbt(CompoundTag compound) {
        this.conversionTimer = (compound.contains("conversionTimer") ? compound.getInt("conversionTimer") : 0);
    }

    public static double reachSq(BlockPos pos, Mob host) {
        double width = host.getBbWidth();
        double dx = (host.getX() + width / 2) - (pos.getX() + 0.5D);
        double dy = (host.getY() + host.getEyeHeight(host.getPose())) - (pos.getY() + 0.5D);
        double dz = (host.getZ() + width / 2) - (pos.getZ() + 0.5D);
        return dx * dx + dy * dy + dz * dz;
    }

    private static final class FallbackPossessable implements Possessable {
        private final Mob mob;
        private Player possessor;

        FallbackPossessable(Mob mob) {
            this.mob = mob;
        }

        @Override
        public Player getPossessor() {
            return this.possessor;
        }

        @Override
        public boolean isBeingPossessed() {
            return this.possessor != null;
        }

        @Override
        public boolean canBePossessedBy(Player player) {
            return this.possessor == null && this.mob.isAlive();
        }

        @Override
        public void setPossessor(@Nullable Player possessor) {
            this.possessor = possessor;
        }
    }
}
