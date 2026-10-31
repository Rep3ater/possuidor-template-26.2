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

    public static void acceptPreview(int chunkX, int chunkZ, int minY, int height, List<Integer> states) {
        previewChunkX = chunkX;
        previewChunkZ = chunkZ;
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
        return shouldPreviewNether() && previewHeight > 0 && !previewStates.isEmpty();
    }

    public static int previewChunkX() { return previewChunkX; }
    public static int previewChunkZ() { return previewChunkZ; }
    public static int previewMinY() { return previewMinY; }
    public static int previewHeight() { return previewHeight; }

    public static BlockState previewState(int x, int y, int z) {
        int index = (y * 16 + z) * 16 + x;
        List<Integer> states = previewStates;
        if (index < 0 || index >= states.size()) return net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();
        BlockState state = Block.BLOCK_STATE_REGISTRY.byId(states.get(index));
        return state == null ? net.minecraft.world.level.block.Blocks.AIR.defaultBlockState() : state;
    }

    public static boolean shouldPreviewNether() {
        Minecraft mc = Minecraft.getInstance();
        return netherPreview && mc.player != null && isParmenides(mc.player);
    }
}
