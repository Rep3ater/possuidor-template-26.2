package ladysnake.requiem.common;

import ladysnake.requiem.Requiem;
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

        // Requiem mode replaces global chat with an in-world bubble.
        event.setCanceled(true);
        String rawText = event.getMessage().getString();
        if (rawText == null || rawText.isBlank()) return;
        final String original = rawText.length() > 256 ? rawText.substring(0, 256) : rawText;

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

        // The speaker must always receive their own bubble, even if a modded player
        // collection unexpectedly omits them from ServerLevel#players().
        String speakerLanguage = languageFor(speaker);
        List<UUID> speakerViewers = viewersByLanguage.computeIfAbsent(speakerLanguage, ignored -> new ArrayList<>());
        if (!speakerViewers.contains(speakerId)) {
            speakerViewers.add(speakerId);
        }

        // Deliver the original text immediately. Translation is an optional enhancement,
        // not a prerequisite for chat delivery: an offline or slow translation service
        // must never make a player's message disappear.
        int recipientCount = 0;
        for (List<UUID> viewerIds : viewersByLanguage.values()) {
            recipientCount += sendBubble(server, speakerId, viewerIds, original);
        }
        Requiem.LOGGER.debug("Requiem chat from {} delivered to {} nearby viewer(s)",
            speaker.getGameProfile().getName(), recipientCount);

        if (!RequiemConfig.speech.translateBubbles) return;

        // Replace the initial text with a translated version once each language group
        // finishes translating. If translation fails, the already-delivered original
        // bubble remains visible.
        for (Map.Entry<String, List<UUID>> group : viewersByLanguage.entrySet()) {
            String targetLanguage = group.getKey();
            List<UUID> viewerIds = List.copyOf(group.getValue());
            SpeechTranslationService.translate(original, targetLanguage).whenComplete((translated, error) -> {
                if (error != null || translated == null || translated.isBlank() || translated.equals(original)) {
                    return;
                }
                try {
                    server.execute(() -> sendBubble(server, speakerId, viewerIds, translated));
                } catch (RuntimeException ignored) {
                    // The server may be shutting down while a remote translation is completing.
                }
            });
        }
    }

    /**
     * Sends a bubble only to players who are still online, in the speaker's dimension,
     * and within the tracking range. This method is called on the logical server thread.
     *
     * @return number of recipients that received the payload
     */
    private static int sendBubble(MinecraftServer server, UUID speakerId, List<UUID> viewerIds, String text) {
        ServerPlayer currentSpeaker = server.getPlayerList().getPlayer(speakerId);
        if (currentSpeaker == null) return 0;

        int sent = 0;
        for (UUID viewerId : viewerIds) {
            ServerPlayer currentViewer = server.getPlayerList().getPlayer(viewerId);
            if (currentViewer == null
                    || currentViewer.serverLevel() != currentSpeaker.serverLevel()
                    || (currentViewer != currentSpeaker
                        && currentViewer.distanceToSqr(currentSpeaker) > SPEECH_BUBBLE_TRACKING_DISTANCE_SQR)) {
                continue;
            }
            PacketDistributor.sendToPlayer(currentViewer,
                new RequiemNetworking.SpeechBubblePayload(currentSpeaker.getId(), text));
            sent++;
        }
        return sent;
    }
}
