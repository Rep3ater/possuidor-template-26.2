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
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package ladysnake.requiem.common.entity;

import baritone.api.IBaritone;
import baritone.api.event.events.BlockInteractEvent;
import baritone.api.event.events.PathEvent;
import baritone.api.event.listener.IGameEventListener;
import baritone.api.fakeplayer.FakeServerPlayerEntity;
import com.google.common.collect.ImmutableList;
import io.github.ladysnake.impersonate.Impersonator;
import ladysnake.requiem.common.entity.ai.ShellPathfindingProcess;
import ladysnake.requiem.common.entity.ai.brain.PandemoniumMemoryModules;
import ladysnake.requiem.common.entity.ai.brain.PandemoniumSensorTypes;
import ladysnake.requiem.common.entity.ai.brain.PlayerShellBrain;
import ladysnake.requiem.common.remnant.PlayerSplitter;
import net.minecraft.core.GlobalPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import com.mojang.serialization.Dynamic;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.sensing.Sensor;
import net.minecraft.world.entity.ai.sensing.SensorType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

public class PlayerShellEntity extends FakeServerPlayerEntity {
    public static final List<SensorType<? extends Sensor<? super PlayerShellEntity>>> SENSOR_TYPES = ImmutableList.of(
        SensorType.NEAREST_LIVING_ENTITIES,
        SensorType.NEAREST_PLAYERS,
        PandemoniumSensorTypes.CLOSEST_PLAYER_HOSTILE
    );
    public static final List<MemoryModuleType<?>> MEMORY_MODULE_TYPES = ImmutableList.of(
        MemoryModuleType.NEAREST_LIVING_ENTITIES,
        MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES,
        MemoryModuleType.NEAREST_VISIBLE_PLAYER,
        MemoryModuleType.LOOK_TARGET,
        MemoryModuleType.WALK_TARGET,
        MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE,
        MemoryModuleType.ATTACK_TARGET,
        MemoryModuleType.HOME,
        PandemoniumMemoryModules.GO_HOME_ATTEMPTS
    );

    private final ShellPathfindingProcess pathfindingProcess;

    public PlayerShellEntity(EntityType<? extends Player> type, ServerLevel world) {
        super(type, world);
        IBaritone baritone = this.getBaritone();
        baritone.getPathingControlManager().registerProcess(this.pathfindingProcess = new ShellPathfindingProcess(baritone));
        baritone.getGameEventHandler().registerEventListener(new IGameEventListener() {
            @Override
            public void onTickServer() {
                // NO-OP
            }

            @Override
            public void onBlockInteract(BlockInteractEvent event) {
                // NO-OP
            }

            @Override
            public void onPathEvent(PathEvent event) {
                Brain<?> brain = PlayerShellEntity.this.getBrain();
                if (event == PathEvent.AT_GOAL) {
                    brain.eraseMemory(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE);
                }
            }
        });
    }

    public static AttributeSupplier.Builder createPlayerShellAttributes() {
        return Player.createAttributes();
    }

    public ShellPathfindingProcess getPathfindingProcess() {
        return pathfindingProcess;
    }

    public @Nullable GlobalPos getHome() {
        return this.getBrain().getMemory(MemoryModuleType.HOME).orElse(null);
    }

    public void setHome(@Nullable GlobalPos home) {
        this.getBrain().setMemory(MemoryModuleType.HOME, Optional.ofNullable(home));
    }

    public void storePlayerData(ServerPlayer player, CompoundTag respawnNbt) {
        // Save the complete representation of the player
        PlayerSplitter.performNbtCopy(respawnNbt, this);
        this.setInvulnerable(false);
        this.getAbilities().invulnerable = false;
        this.getAbilities().flying = false;
        this.getAbilities().mayfly = false;
        this.setDisplayProfile(Optional.ofNullable(Impersonator.get(player).getImpersonatedProfile()).orElse(player.getGameProfile()));
        this.setHome(GlobalPos.of(player.level().dimension(), player.blockPosition()));
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide()) {
            if (!this.isNoGravity() && !this.isPassenger()) {
                Vec3 movement = this.getDeltaMovement();
                if (!this.onGround()) {
                    movement = movement.add(0, -0.08, 0);
                }
                this.move(net.minecraft.world.entity.MoverType.SELF, movement);
                this.setDeltaMovement(movement.multiply(0.91, 0.98, 0.91));
            }
        }
    }

    @Override
    public boolean isInvulnerable() {
        return false;
    }

    @Override
    public boolean isInvulnerableTo(ServerLevel world, net.minecraft.world.damagesource.DamageSource source) {
        return false;
    }

    @Override
    public boolean canHarmPlayer(Player other) {
        return true;
    }

    @Override
    public boolean hurtServer(ServerLevel level, net.minecraft.world.damagesource.DamageSource source, float amount) {
        this.invulnerableTime = 0;
        this.getAbilities().invulnerable = false;
        boolean hurt = super.hurtServer(level, source, amount);
        if (!hurt && amount > 0.0F) {
            float health = this.getHealth() - amount;
            this.setHealth(Math.max(0.0F, health));
            if (this.getHealth() <= 0.0F) {
                this.die(source);
            }
            return true;
        }
        return hurt;
    }

    @Override
    public void die(net.minecraft.world.damagesource.DamageSource cause) {
        super.die(cause);
        if (!this.level().isClientSide()) {
            this.getInventory().dropAll();
            this.remove(RemovalReason.KILLED);
        }
    }

    @Override
    public java.util.UUID getOwnerUuid() {
        com.mojang.authlib.GameProfile profile = this.getDisplayProfile();
        return profile != null ? profile.id() : null;
    }

    @Override
    public void setDisplayProfile(com.mojang.authlib.GameProfile profile) {
        super.setDisplayProfile(profile);
        if (this.level() instanceof ServerLevel && profile != null) {
            net.neoforged.neoforge.network.PacketDistributor.sendToPlayersTrackingEntity(this, new ladysnake.requiem.common.network.RequiemNetworking.ShellProfileSyncPayload(this.getId(), profile));
        }
    }

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        com.mojang.authlib.GameProfile profile = this.getDisplayProfile();
        if (profile != null) {
            ladysnake.requiem.common.network.RequiemNetworking.sendShellProfileSyncMessage(player, this.getId(), profile);
        }
    }

    @Override
    protected Brain<?> makeBrain(Dynamic<?> dynamic) {
        return PlayerShellBrain.create(Brain.provider(MEMORY_MODULE_TYPES, SENSOR_TYPES).makeBrain(dynamic));
    }

    @Override
    public Brain<PlayerShellEntity> getBrain() {
        @SuppressWarnings("unchecked") Brain<PlayerShellEntity> b = (Brain<PlayerShellEntity>) super.getBrain();
        return b;
    }

    @Override
    protected void serverAiStep() {
        super.serverAiStep();
        if (this.shouldTickBrain() && this.level() instanceof ServerLevel serverLevel) {
            this.getBrain().tick(serverLevel, this);
            PlayerShellBrain.refreshActivities(this);
        }
    }

    private boolean shouldTickBrain() {
        return false;
    }

    @Override
    public net.minecraft.network.chat.Component getName() {
        com.mojang.authlib.GameProfile profile = this.getDisplayProfile();
        if (profile != null && profile.name() != null && !profile.name().isEmpty() && !profile.name().startsWith("[") && !profile.name().equals("Mortal Shell")) {
            return net.minecraft.network.chat.Component.literal(profile.name() + "'s Shell");
        }
        return net.minecraft.network.chat.Component.translatable("entity.requiem.player_shell");
    }

    @Override
    public net.minecraft.network.chat.Component getDisplayName() {
        return this.getName();
    }

    @Override
    public boolean canAttack(LivingEntity target) {
        if (target instanceof Player && target.getUUID().equals(this.getOwnerUuid())) {
            return false;
        }
        return super.canAttack(target);
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand, Vec3 vec) {
        if (ladysnake.requiem.api.v1.remnant.RemnantComponent.isVagrant(player)) {
            if (!this.level().isClientSide()) {
                ladysnake.requiem.api.v1.remnant.RemnantComponent.get(player).merge(this);
            }
            return InteractionResult.SUCCESS;
        }

        ItemStack stack = player.getItemInHand(hand);
        if (stack.getItem() != Items.NAME_TAG) {
            if (!this.level().isClientSide() && !player.isSpectator()) {
                EquipmentSlot slot = this.getEquipmentSlotForItem(stack);
                if (stack.isEmpty()) {
                    EquipmentSlot clickedSlot = this.getClickedSlot(vec);
                    if (this.hasItemInSlot(clickedSlot)) {
                        this.swapItem(player, clickedSlot, stack, hand);
                    } else {
                        return InteractionResult.PASS;
                    }
                } else {
                    this.swapItem(player, slot, stack, hand);
                }
                return InteractionResult.SUCCESS;
            } else {
                return stack.isEmpty() && !this.hasItemInSlot(this.getClickedSlot(vec))
                    ? InteractionResult.PASS
                    : InteractionResult.SUCCESS;
            }
        } else {
            return InteractionResult.PASS;
        }
    }

    protected EquipmentSlot getClickedSlot(Vec3 rayTrace) {
        EquipmentSlot slot = EquipmentSlot.MAINHAND;
        boolean flag = this.isBaby();
        double d0 = (rayTrace.y) * (flag ? 2.0D : 1.0D);

        if (d0 >= 0.1D && d0 < 0.1D + (flag ? 0.8D : 0.45D) && this.hasItemInSlot(EquipmentSlot.FEET)) {
            slot = EquipmentSlot.FEET;
        } else if (d0 >= 0.9D + (flag ? 0.3D : 0.0D) && d0 < 0.9D + (flag ? 1.0D : 0.7D)
            && this.hasItemInSlot(EquipmentSlot.CHEST)) {
            slot = EquipmentSlot.CHEST;
        } else if (d0 >= 0.4D && d0 < 0.4D + (flag ? 1.0D : 0.8D) && this.hasItemInSlot(EquipmentSlot.LEGS)) {
            slot = EquipmentSlot.LEGS;
        } else if (d0 >= 1.6D && this.hasItemInSlot(EquipmentSlot.HEAD)) {
            slot = EquipmentSlot.HEAD;
        }

        return slot;
    }

    protected void swapItem(Player player, EquipmentSlot targetedSlot, ItemStack playerItemStack,
                            InteractionHand hand) {
        ItemStack equippedStack = this.getItemBySlot(targetedSlot);
        if (player.getAbilities().instabuild && equippedStack.isEmpty() && !playerItemStack.isEmpty()) {
            ItemStack copy = playerItemStack.copy();
            copy.setCount(1);
            this.setItemSlot(targetedSlot, copy);
        } else if (!playerItemStack.isEmpty() && playerItemStack.getCount() > 1) {
            if (equippedStack.isEmpty()) {
                ItemStack copy = playerItemStack.copy();
                copy.setCount(1);
                this.setItemSlot(targetedSlot, copy);
                playerItemStack.shrink(1);
            }
        } else {
            this.setItemSlot(targetedSlot, playerItemStack);
            player.setItemInHand(hand, equippedStack);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag input) {
        super.readAdditionalSaveData(input);
        ExtraCodecs.AUTHLIB_GAME_PROFILE.parse(NbtOps.INSTANCE, input.get("PlayerProfile")).result().ifPresent(this::setDisplayProfile);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag output) {
        super.addAdditionalSaveData(output);
        if (this.getDisplayProfile() != null) ExtraCodecs.AUTHLIB_GAME_PROFILE.encodeStart(NbtOps.INSTANCE, this.getDisplayProfile()).result().ifPresent(tag -> output.put("PlayerProfile", tag));
    }

    public void playSound(SoundEvent soundEvent) {
        this.playSound(soundEvent, this.getSoundVolume(), this.getVoicePitch());
    }
}
