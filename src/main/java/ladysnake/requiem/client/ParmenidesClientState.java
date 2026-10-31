package ladysnake.requiem.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** Client-side state for Parmenides' expanding counterpart-dimension vision. */
public final class ParmenidesClientState {
    public record PreviewChunk(int chunkX, int chunkZ, int minY, int height, List<Integer> states) {
        public PreviewChunk {
            states = List.copyOf(states);
        }
        public int blockCount() { return states.size() / 2; }
        public int localIndex(int entry) { return states.get(entry * 2); }
        public BlockState blockState(int entry) {
            BlockState state = Block.BLOCK_STATE_REGISTRY.byId(states.get(entry * 2 + 1));
            return state == null ? net.minecraft.world.level.block.Blocks.AIR.defaultBlockState() : state;
        }
    }

    private static final Set<Integer> ACTIVE_PLAYERS = ConcurrentHashMap.newKeySet();
    private static final Set<Integer> OUT_OF_BODY_BODIES = ConcurrentHashMap.newKeySet();
    private static final Map<Long, PreviewChunk> PREVIEW_CHUNKS = new ConcurrentHashMap<>();
    private static volatile boolean netherPreview;
    private static volatile boolean previewPlayerInNether;
    private static volatile int previewCenterChunkX;
    private static volatile int previewCenterChunkZ;
    private static volatile long previewStartedAt = -1L;

    private ParmenidesClientState() { }

    public static void update(int entityId, boolean active) {
        if (active) ACTIVE_PLAYERS.add(entityId);
        else ACTIVE_PLAYERS.remove(entityId);
    }

    public static boolean isParmenides(Player player) {
        return player.getTags().contains(ladysnake.requiem.common.ghost.ParmenidesRole.SCOREBOARD_TAG)
            || ACTIVE_PLAYERS.contains(player.getId());
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
    }

    public static void movePreviewCenter(int centerChunkX, int centerChunkZ, boolean playerInNether) {
        if (centerChunkX != previewCenterChunkX || centerChunkZ != previewCenterChunkZ
                || playerInNether != previewPlayerInNether) {
            previewCenterChunkX = centerChunkX;
            previewCenterChunkZ = centerChunkZ;
            previewPlayerInNether = playerInNether;
            PREVIEW_CHUNKS.clear();
        }
    }

    public static void acceptPreview(int chunkX, int chunkZ, boolean playerInNether, int minY, int height, List<Integer> states) {
        if (playerInNether != previewPlayerInNether
                || Math.abs(chunkX - previewCenterChunkX) > 1
                || Math.abs(chunkZ - previewCenterChunkZ) > 1) return;
        PREVIEW_CHUNKS.put(chunkKey(chunkX, chunkZ), new PreviewChunk(chunkX, chunkZ, minY, height, states));
        netherPreview = true;
    }

    public static void clearPreview() {
        netherPreview = false;
        PREVIEW_CHUNKS.clear();
        previewStartedAt = -1L;
    }

    public static boolean hasPreview() {
        Minecraft mc = Minecraft.getInstance();
        if (!shouldPreviewNether() || mc.player == null || mc.level == null || !mc.options.keyShift.isDown()
                || PREVIEW_CHUNKS.isEmpty()) return false;
        boolean playerInNether = mc.level.dimension() == net.minecraft.world.level.Level.NETHER;
        return previewPlayerInNether == playerInNether
            && previewCenterChunkX == mc.player.chunkPosition().x
            && previewCenterChunkZ == mc.player.chunkPosition().z;
    }

    public static Map<Long, PreviewChunk> previewChunks() {
        return Map.copyOf(PREVIEW_CHUNKS);
    }

    public static double revealRadius() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || previewStartedAt < 0L) return 0.0D;
        long elapsed = Math.max(0L, mc.level.getGameTime() - previewStartedAt);
        // The domain expands over five seconds to cover the complete 3x3 chunk square.
        return Math.min(36.0D, elapsed / 100.0D * 36.0D);
    }

    public static boolean isInsideReveal(BlockPos pos) {
        if (!hasPreview()) return false;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return false;
        int cx = pos.getX() >> 4;
        int cz = pos.getZ() >> 4;
        if (!PREVIEW_CHUNKS.containsKey(chunkKey(cx, cz))) return false;
        double dx = pos.getX() + 0.5D - mc.player.getX();
        double dz = pos.getZ() + 0.5D - mc.player.getZ();
        double radius = revealRadius();
        return dx * dx + dz * dz <= radius * radius;
    }

    public static boolean shouldHideOriginalBlock(BlockPos pos) {
        return isInsideReveal(pos);
    }

    public static boolean shouldPreviewNether() {
        Minecraft mc = Minecraft.getInstance();
        return netherPreview && mc.player != null && isParmenides(mc.player);
    }

    private static long chunkKey(int x, int z) {
        return ((long) x << 32) ^ (z & 0xffffffffL);
    }
}
