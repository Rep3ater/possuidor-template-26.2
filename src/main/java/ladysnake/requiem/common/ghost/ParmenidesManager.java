package ladysnake.requiem.common.ghost;

import ladysnake.requiem.common.block.RequiemBlocks;
import ladysnake.requiem.common.entity.RequiemEntities;
import ladysnake.requiem.common.entity.PlayerShellEntity;
import ladysnake.requiem.common.network.RequiemNetworking;
import ladysnake.requiem.common.particle.RequiemParticleTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Server authority for the Parmenides body, its projections, and temporary spectral terrain. */
public final class ParmenidesManager {
    private static final String PROJECTION_PREFIX = "requiem_parmenides_projection:";
    private static final String SPECTRAL_ACTIVE_TAG = "requiem_parmenides_spectral_active";
    private static final Set<UUID> CHECKED_INACTIVE_PLAYERS = ConcurrentHashMap.newKeySet();
    private static final Map<UUID, String> LAST_DIMENSIONS = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> DIMENSION_STABLE_TICKS = new ConcurrentHashMap<>();
    private static final Map<UUID, Map<ResourceKey<Level>, LinkedHashSet<BlockPos>>> SPECTRAL_TRAILS = new HashMap<>();
    private static final int MAX_TRAIL_BLOCKS_PER_DIMENSION = 128;
    private static final double FADE_DISTANCE_SQUARED = 25.0D;

    private ParmenidesManager() { }

    public static boolean isProjection(PlayerShellEntity shell) {
        return shell.getTags().stream().anyMatch(tag -> tag.startsWith(PROJECTION_PREFIX));
    }

    public static java.util.UUID projectionOwner(PlayerShellEntity shell) {
        return shell.getTags().stream().filter(tag -> tag.startsWith(PROJECTION_PREFIX))
            .map(tag -> tag.substring(PROJECTION_PREFIX.length()))
            .map(value -> {
                try { return java.util.UUID.fromString(value); }
                catch (IllegalArgumentException ignored) { return null; }
            }).filter(java.util.Objects::nonNull).findFirst().orElse(null);
    }

    public static boolean canUseSpectralBlocks(PlayerShellEntity shell) {
        return isProjection(shell) && shell.getTags().contains(SPECTRAL_ACTIVE_TAG);
    }

    public static void switchDimension(ServerPlayer player) {
        if (!ParmenidesRole.isParmenides(player)) return;
        ServerLevel current = (ServerLevel) player.level();
        ServerLevel target = player.level().dimension() == Level.NETHER
            ? current.getServer().getLevel(Level.OVERWORLD)
            : current.getServer().getLevel(Level.NETHER);
        if (target == null || target == current) return;
        player.teleportTo(target, player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot());
    }

    /** Keeps projection entities appearance-synced and only materializes them in loaded chunks. */
    public static void tick(ServerPlayer player) {
        String marker = PROJECTION_PREFIX + player.getUUID();
        if (player.isRemoved()) return;
        if (!ParmenidesRole.isParmenides(player)) {
            removeSpectralTrail(player);
            if (CHECKED_INACTIVE_PLAYERS.add(player.getUUID())) discardProjections(player, marker);
            return;
        }
        CHECKED_INACTIVE_PLAYERS.remove(player.getUUID());

        boolean spectralActive = player.isShiftKeyDown();
        // Promptly synchronize the role to client collision contexts as Shift is pressed.
        if (spectralActive && player.tickCount % 5 == 0) {
            PacketDistributor.sendToPlayersTrackingEntityAndSelf(player,
                new RequiemNetworking.ParmenidesStatePayload(player.getId(), true));
        }
        tickPlayerSpectralTrail(player, spectralActive);

        BlockPos position = player.blockPosition();
        ServerLevel activeLevel = (ServerLevel) player.level();
        String dimensionKey = activeLevel.dimension().location().toString();
        String previousDimension = LAST_DIMENSIONS.put(player.getUUID(), dimensionKey);
        if (previousDimension == null) {
            DIMENSION_STABLE_TICKS.put(player.getUUID(), 10);
        } else if (!previousDimension.equals(dimensionKey)) {
            DIMENSION_STABLE_TICKS.put(player.getUUID(), 0);
        }
        int stableTicks = DIMENSION_STABLE_TICKS.merge(player.getUUID(), 1, Integer::sum);
        if (stableTicks < 10) {
            for (ServerLevel level : activeLevel.getServer().getAllLevels()) {
                level.getEntitiesOfClass(ParmenidesProjectionEntity.class, new AABB(position).inflate(4.0),
                    shell -> shell.getTags().contains(marker)).forEach(ParmenidesManager::discardProjection);
            }
            return;
        }

        for (ServerLevel level : activeLevel.getServer().getAllLevels()) {
            List<ParmenidesProjectionEntity> nearby = level.getEntitiesOfClass(
                ParmenidesProjectionEntity.class, new AABB(position).inflate(2.0),
                shell -> shell.getTags().contains(marker)
            );
            if (level == player.level()) {
                nearby.forEach(ParmenidesManager::discardProjection);
                continue;
            }

            ParmenidesProjectionEntity projection = nearby.stream().findFirst().orElse(null);
            if (projection == null) {
                if (!level.hasChunkAt(position)) continue;
                projection = new ParmenidesProjectionEntity(RequiemEntities.PARMENIDES_PROJECTION, level);
                projection.setDisplayProfile(player.getGameProfile());
                projection.addTag(marker);
                projection.setNoGravity(true);
                projection.noPhysics = true;
                projection.setInvulnerable(false);
                projection.getAbilities().invulnerable = false;
                level.addFreshEntity(projection);
                syncProjectionTracking(projection, true);
            }

            projection.setPos(player.getX(), player.getY(), player.getZ());
            projection.setYRot(player.getYRot());
            projection.setXRot(player.getXRot());
            projection.setYHeadRot(player.getYHeadRot());
            projection.setYBodyRot(player.yBodyRot);
            projection.setPose(player.getPose());
            projection.setDeltaMovement(player.getDeltaMovement());
            projection.setHealth(Math.min(player.getHealth(), projection.getMaxHealth()));
            projection.setNoGravity(true);
            if (spectralActive) {
                projection.addTag(SPECTRAL_ACTIVE_TAG);
                projection.noPhysics = false;
                tickProjectionSpectralTrail(player, projection, level);
            } else {
                projection.removeTag(SPECTRAL_ACTIVE_TAG);
                projection.noPhysics = true;
            }
            for (EquipmentSlot slot : EquipmentSlot.values()) {
                projection.setItemSlot(slot, player.getItemBySlot(slot).copy());
            }
            if (player.tickCount % 20 == 0) syncProjectionTracking(projection, true);
        }
    }

    private static void tickPlayerSpectralTrail(ServerPlayer player, boolean active) {
        if (active) {
            ServerLevel level = (ServerLevel) player.level();
            boolean contactOrJump = player.horizontalCollision || player.verticalCollision
                || (!player.onGround() && player.getDeltaMovement().y > 0.04D);
            updateBuilderBlocks(player, level, player.getUUID(), contactOrJump);
            if (player.tickCount % 4 == 0) {
                level.sendParticles(RequiemParticleTypes.GHOST,
                    player.getX(), player.getY() + 0.15D, player.getZ(),
                    3, 0.32D, 0.22D, 0.32D, 0.008D);
            }
        }
        fadeDistantBlocks(player, active);
    }

    private static void tickProjectionSpectralTrail(ServerPlayer owner, ParmenidesProjectionEntity projection, ServerLevel level) {
        boolean contactOrJump = owner.horizontalCollision || owner.verticalCollision
            || (!owner.onGround() && owner.getDeltaMovement().y > 0.04D);
        updateBuilderBlocks(projection, level, owner.getUUID(), contactOrJump);
    }

    private static void updateBuilderBlocks(Entity builder, ServerLevel level, UUID owner, boolean contactOrJump) {
        if (!hasSolidSupport(level, builder)) {
            BlockPos underfoot = new BlockPos(
                net.minecraft.util.Mth.floor(builder.getX()),
                net.minecraft.util.Mth.floor(builder.getY()) - 1,
                net.minecraft.util.Mth.floor(builder.getZ())
            );
            addSpectralBlock(level, owner, underfoot, false);
        }
        if (contactOrJump) {
            // A half-height collision volume turns repeated jumps and wall contacts into climbable steps.
            BlockPos step = BlockPos.containing(builder.getX(), builder.getY(), builder.getZ());
            addSpectralBlock(level, owner, step, true);
        }
    }

    private static boolean hasSolidSupport(ServerLevel level, Entity entity) {
        BlockPos atFeet = BlockPos.containing(entity.getX(), entity.getY() - 1.0E-3D, entity.getZ());
        BlockPos below = atFeet.below();
        CollisionContext context = CollisionContext.of(entity);
        return !level.getBlockState(atFeet).getCollisionShape(level, atFeet, context).isEmpty()
            || !level.getBlockState(below).getCollisionShape(level, below, context).isEmpty();
    }

    private static void addSpectralBlock(ServerLevel level, UUID owner, BlockPos pos, boolean step) {
        if (pos.getY() < level.getMinBuildHeight() || pos.getY() >= level.getMaxBuildHeight()
                || !level.hasChunkAt(pos)) return;

        Map<ResourceKey<Level>, LinkedHashSet<BlockPos>> dimensions =
            SPECTRAL_TRAILS.computeIfAbsent(owner, ignored -> new HashMap<>());
        LinkedHashSet<BlockPos> trail = dimensions.computeIfAbsent(level.dimension(), ignored -> new LinkedHashSet<>());
        BlockState existing = level.getBlockState(pos);
        if (existing.is(RequiemBlocks.PARMENIDES_SPECTRAL)) {
            trail.add(pos.immutable());
            if (existing.getValue(ParmenidesSpectralBlock.FADE) != 0) {
                level.setBlock(pos, existing.setValue(ParmenidesSpectralBlock.FADE, 0), 3);
            }
            return;
        }
        if (trail.size() >= MAX_TRAIL_BLOCKS_PER_DIMENSION) return;
        // Never replace terrain, snow layers, plants, fluids, or another mod's blocks.
        if (!existing.isAir() || !level.getFluidState(pos).isEmpty()) return;

        BlockState spectral = RequiemBlocks.PARMENIDES_SPECTRAL.defaultBlockState()
            .setValue(ParmenidesSpectralBlock.STEP, step);
        if (level.setBlock(pos, spectral, 3)) {
            trail.add(pos.immutable());
            level.sendParticles(RequiemParticleTypes.GHOST,
                pos.getX() + 0.5D, pos.getY() + 0.52D, pos.getZ() + 0.5D,
                8, 0.36D, 0.36D, 0.36D, 0.014D);
        }
    }

    private static void fadeDistantBlocks(ServerPlayer player, boolean active) {
        Map<ResourceKey<Level>, LinkedHashSet<BlockPos>> dimensions = SPECTRAL_TRAILS.get(player.getUUID());
        if (dimensions == null || dimensions.isEmpty()) return;
        double px = player.getX(), py = player.getY(), pz = player.getZ();

        for (Iterator<Map.Entry<ResourceKey<Level>, LinkedHashSet<BlockPos>>> dimensionsIt = dimensions.entrySet().iterator(); dimensionsIt.hasNext();) {
            Map.Entry<ResourceKey<Level>, LinkedHashSet<BlockPos>> dimensionEntry = dimensionsIt.next();
            ServerLevel level = player.getServer().getLevel(dimensionEntry.getKey());
            if (level == null) continue;
            LinkedHashSet<BlockPos> trail = dimensionEntry.getValue();
            for (Iterator<BlockPos> it = trail.iterator(); it.hasNext();) {
                BlockPos pos = it.next();
                if (!level.hasChunkAt(pos)) continue;
                BlockState state = level.getBlockState(pos);
                if (!state.is(RequiemBlocks.PARMENIDES_SPECTRAL)) {
                    it.remove();
                    continue;
                }

                double dx = pos.getX() + 0.5D - px;
                double dy = pos.getY() + 0.5D - py;
                double dz = pos.getZ() + 0.5D - pz;
                boolean stillNear = active && dx * dx + dy * dy + dz * dz <= FADE_DISTANCE_SQUARED;
                int fade = state.getValue(ParmenidesSpectralBlock.FADE);
                if (stillNear) {
                    if (fade != 0) level.setBlock(pos, state.setValue(ParmenidesSpectralBlock.FADE, 0), 3);
                } else if ((level.getGameTime() & 3L) == 0L) {
                    if (fade >= 4) {
                        level.removeBlock(pos, false);
                        it.remove();
                    } else {
                        level.setBlock(pos, state.setValue(ParmenidesSpectralBlock.FADE, fade + 1), 3);
                        if (level.getRandom().nextFloat() < 0.45F) {
                            level.sendParticles(RequiemParticleTypes.GHOST,
                                pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D,
                                1, 0.22D, 0.22D, 0.22D, 0.008D);
                        }
                    }
                }
            }
            if (trail.isEmpty()) dimensionsIt.remove();
        }
        if (dimensions.isEmpty()) SPECTRAL_TRAILS.remove(player.getUUID());
    }

    private static void removeSpectralTrail(ServerPlayer player) {
        Map<ResourceKey<Level>, LinkedHashSet<BlockPos>> dimensions = SPECTRAL_TRAILS.remove(player.getUUID());
        if (dimensions == null || player.getServer() == null) return;
        for (Map.Entry<ResourceKey<Level>, LinkedHashSet<BlockPos>> entry : dimensions.entrySet()) {
            ServerLevel level = player.getServer().getLevel(entry.getKey());
            if (level == null) continue;
            for (BlockPos pos : entry.getValue()) {
                if (level.hasChunkAt(pos) && level.getBlockState(pos).is(RequiemBlocks.PARMENIDES_SPECTRAL)) {
                    level.removeBlock(pos, false);
                }
            }
        }
    }

    public static void clearProjections(ServerPlayer player) {
        removeSpectralTrail(player);
        discardProjections(player, PROJECTION_PREFIX + player.getUUID());
        CHECKED_INACTIVE_PLAYERS.add(player.getUUID());
        LAST_DIMENSIONS.remove(player.getUUID());
        DIMENSION_STABLE_TICKS.remove(player.getUUID());
    }

    private static void discardProjections(ServerPlayer player, String marker) {
        AABB bounds = new AABB(player.blockPosition()).inflate(2.0);
        for (ServerLevel level : player.getServer().getAllLevels()) {
            level.getEntitiesOfClass(ParmenidesProjectionEntity.class, bounds,
                shell -> shell.getTags().contains(marker)).forEach(ParmenidesManager::discardProjection);
        }
    }

    private static void discardProjection(ParmenidesProjectionEntity projection) {
        syncProjectionTracking(projection, false);
        projection.discard();
    }

    private static void syncProjectionTracking(ParmenidesProjectionEntity projection, boolean active) {
        PacketDistributor.sendToPlayersTrackingEntity(projection,
            new RequiemNetworking.OutOfBodyBodyPayload(projection.getId(), active));
    }
}
