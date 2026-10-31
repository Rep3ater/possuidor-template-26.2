package ladysnake.requiem.common;

import ladysnake.requiem.common.network.RequiemNetworking;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.ServerChatEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Server-side policy for the optional Say It English integration. */
public final class SayItCompatibilityManager {
    public enum Mode { SAYIT, REQUIEM, DISABLED }
    private static final Map<UUID, Mode> PLAYER_MODES = new ConcurrentHashMap<>();
    private static volatile boolean globallyEnabled = true;

    private SayItCompatibilityManager() { }

    public static Mode mode(ServerPlayer player) {
        return PLAYER_MODES.getOrDefault(player.getUUID(), Mode.SAYIT);
    }

    public static boolean shouldRunVanillaSayIt(ServerPlayer player) {
        return globallyEnabled && mode(player) == Mode.SAYIT;
    }

    public static boolean isGloballyEnabled() {
        return globallyEnabled;
    }

    public static void setGlobalEnabled(boolean enabled) {
        globallyEnabled = enabled;
    }

    public static void setPlayerMode(ServerPlayer player, Mode requested, boolean enabled) {
        Mode current = mode(player);
        if (enabled) {
            PLAYER_MODES.put(player.getUUID(), requested);
        } else if (current == requested) {
            PLAYER_MODES.put(player.getUUID(), requested == Mode.REQUIEM
                ? (globallyEnabled ? Mode.SAYIT : Mode.DISABLED)
                : Mode.DISABLED);
        }
    }

    public static void onChat(ServerChatEvent event) {
        if (event.isCanceled()) return;
        ServerPlayer player = event.getPlayer();
        if (mode(player) != Mode.REQUIEM) return;

        // Cancel the ordinary chat broadcast; only clients tracking this player
        // receive the original message for the in-world bubble.
        event.setCanceled(true);
        String original = event.getMessage().getString();
        if (original == null || original.isBlank()) return;
        if (original.length() > 256) original = original.substring(0, 256);
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(
            player, new RequiemNetworking.SpeechBubblePayload(player.getId(), original));
    }
}
