package ladysnake.requiem.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** Client copy of the server-only scoreboard role, keyed by tracked entity ID. */
public final class ParmenidesClientState {
    private static final Set<Integer> ACTIVE_PLAYERS = ConcurrentHashMap.newKeySet();
    private static final Set<Integer> OUT_OF_BODY_BODIES = ConcurrentHashMap.newKeySet();
    private static volatile boolean netherPreview;

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

    public static boolean shouldPreviewNether() {
        Minecraft mc = Minecraft.getInstance();
        return netherPreview && mc.player != null && isParmenides(mc.player);
    }
}
