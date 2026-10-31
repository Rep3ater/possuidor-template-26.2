package ladysnake.requiem.common;

import ladysnake.requiem.common.network.RequiemNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.ServerChatEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Server-side policy and language routing for the optional Say It English integration. */
public final class SayItCompatibilityManager {
    public enum Mode { SAYIT, REQUIEM, DISABLED }

    private static final Map<UUID, Mode> PLAYER_MODES = new ConcurrentHashMap<>();
    private static final Map<UUID, String> PLAYER_LANGUAGES = new ConcurrentHashMap<>();
    private static volatile boolean globallyEnabled = true;
    private static final double SPEECH_BUBBLE_TRACKING_DISTANCE_SQR = 128.0D * 128.0D;

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

    /** Stores a language reported by the client; only a short ISO language code is accepted. */
    public static void setPlayerLanguage(ServerPlayer player, String language) {
        if (language == null) return;
        String normalized = language.trim().toLowerCase(java.util.Locale.ROOT);
        if (normalized.matches("[a-z]{2,3}")) {
            PLAYER_LANGUAGES.put(player.getUUID(), normalized);
        }
    }

    private static String languageFor(ServerPlayer player) {
        String configured = RequiemConfig.speech.defaultLanguage;
        String fallback = configured == null ? "en" : configured.trim().toLowerCase(java.util.Locale.ROOT);
        if (!fallback.matches("[a-z]{2,3}")) fallback = "en";
        return PLAYER_LANGUAGES.getOrDefault(player.getUUID(), fallback);
    }

    public static void onLogout(ServerPlayer player) {
        PLAYER_MODES.remove(player.getUUID());
        PLAYER_LANGUAGES.remove(player.getUUID());
    }

    public static void onChat(ServerChatEvent event) {
        if (event.isCanceled()) return;
        ServerPlayer speaker = event.getPlayer();
        if (mode(speaker) != Mode.REQUIEM) return;

        // Requiem mode replaces global chat with an in-world bubble. Each nearby
        // viewer receives the text translated into their own Minecraft language.
        event.setCanceled(true);
        String original = event.getMessage().getString();
        if (original == null || original.isBlank()) return;
        if (original.length() > 256) original = original.substring(0, 256);

        MinecraftServer server = speaker.getServer();
        if (server == null) return;
        UUID speakerId = speaker.getUUID();

        Map<String, List<UUID>> viewersByLanguage = new HashMap<>();
        for (ServerPlayer viewer : speaker.serverLevel().players()) {
            if (viewer != speaker && viewer.distanceToSqr(speaker) > SPEECH_BUBBLE_TRACKING_DISTANCE_SQR) {
                continue;
            }
            viewersByLanguage.computeIfAbsent(languageFor(viewer), ignored -> new ArrayList<>())
                .add(viewer.getUUID());
        }

        for (Map.Entry<String, List<UUID>> group : viewersByLanguage.entrySet()) {
            String targetLanguage = group.getKey();
            List<UUID> viewerIds = List.copyOf(group.getValue());
            SpeechTranslationService.translate(original, targetLanguage).whenComplete((translated, error) -> {
                String bubbleText = error == null && translated != null && !translated.isBlank()
                    ? translated : original;
                try {
                    server.execute(() -> {
                        ServerPlayer currentSpeaker = server.getPlayerList().getPlayer(speakerId);
                        if (currentSpeaker == null) return;
                        for (UUID viewerId : viewerIds) {
                            ServerPlayer currentViewer = server.getPlayerList().getPlayer(viewerId);
                            if (currentViewer == null
                                    || currentViewer.serverLevel() != currentSpeaker.serverLevel()
                                    || (currentViewer != currentSpeaker
                                        && currentViewer.distanceToSqr(currentSpeaker) > SPEECH_BUBBLE_TRACKING_DISTANCE_SQR)) {
                                continue;
                            }
                            PacketDistributor.sendToPlayer(currentViewer,
                                new RequiemNetworking.SpeechBubblePayload(currentSpeaker.getId(), bubbleText));
                        }
                    });
                } catch (RuntimeException ignored) {
                    // The server may be shutting down while a remote translation is completing.
                }
            });
        }
    }
}