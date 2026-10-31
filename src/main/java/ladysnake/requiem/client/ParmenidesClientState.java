package ladysnake.requiem.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import java.util.List;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** Client copy of the server-only scoreboard role, keyed by tracked entity ID. */
public final class ParmenidesClientState {
    private static final Set<Integer> ACTIVE_PLAYERS = ConcurrentHashMap.newKeySet();
    private static final Set<Integer> OUT_OF_BODY_BODIES = ConcurrentHashMap.newKeySet();
    private static volatile boolean netherPreview;
    private static volatile int previewChunkX;
    private static volatile int previewChunkZ;
    private static volatile boolean previewPlayerInNether;
    private static volatile int previewMinY;
    private static volatile int previewHeight;
    private static volatile List<Integer> previewStates = List.of();

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
        netherPreview = false;
        previewStates = List.of();
        previewHeight = 0;
    }

    public static void updateBody(int entityId, boolean active) {
        if (active) OUT_OF_BODY_BODIES.add(entityId);
        else OUT_OF_BODY_BODIES.remove(entityId);
    }

    public static boolean isOutOfBodyBody(int entityId) {
        return OUT_OF_BODY_BODIES.contains(entityId);
    }

    public static void setNetherPreview(boolean active) {
        netherPreview = active;
    }

    public static void acceptPreview(int chunkX, int chunkZ, boolean playerInNether, int minY, int height, List<Integer> states) {
        previewChunkX = chunkX;
        previewChunkZ = chunkZ;
        previewPlayerInNether = playerInNether;
        previewMinY = minY;
        previewHeight = height;
        previewStates = List.copyOf(states);
        netherPreview = true;
    }

    public static void clearPreview() {
        netherPreview = false;
        previewStates = List.of();
        previewHeight = 0;
    }

    public static boolean hasPreview() {
        Minecraft mc = Minecraft.getInstance();
        if (!shouldPreviewNether() || mc.player == null || !mc.options.keyShift.isDown()
                || previewHeight <= 0 || previewStates.isEmpty()) return false;
        boolean playerInNether = mc.level != null && mc.level.dimension() == net.minecraft.world.level.Level.NETHER;
        return previewPlayerInNether == playerInNether
            && previewChunkX == mc.player.chunkPosition().x
            && previewChunkZ == mc.player.chunkPosition().z;
    }

    public static int previewChunkX() { return previewChunkX; }
    public static int previewChunkZ() { return previewChunkZ; }
    public static int previewMinY() { return previewMinY; }
    public static int previewHeight() { return previewHeight; }

    public static int previewBlockCount() {
        return previewStates.size() / 2;
    }

    public static int previewLocalIndex(int entry) {
        return previewStates.get(entry * 2);
    }

    public static BlockState previewBlockState(int entry) {
        BlockState state = Block.BLOCK_STATE_REGISTRY.byId(previewStates.get(entry * 2 + 1));
        return state == null ? net.minecraft.world.level.block.Blocks.AIR.defaultBlockState() : state;
    }

    public static boolean shouldPreviewNether() {
        Minecraft mc = Minecraft.getInstance();
        return netherPreview && mc.player != null && isParmenides(mc.player);
    }
}
