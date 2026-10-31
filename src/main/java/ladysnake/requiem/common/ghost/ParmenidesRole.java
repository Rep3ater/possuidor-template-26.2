package ladysnake.requiem.common.ghost;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** Role marker for Parmenides, Requiem's multi-dimensional ghost player. */
public final class ParmenidesRole {
    public static final String SCOREBOARD_TAG = "Parmenides";
    private static final Set<Integer> CLIENT_ACTIVE_PLAYERS = ConcurrentHashMap.newKeySet();

    private ParmenidesRole() { }

    public static boolean isParmenides(ServerPlayer player) {
        return isParmenides((Player) player);
    }

    public static boolean isParmenides(Player player) {
        return player.getTags().contains(SCOREBOARD_TAG)
            || (player.level().isClientSide() && CLIENT_ACTIVE_PLAYERS.contains(player.getId()));
    }

    public static void updateClientState(int entityId, boolean active) {
        if (active) CLIENT_ACTIVE_PLAYERS.add(entityId);
        else CLIENT_ACTIVE_PLAYERS.remove(entityId);
    }

    public static void clearClientStates() {
        CLIENT_ACTIVE_PLAYERS.clear();
    }
}
