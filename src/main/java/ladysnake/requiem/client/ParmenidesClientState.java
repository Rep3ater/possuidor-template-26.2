package ladysnake.requiem.client;

import ladysnake.requiem.common.ghost.ParmenidesRole;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** Client-side state for Parmenides' counterpart-dimension domain. */
public final class ParmenidesClientState {
    public record PreviewBlock(int localIndex, BlockState state) { }

    public static final class PreviewChunk {
        private final int chunkX, chunkZ, minY, height;
        private final List<PreviewBlock> surfaceBlocks;

        public PreviewChunk(int chunkX, int chunkZ, int minY, int height, List<Integer> encoded) {
            this.chunkX = chunkX;
            this.chunkZ = chunkZ;
            this.minY = minY;
            this.height = height;
            List<PreviewBlock> decoded = new ArrayList<>(encoded.size() / 2);
            for (int i = 0; i + 1 < encoded.size(); i += 2) {
                BlockState state = Block.BLOCK_STATE_REGISTRY.byId(encoded.get(i + 1));
                if (state != null && !state.isAir()) decoded.add(new PreviewBlock(encoded.get(i), state));
            }
            this.surfaceBlocks = List.copyOf(decoded);
        }

        public int chunkX() { return chunkX; }
        public int chunkZ() { return chunkZ; }
        public int minY() { return minY; }
        public int height() { return height; }
        public List<PreviewBlock> surfaceBlocks() { return surfaceBlocks; }
    }

    private static final Set<Integer> ACTIVE_PLAYERS = ConcurrentHashMap.newKeySet();
    private static final Set<Integer> OUT_OF_BODY_BODIES = ConcurrentHashMap.newKeySet();
    private static final Map<Long, PreviewChunk> PREVIEW_CHUNKS = new ConcurrentHashMap<>();
    private static volatile boolean netherPreview;
    private static volatile boolean previewPlayerInNether;
    private static volatile int previewCenterChunkX;
    private static volatile int previewCenterChunkZ;
    private static volatile long previewStartedAt = -1L;
    private static boolean terrainRefreshPending;
    private static boolean terrainWasHidden;

    private ParmenidesClientState() { }

    public static void update(int entityId, boolean active) {
        if (active) ACTIVE_PLAYERS.add(entityId);
        else ACTIVE_PLAYERS.remove(entityId);
    }

    public static boolean isParmenides(Player player) {
        return player.getTags().contains(ParmenidesRole.SCOREBOARD_TAG) || ACTIVE_PLAYERS.contains(player.getId());
    }

    public static void clear() {
        ACTIVE_PLAYERS.clear();
        OUT_OF_BODY_BODIES.clear();
        clearPreview();
    }

    public static void updateBody(int entityId, boolean active) {
        if (active) OUT_OF_BODY_BODIES.add(entityId);
        else OUT_OF_BODY_BODIES.remove(entityId);
    }

    public static boolean isOutOfBodyBody(int entityId) {
        return OUT_OF_BODY_BODIES.contains(entityId);
    }

    public static void beginPreview(long gameTime, int centerChunkX, int centerChunkZ, boolean playerInNether) {
        netherPreview = true;
        previewStartedAt = gameTime;
        previewCenterChunkX = centerChunkX;
        previewCenterChunkZ = centerChunkZ;
        previewPlayerInNether = playerInNether;
        PREVIEW_CHUNKS.clear();
        terrainRefreshPending = false;
    }

    public static void movePreviewCenter(int centerChunkX, int centerChunkZ, boolean playerInNether) {
        if (centerChunkX != previewCenterChunkX || centerChunkZ != previewCenterChunkZ
                || playerInNether != previewPlayerInNether) {
            if (terrainWasHidden) {
                markSectionsDirty(previewCenterChunkX, previewCenterChunkZ);
                terrainWasHidden = false;
            }
            previewCenterChunkX = centerChunkX;
            previewCenterChunkZ = centerChunkZ;
            previewPlayerInNether = playerInNether;
            PREVIEW_CHUNKS.clear();
            terrainRefreshPending = false;
        }
    }

    public static void acceptPreview(int chunkX, int chunkZ, boolean playerInNether, int minY, int height, List<Integer> states) {
        if (playerInNether != previewPlayerInNether
                || Math.abs(chunkX - previewCenterChunkX) > 1
                || Math.abs(chunkZ - previewCenterChunkZ) > 1) return;
        PREVIEW_CHUNKS.put(chunkKey(chunkX, chunkZ), new PreviewChunk(chunkX, chunkZ, minY, height, states));
        netherPreview = true;
        if (PREVIEW_CHUNKS.size() == 9) terrainRefreshPending = true;
    }

    /** Rebuild local chunk meshes once after all nine counterpart snapshots arrive. */
    public static void refreshHiddenTerrainIfNeeded() {
        if (!terrainRefreshPending || PREVIEW_CHUNKS.size() != 9) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.levelRenderer == null) return;
        terrainRefreshPending = false;
        markSectionsDirty(previewCenterChunkX, previewCenterChunkZ);
        terrainWasHidden = true;
    }

    private static void markSectionsDirty(int centerX, int centerZ) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.levelRenderer == null) return;
        int minSection = mc.level.getMinSection();
        int maxSection = mc.level.getMaxSection();
        for (int z = centerZ - 1; z <= centerZ + 1; z++) {
            for (int x = centerX - 1; x <= centerX + 1; x++) {
                for (int section = minSection; section < maxSection; section++) {
                    mc.levelRenderer.setSectionDirty(x << 4, section << 4, z << 4, false);
                }
            }
        }
    }

    public static void clearPreview() {
        if (terrainWasHidden) {
            markSectionsDirty(previewCenterChunkX, previewCenterChunkZ);
            terrainWasHidden = false;
        }
        netherPreview = false;
        PREVIEW_CHUNKS.clear();
        previewStartedAt = -1L;
        terrainRefreshPending = false;
    }

    public static boolean hasPreview() {
        Minecraft mc = Minecraft.getInstance();
        if (!shouldPreviewNether() || mc.player == null || mc.level == null || !mc.options.keyShift.isDown()
                || PREVIEW_CHUNKS.size() != 9) return false;
        boolean playerInNether = mc.level.dimension() == net.minecraft.world.level.Level.NETHER;
        return previewPlayerInNether == playerInNether
            && previewCenterChunkX == mc.player.chunkPosition().x
            && previewCenterChunkZ == mc.player.chunkPosition().z;
    }

    public static Map<Long, PreviewChunk> previewChunks() {
        return PREVIEW_CHUNKS;
    }

    public static double revealRadius() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || previewStartedAt < 0L) return 0.0D;
        long elapsed = Math.max(0L, mc.level.getGameTime() - previewStartedAt);
        // 52 blocks reaches beyond every corner of the 3x3 chunk square in five seconds.
        return Math.min(52.0D, elapsed * 0.52D);
    }

    public static boolean isInsideReveal(BlockPos pos) {
        if (!hasPreview()) return false;
        int cx = pos.getX() >> 4;
        int cz = pos.getZ() >> 4;
        return PREVIEW_CHUNKS.containsKey(chunkKey(cx, cz));
    }

    /** Hide the entire original-dimension column in every counterpart chunk. */
    public static boolean shouldHideOriginalBlock(BlockPos pos) {
        // renderBatched receives a RenderChunkRegion wrapper, not Minecraft.level itself.
        // The mixin is client-only, so the active preview chunk key is the correct guard.
        return hasPreview() && isInsideReveal(pos);
    }

    public static boolean shouldPreviewNether() {
        Minecraft mc = Minecraft.getInstance();
        return netherPreview && mc.player != null && isParmenides(mc.player);
    }

    private static long chunkKey(int x, int z) {
        return ((long) x << 32) ^ (z & 0xffffffffL);
    }
}
