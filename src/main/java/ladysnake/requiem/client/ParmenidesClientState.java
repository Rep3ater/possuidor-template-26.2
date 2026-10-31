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
    private static final Set<Long> HIDDEN_ORIGINAL_CHUNKS = ConcurrentHashMap.newKeySet();
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
        restoreHiddenOriginalChunks();
        netherPreview = true;
        previewStartedAt = -1L;
        previewCenterChunkX = centerChunkX;
        previewCenterChunkZ = centerChunkZ;
        previewPlayerInNether = playerInNether;
        PREVIEW_CHUNKS.clear();
        terrainRefreshPending = false;
    }

    public static void movePreviewCenter(int centerChunkX, int centerChunkZ, boolean playerInNether) {
        if (centerChunkX != previewCenterChunkX || centerChunkZ != previewCenterChunkZ
                || playerInNether != previewPlayerInNether) {
            restoreHiddenOriginalChunks();
            previewCenterChunkX = centerChunkX;
            previewCenterChunkZ = centerChunkZ;
            previewPlayerInNether = playerInNether;
            previewStartedAt = -1L;
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
        if (PREVIEW_CHUNKS.size() == 9) {
            terrainRefreshPending = true;
            Minecraft mc = Minecraft.getInstance();
            previewStartedAt = mc.level == null ? 0L : mc.level.getGameTime();
        }
    }

    /**
     * Hide each original chunk only when the expanding counterpart domain reaches it.
     * This keeps the original chunk visible until its replacement is actually arriving.
     */
    public static void refreshHiddenTerrainIfNeeded() {
        if (PREVIEW_CHUNKS.size() != 9) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.levelRenderer == null) return;
        terrainRefreshPending = false;

        double radius = revealRadius();
        if (mc.player == null) return;
        double playerX = mc.player.getX();
        double playerZ = mc.player.getZ();
        for (int z = previewCenterChunkZ - 1; z <= previewCenterChunkZ + 1; z++) {
            for (int x = previewCenterChunkX - 1; x <= previewCenterChunkX + 1; x++) {
                long key = chunkKey(x, z);
                if (HIDDEN_ORIGINAL_CHUNKS.contains(key)) continue;
                double nearestX = Math.max(x << 4, Math.min(playerX, (x << 4) + 16.0D));
                double nearestZ = Math.max(z << 4, Math.min(playerZ, (z << 4) + 16.0D));
                double dx = nearestX - playerX;
                double dz = nearestZ - playerZ;
                if (dx * dx + dz * dz <= radius * radius) {
                    HIDDEN_ORIGINAL_CHUNKS.add(key);
                    markChunkDirty(x, z);
                    terrainWasHidden = true;
                }
            }
        }
    }

    private static void markChunkDirty(int chunkX, int chunkZ) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.levelRenderer == null) return;
        mc.levelRenderer.setBlocksDirty(
            chunkX << 4, mc.level.getMinBuildHeight(), chunkZ << 4,
            (chunkX << 4) + 15, mc.level.getMaxBuildHeight() - 1, (chunkZ << 4) + 15
        );
    }

    private static void restoreHiddenOriginalChunks() {
        if (HIDDEN_ORIGINAL_CHUNKS.isEmpty()) {
            terrainWasHidden = false;
            return;
        }
        for (long key : HIDDEN_ORIGINAL_CHUNKS) {
            int x = (int) (key >> 32);
            int z = (int) key;
            markChunkDirty(x, z);
        }
        HIDDEN_ORIGINAL_CHUNKS.clear();
        terrainWasHidden = false;
    }

    public static void clearPreview() {
        restoreHiddenOriginalChunks();
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
        return HIDDEN_ORIGINAL_CHUNKS.contains(chunkKey(cx, cz));
    }

    /** Hide the full original-dimension column of a chunk once its counterpart is revealed. */
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
