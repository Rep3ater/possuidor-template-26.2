package ladysnake.requiem.common.construct.entity;

import ladysnake.requiem.api.v1.possession.Possessable;
import ladysnake.requiem.common.block.RequiemBlocks;
import ladysnake.requiem.common.construct.ActuatorType;
import ladysnake.requiem.common.construct.ConstructPart;
import ladysnake.requiem.common.construct.ConstructVoxel;
import ladysnake.requiem.common.construct.ConstructVoxelModel;
import ladysnake.requiem.common.construct.ConstructWorkshopManager;
import ladysnake.requiem.common.entity.RequiemTrackedDataHandlers;
import ladysnake.requiem.core.entity.SoulHolderComponent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RedstoneLampBlock;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.*;

public class RoboticConstructEntity extends PathfinderMob implements Possessable {
    private static final EntityDataAccessor<CompoundTag> VOXEL_DATA = SynchedEntityData.defineId(
        RoboticConstructEntity.class,
        RequiemTrackedDataHandlers.COMPOUND_TAG
    );
    private static final EntityDataAccessor<Integer> PUNCH_TICKS = SynchedEntityData.defineId(
        RoboticConstructEntity.class,
        EntityDataSerializers.INT
    );

    private ConstructVoxelModel voxelModel = new ConstructVoxelModel();
    private int punchAnimationTicks = 0;
    private int lampResetTimer = 0;

    public RoboticConstructEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        // Constructs start completely soulless with 0 souls
        SoulHolderComponent.get(this).removeSoul();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 100.0)
            .add(Attributes.MOVEMENT_SPEED, 0.25)
            .add(Attributes.ATTACK_DAMAGE, 6.0)
            .add(Attributes.ARMOR, 10.0)
            .add(Attributes.KNOCKBACK_RESISTANCE, 0.5)
            .add(Attributes.STEP_HEIGHT, 1.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(VOXEL_DATA, new CompoundTag());
        builder.define(PUNCH_TICKS, 0);
    }

    public int getPunchAnimationTicks() {
        return this.punchAnimationTicks;
    }

    @Override
    public boolean canCollideWith(Entity other) {
        return other != this.getPossessor() && !this.isPassengerOfSameVehicle(other);
    }

    @Override
    public boolean isPickable() {
        return this.isAlive();
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    public ConstructVoxelModel getVoxelModel() {
        if (this.level().isClientSide()) {
            CompoundTag tag = this.entityData.get(VOXEL_DATA);
            if (tag != null && !tag.isEmpty() && (this.voxelModel.isEmpty() || tag.size() != this.voxelModel.getVoxelCount())) {
                this.voxelModel = ConstructVoxelModel.fromNbt(tag);
            }
        }
        return this.voxelModel;
    }

    public void setVoxelModel(ConstructVoxelModel model) {
        this.voxelModel = model;
        if (!this.level().isClientSide()) {
            CompoundTag tag = model.toNbt();
            this.entityData.set(VOXEL_DATA, tag);
            updatePhysicalAttributes();
            this.refreshDimensions();
        }
    }

    public void updatePhysicalAttributes() {
        if (this.voxelModel == null || this.voxelModel.isEmpty()) return;

        double speed = this.voxelModel.getCalculatedMovementSpeed();
        double armor = this.voxelModel.getCalculatedArmor();
        double damage = this.voxelModel.getCalculatedAttackDamage();
        
        // Each heart in Minecraft represents 1 block on the construct (1 heart = 2.0 HP)
        int blockCount = this.voxelModel.getNonDestroyedCount();
        float maxHp = Math.max(2.0f, (float) blockCount * 2.0f);

        if (this.getAttribute(Attributes.MOVEMENT_SPEED) != null) {
            this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(speed);
        }
        if (this.getAttribute(Attributes.ARMOR) != null) {
            this.getAttribute(Attributes.ARMOR).setBaseValue(armor);
        }
        if (this.getAttribute(Attributes.ATTACK_DAMAGE) != null) {
            this.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(damage);
        }
        if (this.getAttribute(Attributes.MAX_HEALTH) != null) {
            this.getAttribute(Attributes.MAX_HEALTH).setBaseValue(maxHp);
            if (this.getHealth() > maxHp) {
                this.setHealth(maxHp);
            }
        }
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (VOXEL_DATA.equals(key) && this.level().isClientSide()) {
            this.voxelModel = ConstructVoxelModel.fromNbt(this.entityData.get(VOXEL_DATA));
            this.refreshDimensions();
        }
    }

    @Override
    public EntityDimensions getDefaultDimensions(Pose pose) {
        if (this.voxelModel != null && !this.voxelModel.isEmpty()) {
            AABB bb = this.voxelModel.calculateBoundingBox();
            float width = (float) Math.max(0.6, Math.max(bb.maxX - bb.minX, bb.maxZ - bb.minZ));
            float height = (float) Math.max(0.8, bb.maxY - bb.minY);
            float eyeHeight = height * 0.85f;
            return EntityDimensions.scalable(width, height).withEyeHeight(eyeHeight);
        }
        return super.getDefaultDimensions(pose);
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (!this.level().isClientSide() && player.isShiftKeyDown() && player.getItemInHand(hand).isEmpty()) {
            if (player instanceof ServerPlayer serverPlayer && !this.isBeingPossessed()) {
                ConstructWorkshopManager.enterWorkshopForDisassembly(
                    serverPlayer,
                    this.blockPosition(),
                    this.level().dimension(),
                    this.voxelModel,
                    this.voxelModel.isMiniature()
                );
                this.discard();
                return InteractionResult.SUCCESS;
            }
        }
        return super.mobInteract(player, hand);
    }

    @Override
    public void aiStep() {
        super.aiStep();

        if (this.punchAnimationTicks > 0) {
            this.punchAnimationTicks--;
            if (!this.level().isClientSide()) {
                this.entityData.set(PUNCH_TICKS, this.punchAnimationTicks);
            }
        } else if (this.level().isClientSide()) {
            this.punchAnimationTicks = this.entityData.get(PUNCH_TICKS);
        }

        if (!this.level().isClientSide() && this.lampResetTimer > 0) {
            this.lampResetTimer--;
            if (this.lampResetTimer == 0) {
                resetLitLamps();
            }
        }

        // Piston hydraulic leap when jumping and possessing
        if (this.isBeingPossessed() && this.jumping && this.onGround()) {
            if (this.voxelModel.hasLegActuator(ActuatorType.HYDRAULIC_JUMP)) {
                double boost = this.voxelModel.isMiniature() ? 0.35 : 0.55;
                this.setDeltaMovement(this.getDeltaMovement().add(0, boost, 0));
                this.level().playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.PISTON_EXTEND, SoundSource.PLAYERS, 1.0f, 1.4f);
            }
        }
    }

    private void resetLitLamps() {
        boolean changed = false;
        for (ConstructVoxel v : this.voxelModel.getVoxels().values()) {
            if (v.getState().is(Blocks.REDSTONE_LAMP) && v.getState().getValue(RedstoneLampBlock.LIT)) {
                this.voxelModel.updateVoxelBlockState(v.getRelPos(), Blocks.REDSTONE_LAMP.defaultBlockState().setValue(RedstoneLampBlock.LIT, false));
                changed = true;
            }
        }
        if (changed) {
            this.entityData.set(VOXEL_DATA, this.voxelModel.toNbt());
        }
    }

    public void triggerPistonPunch() {
        if (this.punchAnimationTicks <= 0) {
            this.punchAnimationTicks = 12;
            if (!this.level().isClientSide()) {
                this.entityData.set(PUNCH_TICKS, 12);
                this.level().playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.PISTON_EXTEND, SoundSource.PLAYERS, 1.0f, 1.2f);
            }
        }
    }

    public void performConstructAction(int actionId) {
        if (this.level().isClientSide()) return;
        ServerLevel serverLevel = (ServerLevel) this.level();

        ActuatorType triggerType = (actionId == 1) ? ActuatorType.KEY_R_TRIGGER : ActuatorType.KEY_V_TRIGGER;
        List<ConstructVoxel> triggers = this.voxelModel.getActuatorVoxels(triggerType);

        if (!triggers.isEmpty()) {
            // Authentic Redstone Simulation from the R / V Key Trigger Blocks!
            simulateRedstoneActivation(serverLevel, triggers);
            return;
        }

        // Fallback default actions when no trigger block was placed
        if (actionId == 1) {
            boolean fired = false;
            List<ConstructVoxel> cannons = this.voxelModel.getActuatorVoxels(ActuatorType.DISPENSER_CANNON);
            if (!cannons.isEmpty()) {
                fireDispenser(serverLevel, cannons.get(0));
                fired = true;
            }

            List<ConstructVoxel> redstone = this.voxelModel.getActuatorVoxels(ActuatorType.REDSTONE_PULSE);
            if (!redstone.isEmpty()) {
                performRedstoneShockwave(serverLevel);
                fired = true;
            }

            if (!fired) {
                triggerPistonPunch();
            }
        } else if (actionId == 2) {
            if (this.onGround() || this.voxelModel.hasLegActuator(ActuatorType.HYDRAULIC_JUMP)) {
                double boost = this.voxelModel.isMiniature() ? 0.45 : 0.75;
                this.setDeltaMovement(this.getDeltaMovement().add(0, boost, 0));
                serverLevel.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.PISTON_EXTEND, SoundSource.PLAYERS, 1.0f, 1.2f);
                serverLevel.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, this.getX(), this.getY(), this.getZ(), 10, 0.3, 0.1, 0.3, 0.05);
            }
        }
    }

    private void simulateRedstoneActivation(ServerLevel serverLevel, List<ConstructVoxel> triggers) {
        Map<BlockPos, Integer> powerMap = new HashMap<>();
        Queue<BlockPos> queue = new ArrayDeque<>();

        for (ConstructVoxel trigger : triggers) {
            powerMap.put(trigger.getRelPos(), 15);
            queue.add(trigger.getRelPos());
        }

        // BFS flood-fill redstone wire and connected components in voxel graph
        while (!queue.isEmpty()) {
            BlockPos currentPos = queue.poll();
            int currentPower = powerMap.getOrDefault(currentPos, 0);
            if (currentPower <= 0) continue;

            for (Direction dir : Direction.values()) {
                BlockPos neighborPos = currentPos.relative(dir);
                ConstructVoxel neighbor = this.voxelModel.getVoxel(neighborPos);
                if (neighbor == null || neighbor.isDestroyed()) continue;

                Block block = neighbor.getState().getBlock();
                int nextPower = currentPower - 1;

                if (block == Blocks.REDSTONE_WIRE) {
                    if (nextPower > powerMap.getOrDefault(neighborPos, 0)) {
                        powerMap.put(neighborPos, nextPower);
                        queue.add(neighborPos);
                    }
                } else if (block == Blocks.REPEATER) {
                    if (15 > powerMap.getOrDefault(neighborPos, 0)) {
                        powerMap.put(neighborPos, 15);
                        queue.add(neighborPos);
                    }
                } else {
                    // Receiver component (piston, lamp, dispenser, tnt, note block)
                    if (currentPower > powerMap.getOrDefault(neighborPos, 0)) {
                        powerMap.put(neighborPos, currentPower);
                    }
                }
            }
        }

        boolean modelChanged = false;

        // Activate all reached receivers
        for (Map.Entry<BlockPos, Integer> entry : powerMap.entrySet()) {
            if (entry.getValue() <= 0) continue;
            ConstructVoxel voxel = this.voxelModel.getVoxel(entry.getKey());
            if (voxel == null || voxel.isDestroyed()) continue;

            Block block = voxel.getState().getBlock();

            if (block == Blocks.REDSTONE_LAMP) {
                this.voxelModel.updateVoxelBlockState(voxel.getRelPos(), Blocks.REDSTONE_LAMP.defaultBlockState().setValue(RedstoneLampBlock.LIT, true));
                modelChanged = true;
                this.lampResetTimer = 15;
                serverLevel.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.REDSTONE_TORCH_BURNOUT, SoundSource.PLAYERS, 0.8f, 1.8f);
            } else if (block == Blocks.PISTON || block == Blocks.STICKY_PISTON || voxel.getActuator() == ActuatorType.PISTON_THRUST) {
                triggerPistonPunch();
                performDirectionalPistonThrust(serverLevel, voxel.getFacing());
            } else if (block instanceof net.minecraft.world.level.block.DispenserBlock || block instanceof net.minecraft.world.level.block.DropperBlock || voxel.getActuator() == ActuatorType.DISPENSER_CANNON) {
                fireDispenser(serverLevel, voxel);
            } else if (block == Blocks.TNT) {
                igniteVoxelTnt(serverLevel, voxel);
            } else if (block == Blocks.NOTE_BLOCK) {
                serverLevel.playSound(null, this.getX(), this.getY() + 1.0, this.getZ(), SoundEvents.NOTE_BLOCK_HARP.value(), SoundSource.PLAYERS, 1.5f, 1.0f);
            }
        }

        if (modelChanged) {
            this.entityData.set(VOXEL_DATA, this.voxelModel.toNbt());
        }
    }

    private void performDirectionalPistonThrust(ServerLevel level, Direction facing) {
        Vec3 dir = new Vec3(facing.getStepX(), facing.getStepY(), facing.getStepZ());
        AABB reachBox = this.getBoundingBox().move(dir.scale(1.0)).inflate(0.5);
        List<Entity> targets = level.getEntities(this, reachBox, e -> e instanceof LivingEntity && e != this && e != this.getPossessor());
        for (Entity target : targets) {
            target.hurt(level.damageSources().mobAttack(this), (float) (this.voxelModel.isMiniature() ? 4.0 : 8.0));
            target.push(dir.x * 1.2, 0.35, dir.z * 1.2);
            if (this.level() instanceof ServerLevel serverLevel) serverLevel.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.ANVIL_LAND, SoundSource.HOSTILE, 0.5f, 1.8f);
        }
    }

    private void fireDispenser(ServerLevel serverLevel, ConstructVoxel dispenserVoxel) {
        Vec3 look = this.getLookAngle();
        Vec3 spawnPos = this.position().add(0, this.getEyeHeight() * 0.8, 0).add(look.scale(this.voxelModel.isMiniature() ? 0.5 : 1.2));

        ItemStack item = dispenserVoxel.popItem();

        if (item.isEmpty()) {
            serverLevel.playSound(null, spawnPos.x, spawnPos.y, spawnPos.z, SoundEvents.DISPENSER_FAIL, SoundSource.PLAYERS, 1.0f, 1.2f);
            return;
        }

        if (item.is(Items.TNT)) {
            net.minecraft.world.entity.item.PrimedTnt tnt = new net.minecraft.world.entity.item.PrimedTnt(net.minecraft.world.entity.EntityType.TNT, serverLevel);
            tnt.setPos(spawnPos.x, spawnPos.y, spawnPos.z);
            tnt.setDeltaMovement(look.scale(1.2).add(0, 0.2, 0));
            tnt.setFuse(45);
            serverLevel.addFreshEntity(tnt);
            serverLevel.playSound(null, spawnPos.x, spawnPos.y, spawnPos.z, SoundEvents.DISPENSER_DISPENSE, SoundSource.PLAYERS, 1.2f, 0.9f);
        } else if (item.is(Items.ARROW) || item.is(Items.SPECTRAL_ARROW) || item.is(Items.TIPPED_ARROW)) {
            net.minecraft.world.entity.projectile.Arrow arrow = new net.minecraft.world.entity.projectile.Arrow(serverLevel, this, item, null);
            arrow.setPos(spawnPos.x, spawnPos.y, spawnPos.z);
            arrow.shoot(look.x, look.y, look.z, 2.0f, 1.0f);
            serverLevel.addFreshEntity(arrow);
            serverLevel.playSound(null, spawnPos.x, spawnPos.y, spawnPos.z, SoundEvents.ARROW_SHOOT, SoundSource.PLAYERS, 1.0f, 1.0f);
        } else if (item.is(Items.FIRE_CHARGE)) {
            net.minecraft.world.entity.projectile.SmallFireball fireball = 
                new net.minecraft.world.entity.projectile.SmallFireball(serverLevel, this, look.scale(0.8));
            fireball.setPos(spawnPos.x, spawnPos.y, spawnPos.z);
            serverLevel.addFreshEntity(fireball);
            serverLevel.playSound(null, spawnPos.x, spawnPos.y, spawnPos.z, SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 1.0f, 1.0f);
        } else if (item.is(Items.SNOWBALL)) {
            net.minecraft.world.entity.projectile.Snowball snowball = 
                new net.minecraft.world.entity.projectile.Snowball(serverLevel, this);
            snowball.setPos(spawnPos.x, spawnPos.y, spawnPos.z);
            snowball.shoot(look.x, look.y, look.z, 1.5f, 1.0f);
            serverLevel.addFreshEntity(snowball);
            serverLevel.playSound(null, spawnPos.x, spawnPos.y, spawnPos.z, SoundEvents.SNOWBALL_THROW, SoundSource.PLAYERS, 1.0f, 1.0f);
        } else {
            // General item dispense / drop
            ItemEntity drop = new ItemEntity(serverLevel, spawnPos.x, spawnPos.y, spawnPos.z, item);
            drop.setDeltaMovement(look.scale(0.6).add(0, 0.1, 0));
            serverLevel.addFreshEntity(drop);
            serverLevel.playSound(null, spawnPos.x, spawnPos.y, spawnPos.z, SoundEvents.DISPENSER_DISPENSE, SoundSource.PLAYERS, 1.0f, 1.0f);
        }
    }

    private void igniteVoxelTnt(ServerLevel serverLevel, ConstructVoxel voxel) {
        Vec3 look = this.getLookAngle();
        Vec3 spawnPos = this.position().add(0, this.getEyeHeight() * 0.8, 0).add(look.scale(1.2));
        net.minecraft.world.entity.item.PrimedTnt tnt = new net.minecraft.world.entity.item.PrimedTnt(net.minecraft.world.entity.EntityType.TNT, serverLevel);
        tnt.setPos(spawnPos.x, spawnPos.y, spawnPos.z);
        tnt.setDeltaMovement(look.scale(1.1).add(0, 0.25, 0));
        tnt.setFuse(40);
        serverLevel.addFreshEntity(tnt);
        serverLevel.playSound(null, spawnPos.x, spawnPos.y, spawnPos.z, SoundEvents.TNT_PRIMED, SoundSource.PLAYERS, 1.0f, 1.0f);
    }

    private void performRedstoneShockwave(ServerLevel serverLevel) {
        double radius = this.voxelModel.isMiniature() ? 2.5 : 5.0;
        List<Entity> nearby = serverLevel.getEntities(this, this.getBoundingBox().inflate(radius), e -> e instanceof LivingEntity && e != this && e != this.getPossessor());
        for (Entity e : nearby) {
            e.hurt(serverLevel.damageSources().mobAttack(this), (float) (this.voxelModel.isMiniature() ? 4.0 : 8.0));
            Vec3 pushDir = e.position().subtract(this.position()).normalize();
            e.push(pushDir.x * 0.8, 0.3, pushDir.z * 0.8);
        }
        serverLevel.sendParticles(ParticleTypes.CRIT, this.getX(), this.getY() + 1.0, this.getZ(), 20, 1.0, 0.5, 1.0, 0.2);
        serverLevel.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.REDSTONE_TORCH_BURNOUT, SoundSource.PLAYERS, 1.0f, 1.5f);
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        triggerPistonPunch();
        boolean hit = super.doHurtTarget(target);
        if (hit && this.voxelModel.hasArmActuator(ActuatorType.PISTON_THRUST)) {
            Vec3 look = this.getLookAngle();
            target.push(look.x * 1.2, 0.35, look.z * 1.2);
            if (this.level() instanceof ServerLevel serverLevel) serverLevel.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.ANVIL_LAND, SoundSource.HOSTILE, 0.5f, 1.8f);
        }
        return hit;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (!(this.level() instanceof ServerLevel level)) return super.hurt(source, amount);
        Vec3 hitDir = source.getSourcePosition() != null 
            ? source.getSourcePosition().subtract(this.position()).normalize()
            : this.getLookAngle().reverse();

        ConstructVoxel damagedVoxel = this.voxelModel.damageClosestVoxel(hitDir, amount);

        if (damagedVoxel != null) {
            BlockPos worldPos = this.blockPosition().offset(damagedVoxel.getRelPos());
            if (damagedVoxel.isDestroyed()) {
                level.levelEvent(2001, worldPos, Block.getId(damagedVoxel.getState()));
                // Drop broken block item and its dispenser inventory
                level.addFreshEntity(new ItemEntity(level, worldPos.getX() + 0.5, worldPos.getY() + 0.5, worldPos.getZ() + 0.5, new ItemStack(damagedVoxel.getState().getBlock())));
                for (ItemStack s : damagedVoxel.getInventory()) {
                    level.addFreshEntity(new ItemEntity(level, worldPos.getX() + 0.5, worldPos.getY() + 0.5, worldPos.getZ() + 0.5, s.copy()));
                }
            }

            List<ConstructVoxel> severed = this.voxelModel.checkConnectivityAndSever();
            for (ConstructVoxel v : severed) {
                BlockPos severedPos = this.blockPosition().offset(v.getRelPos());
                level.levelEvent(2001, severedPos, Block.getId(v.getState()));
                level.addFreshEntity(new ItemEntity(level, severedPos.getX() + 0.5, severedPos.getY() + 0.5, severedPos.getZ() + 0.5, new ItemStack(v.getState().getBlock())));
                for (ItemStack s : v.getInventory()) {
                    level.addFreshEntity(new ItemEntity(level, severedPos.getX() + 0.5, severedPos.getY() + 0.5, severedPos.getZ() + 0.5, s.copy()));
                }
            }

            this.entityData.set(VOXEL_DATA, this.voxelModel.toNbt());
            updatePhysicalAttributes();
            this.refreshDimensions();
        }

        // When only 1 block is left (the core heart) or no blocks left, it completely dies and returns to block state
        if (this.voxelModel.isEmpty() || this.voxelModel.getNonDestroyedCount() <= 1 || this.voxelModel.getTotalHealth() <= 0.0f) {
            this.die(source);
            return true;
        }

        return super.hurt(source, amount);
    }

    @Override
    public void die(DamageSource cause) {
        super.die(cause);
        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.EXPLOSION, this.getX(), this.getY() + 1.0, this.getZ(), 5, 0.5, 0.5, 0.5, 0.1);
            serverLevel.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.IRON_GOLEM_DEATH, SoundSource.HOSTILE, 1.0f, 0.8f);

            // Drop all remaining blocks and dispenser inventory so nothing is ever lost
            if (this.voxelModel != null) {
                for (ConstructVoxel v : this.voxelModel.getVoxels().values()) {
                    if (!v.isDestroyed() && !v.getRelPos().equals(BlockPos.ZERO)) {
                        serverLevel.addFreshEntity(new ItemEntity(serverLevel, this.getX(), this.getY() + 0.5, this.getZ(), new ItemStack(v.getState().getBlock())));
                        for (ItemStack s : v.getInventory()) {
                            serverLevel.addFreshEntity(new ItemEntity(serverLevel, this.getX(), this.getY() + 0.5, this.getZ(), s.copy()));
                        }
                    }
                }
            }

            // Return core heart to block form right at its death position
            BlockPos corePos = this.blockPosition();
            boolean isMini = this.voxelModel != null && this.voxelModel.isMiniature();
            serverLevel.setBlock(corePos, isMini ? RequiemBlocks.SMALL_CONSTRUCT_HEART.defaultBlockState() : RequiemBlocks.CONSTRUCT_HEART.defaultBlockState(), 3);
            serverLevel.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.ANVIL_LAND, SoundSource.BLOCKS, 1.0f, 0.8f);
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag output) {
        super.addAdditionalSaveData(output);
        if (this.voxelModel != null) {
            output.put("ConstructVoxels", this.voxelModel.toNbt());
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag input) {
        super.readAdditionalSaveData(input);
        if (input.contains("ConstructVoxels")) setVoxelModel(ConstructVoxelModel.fromNbt(input.getCompound("ConstructVoxels")));
    }

    @Override
    public boolean canBePossessedBy(Player player) {
        return !this.isBeingPossessed() && this.isAlive();
    }
}
