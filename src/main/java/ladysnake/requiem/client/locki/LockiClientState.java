package ladysnake.requiem.client.locki;

import ladysnake.requiem.Requiem;
import ladysnake.requiem.client.RequiemClient;
import ladysnake.requiem.client.gui.LockiPlantDialogueScreen;
import ladysnake.requiem.common.locki.LockiRootNetwork;
import ladysnake.requiem.common.entity.effect.RequiemStatusEffects;
import ladysnake.requiem.common.network.RequiemNetworking;
import ladysnake.requiem.common.particle.WispTrailParticleEffect;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Client-side camera, shader and visual state mirrored from the server's Locki controller. */
public final class LockiClientState {
    private static String mode = "none";
    private static int cameraEntityId = -1;
    private static int cameraMode = 0;
    private static int ownRotStage = 0;
    private static int lastPlayerEntityId = -1;
    private static String previousMode = "none";
    private static CameraType savedCameraType;
    private static boolean specialCameraActive;
    private static boolean setSwimmingPose;
    private static long lastNetworkBuild = Long.MIN_VALUE;
    private static List<LockiRootNetwork.PlantNode> cachedPlants = List.of();
    private static List<LockiRootNetwork.Segment> cachedSegments = List.of();
    private static final Map<Integer, Integer> ROT_STAGES = new HashMap<>();
    private static final Map<Integer, String> ENTITY_MODES = new HashMap<>();

    private LockiClientState() {}

    public static void accept(RequiemNetworking.LockiStatePayload payload) {
        ROT_STAGES.put(payload.playerEntityId(), payload.rotStage());
        ENTITY_MODES.put(payload.playerEntityId(), payload.mode());
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.player.getId() != payload.playerEntityId()) return;
        lastPlayerEntityId = payload.playerEntityId();
        previousMode = mode;
        mode = payload.mode();
        cameraEntityId = payload.cameraEntityId();
        cameraMode = payload.cameraMode();
        ownRotStage = payload.rotStage();

        if ("conversation".equals(mode) && !"conversation".equals(previousMode)) {
            mc.setScreen(new LockiPlantDialogueScreen());
        } else if (!"conversation".equals(mode) && mc.screen instanceof LockiPlantDialogueScreen) {
            mc.setScreen(null);
        }
    }

    public static void tick() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null) {
            restoreCamera(mc);
            return;
        }

        boolean specialView = "roots".equals(mode) || "apple".equals(mode);
        if (specialView && !specialCameraActive) savedCameraType = mc.options.getCameraType();
        if (!specialView && specialCameraActive) restoreSavedCameraType(mc);
        specialCameraActive = specialView;

        if ("roots".equals(mode)) {
            mc.setCameraEntity(player);
            mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
            player.setPose(Pose.SWIMMING);
            setSwimmingPose = true;
            spawnSpiritVisuals(mc, player);
        } else if ("apple".equals(mode)) {
            player.setPose(Pose.STANDING);
            setSwimmingPose = false;
            Entity cameraTarget = mc.level.getEntity(cameraEntityId);
            if (cameraTarget == null) cameraTarget = player;
            mc.setCameraEntity(cameraTarget);
            if (cameraMode == 2) {
                // The apple is held in the target's main hand: use a first-person view and no spirit shader.
                mc.options.setCameraType(CameraType.FIRST_PERSON);
            } else if (cameraMode == 3) {
                // It was picked up but put away: look over the carrier's shoulder.
                mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
            } else {
                // The apple is on the ground; the camera is placed at the item entity itself.
                mc.options.setCameraType(CameraType.FIRST_PERSON);
            }
        } else {
            if (mc.getCameraEntity() != player) mc.setCameraEntity(player);
            if (setSwimmingPose) {
                player.setPose(Pose.STANDING);
                setSwimmingPose = false;
            }
            if ("spirit".equals(mode)) spawnSpiritVisuals(mc, player);
        }
    }

    private static void spawnSpiritVisuals(Minecraft mc, LocalPlayer player) {
        if (player.tickCount % 8 != 0) return;
        if (lastNetworkBuild == Long.MIN_VALUE || player.tickCount - lastNetworkBuild >= 20
                || cachedSegments.isEmpty()) {
            BlockPos center = player.blockPosition();
            cachedPlants = LockiRootNetwork.findPlants(mc.level, center, LockiRootNetwork.DEFAULT_RADIUS);
            cachedSegments = LockiRootNetwork.build(mc.level, cachedPlants, LockiRootNetwork.DEFAULT_RADIUS);
            lastNetworkBuild = player.tickCount;
        }
        Vec3 center = player.position();
        List<LockiRootNetwork.Segment> nearest = new ArrayList<>(cachedSegments);
        nearest.sort(Comparator.comparingDouble(segment -> segment.start().distanceToSqr(center) + segment.end().distanceToSqr(center)));
        int drawn = 0;
        int rootParticleCount = 0;
        for (LockiRootNetwork.Segment segment : nearest) {
            if (drawn++ >= 120 || rootParticleCount >= 1050) break;
            Vec3 a = segment.start();
            Vec3 b = segment.end();
            double dx = b.x - a.x;
            double dy = b.y - a.y;
            double dz = b.z - a.z;
            double length = Math.sqrt(dx * dx + dy * dy + dz * dz);
            int samples = Math.max(3, Math.min(48, (int) (length / 0.38)));
            double horizontal = Math.sqrt(dx * dx + dz * dz);
            double sideX = horizontal < 1.0E-4 ? 0.045 : -dz / horizontal * 0.055;
            double sideZ = horizontal < 1.0E-4 ? 0.0 : dx / horizontal * 0.055;
            for (int i = 0; i <= samples && rootParticleCount < 1050; i++) {
                double t = i / (double) samples;
                double x = a.x + dx * t;
                double y = a.y + dy * t;
                double z = a.z + dz * t;
                if (player.distanceToSqr(x, y, z) > 22.0 * 22.0) continue;
                // A close central filament plus two fine side filaments read as a thick, branching root.
                mc.level.addParticle(new WispTrailParticleEffect(0.72F, 0.91F, 0.91F, 0.0F, 0.0F, 0.0F),
                    x, y, z, 0.0, 0.001, 0.0);
                rootParticleCount++;
                if (rootParticleCount < 1050 && i % 2 == 0) {
                    mc.level.addParticle(new WispTrailParticleEffect(0.55F, 0.77F, 0.77F, 0.0F, 0.0F, 0.0F),
                        x + sideX, y + 0.018, z + sideZ, 0.0, 0.001, 0.0);
                    rootParticleCount++;
                }
                if (rootParticleCount < 1050 && i % 3 == 0) {
                    mc.level.addParticle(new WispTrailParticleEffect(0.86F, 0.95F, 0.92F, 0.0F, 0.0F, 0.0F),
                        x - sideX, y - 0.018, z - sideZ, 0.0, 0.001, 0.0);
                    rootParticleCount++;
                }
            }
        }

        int glowingPlants = 0;
        for (LockiRootNetwork.PlantNode plant : cachedPlants) {
            if (glowingPlants++ >= 42) break;
            Vec3 p = plant.anchor();
            if (player.distanceToSqr(p) > 20.0 * 20.0) continue;
            // Bright, full-bright motes form a soft halo, stronger than the root filaments.
            for (int i = 0; i < (plant.tree() ? 8 : 3); i++) {
                double ox = (player.getRandom().nextDouble() - 0.5) * (plant.tree() ? 1.15 : 0.55);
                double oz = (player.getRandom().nextDouble() - 0.5) * (plant.tree() ? 1.15 : 0.55);
                double oy = player.getRandom().nextDouble() * (plant.tree() ? 2.0 : 0.7);
                mc.level.addParticle(new WispTrailParticleEffect(0.82F, 0.93F, 0.87F, 0.0F, 0.0F, 0.0F),
                    p.x + ox, p.y + oy, p.z + oz, 0.0, 0.018, 0.0);
            }
        }

        // A few motes sit inside actual leaf blocks, so tree canopies feel softly lit while
        // grass blocks and the rest of the terrain remain close to monochrome.
        int scannedTrees = 0;
        for (LockiRootNetwork.PlantNode plant : cachedPlants) {
            if (!plant.tree() || scannedTrees++ >= 8) continue;
            Vec3 p = plant.anchor();
            if (player.distanceToSqr(p) > 20.0 * 20.0) continue;
            spawnTreeCanopyMotes(mc, player, p);
        }

    }

    private static void spawnTreeCanopyMotes(Minecraft mc, LocalPlayer player, Vec3 base) {
        int placed = 0;
        int centerX = (int) Math.floor(base.x);
        int centerY = (int) Math.floor(base.y);
        int centerZ = (int) Math.floor(base.z);
        search:
        for (int y = 2; y <= 8; y++) {
            for (int x = -3; x <= 3; x++) {
                for (int z = -3; z <= 3; z++) {
                    BlockPos pos = new BlockPos(centerX + x, centerY + y, centerZ + z);
                    if (!mc.level.hasChunkAt(pos) || !mc.level.getBlockState(pos).is(net.minecraft.tags.BlockTags.LEAVES)) continue;
                    if (player.getRandom().nextFloat() > 0.16F) continue;
                    mc.level.addParticle(new WispTrailParticleEffect(0.82F, 0.91F, 0.84F, 0.0F, 0.0F, 0.0F),
                        pos.getX() + 0.2 + player.getRandom().nextDouble() * 0.6,
                        pos.getY() + 0.2 + player.getRandom().nextDouble() * 0.6,
                        pos.getZ() + 0.2 + player.getRandom().nextDouble() * 0.6,
                        0.0, 0.006, 0.0);
                    if (++placed >= 2) break search;
                }
            }
        }
    }

    public static int rotStage(int entityId) {
        return ROT_STAGES.getOrDefault(entityId, 0);
    }

    public static int ownRotStage() { return ownRotStage; }
    public static String mode() { return mode; }
    public static int cameraMode() { return cameraMode; }
    public static boolean isRootDiving() { return "roots".equals(mode); }
    public static boolean isInvisibleMode(int playerEntityId) {
        String entityMode = ENTITY_MODES.get(playerEntityId);
        return "spirit".equals(entityMode) || "roots".equals(entityMode)
            || "conversation".equals(entityMode) || "apple".equals(entityMode);
    }
    public static boolean isInvisibleMode() {
        return "spirit".equals(mode) || "roots".equals(mode)
            || "conversation".equals(mode) || "apple".equals(mode);
    }
    public static boolean shouldSuppressFlight() { return "roots".equals(mode) || "conversation".equals(mode) || "apple".equals(mode); }
    public static boolean isLockiModeActive() { return mode != null && !mode.isBlank() && !"none".equals(mode); }
    public static boolean isSpiritVision() { return "spirit".equals(mode) || "roots".equals(mode); }
    public static boolean isAppleGroundVision() { return "apple".equals(mode) && cameraMode == 4; }
    public static ResourceLocation desiredPostEffect() {
        if (isSpiritVision()) {
            LocalPlayer player = Minecraft.getInstance().player;
            if (player != null && player.hasEffect(RequiemStatusEffects.WHITE_VISION)) {
                return RequiemClient.LOCKI_WHITE_VISION_POST_EFFECT;
            }
            return RequiemClient.LOCKI_SPIRIT_POST_EFFECT;
        }
        if (isAppleGroundVision()) return RequiemClient.LOCKI_APPLE_POST_EFFECT;
        return null;
    }

    public static void reset() {
        ROT_STAGES.clear();
        ENTITY_MODES.clear();
        mode = "none";
        previousMode = "none";
        cameraEntityId = -1;
        cameraMode = 0;
        ownRotStage = 0;
        lastPlayerEntityId = -1;
        lastNetworkBuild = Long.MIN_VALUE;
        cachedPlants = List.of();
        cachedSegments = List.of();
        setSwimmingPose = false;
        specialCameraActive = false;
        savedCameraType = null;
    }

    private static void restoreCamera(Minecraft mc) {
        if (specialCameraActive) restoreSavedCameraType(mc);
        if (mc.player != null && mc.getCameraEntity() != mc.player) mc.setCameraEntity(mc.player);
        specialCameraActive = false;
    }

    private static void restoreSavedCameraType(Minecraft mc) {
        if (savedCameraType != null) mc.options.setCameraType(savedCameraType);
        savedCameraType = null;
    }
}
