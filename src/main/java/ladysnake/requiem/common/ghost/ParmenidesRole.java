package ladysnake.requiem.common.ghost;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/** Role marker for Parmenides, Requiem's multi-dimensional ghost player. */
public final class ParmenidesRole {
    public static final String SCOREBOARD_TAG = "Parmenides";

    private ParmenidesRole() { }

    public static boolean isParmenides(ServerPlayer player) {
        return isParmenides((Player) player);
    }

    public static boolean isParmenides(Player player) {
        return player.getTags().contains(SCOREBOARD_TAG);
    }
}

