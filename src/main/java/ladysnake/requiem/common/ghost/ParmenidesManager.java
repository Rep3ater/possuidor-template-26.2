package ladysnake.requiem.common.ghost;

import ladysnake.requiem.common.entity.RequiemEntities;
import ladysnake.requiem.common.entity.PlayerShellEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.network.PacketDistributor;
import ladysnake.requiem.common.network.RequiemNetworking;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Server authority for the Parmenides body and its dimension-local projections. */
public final class ParmenidesManager {
    private static final String PROJECTION_PREFIX = "requiem_parmenides_projection:";
    private static final Set<UUID> CHECKED_INACTIVE_PLAYERS = ConcurrentHashMap.newKeySet();

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
            if (CHECKED_INACTIVE_PLAYERS.add(player.getUUID())) discardProjections(player, marker);
            return;
        }
        CHECKED_INACTIVE_PLAYERS.remove(player.getUUID());
        BlockPos position = player.blockPosition();
        ServerLevel activeLevel = (ServerLevel) player.level();

        for (ServerLevel level : activeLevel.getServer().getAllLevels()) {
            List<ParmenidesProjectionEntity> nearby = level.getEntitiesOfClass(
                ParmenidesProjectionEntity.class,
                new AABB(position).inflate(2.0),
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
            projection.noPhysics = true;
            for (EquipmentSlot slot : EquipmentSlot.values()) {
                projection.setItemSlot(slot, player.getItemBySlot(slot).copy());
            }
            if (player.tickCount % 20 == 0) syncProjectionTracking(projection, true);
        }
    }

    public static void clearProjections(ServerPlayer player) {
        discardProjections(player, PROJECTION_PREFIX + player.getUUID());
        CHECKED_INACTIVE_PLAYERS.add(player.getUUID());
    }

    private static void discardProjections(ServerPlayer player, String marker) {
        // Projections are repositioned to the owner's block position every server tick.
        // Restrict cleanup to that area instead of scanning every loaded entity section.
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
