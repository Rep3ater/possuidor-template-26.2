package ladysnake.requiem.common;

import ladysnake.requiem.Requiem;
import ladysnake.requiem.common.network.RequiemNetworking;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.ServerChatEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Server-side policy, Say It challenge prompting, and language routing for speech bubbles. */
public final class SayItCompatibilityManager {
    public enum Mode { SAYIT, REQUIEM, DISABLED }

    public record PendingSpeech(String speechText, String originalInput, int color, long createdAt) {}

    private static final String NBT_KEY_MODE = "RequiemSpeechMode";
    private static final String NBT_KEY_LANG = "RequiemSpeechLanguage";
    private static final Map<UUID, Mode> PLAYER_MODES = new ConcurrentHashMap<>();
    private static final Map<UUID, String> PLAYER_LANGUAGES = new ConcurrentHashMap<>();
    private static final Map<UUID, PendingSpeech> PENDING_SPEECH = new ConcurrentHashMap<>();
    private static final ThreadLocal<UUID> REQUIEM_PROMPTING = new ThreadLocal<>();
    private static volatile boolean globallyEnabled = true;
    private static final double SPEECH_BUBBLE_TRACKING_DISTANCE_SQR = 128.0D * 128.0D;

    private SayItCompatibilityManager() { }

    public static Mode mode(ServerPlayer player) {
        Mode cached = PLAYER_MODES.get(player.getUUID());
        if (cached != null) return cached;

        CompoundTag persistent = player.getPersistentData();
        if (persistent.contains(NBT_KEY_MODE, Tag.TAG_STRING)) {
            try {
                String saved = persistent.getString(NBT_KEY_MODE);
                Mode mode = Mode.valueOf(saved.toUpperCase(Locale.ROOT));
                PLAYER_MODES.put(player.getUUID(), mode);
                return mode;
            } catch (IllegalArgumentException ignored) {
            }
        }

        Mode fallback = Mode.SAYIT;
        PLAYER_MODES.put(player.getUUID(), fallback);
        return fallback;
    }

    public static boolean shouldAllowSayItChallenge(ServerPlayer player) {
        UUID prompting = REQUIEM_PROMPTING.get();
        if (prompting != null && prompting.equals(player.getUUID())) {
            return true;
        }
        return globallyEnabled && mode(player) == Mode.SAYIT;
    }

    public static boolean shouldRunVanillaSayIt(ServerPlayer player) {
        return shouldAllowSayItChallenge(player);
    }

    public static boolean isGloballyEnabled() {
        return globallyEnabled;
    }

    public static void setGlobalEnabled(boolean enabled) {
        globallyEnabled = enabled;
    }

    public static void setPlayerMode(ServerPlayer player, Mode requested, boolean enabled) {
        Mode current = mode(player);
        Mode newMode;
        if (enabled) {
            newMode = requested;
        } else if (current == requested) {
            newMode = (requested == Mode.REQUIEM
                ? (globallyEnabled ? Mode.SAYIT : Mode.DISABLED)
                : Mode.DISABLED);
        } else {
            return;
        }
        PLAYER_MODES.put(player.getUUID(), newMode);
        player.getPersistentData().putString(NBT_KEY_MODE, newMode.name());
    }

    /** Stores a language reported by the client; only a short ISO language code is accepted. */
    public static void setPlayerLanguage(ServerPlayer player, String language) {
        if (language == null) return;
        String normalized = language.trim().toLowerCase(Locale.ROOT);
        if (normalized.matches("[a-z]{2,3}")) {
            PLAYER_LANGUAGES.put(player.getUUID(), normalized);
            player.getPersistentData().putString(NBT_KEY_LANG, normalized);
        }
    }

    public static String languageFor(ServerPlayer player) {
        String cached = PLAYER_LANGUAGES.get(player.getUUID());
        if (cached != null) return cached;

        CompoundTag persistent = player.getPersistentData();
        if (persistent.contains(NBT_KEY_LANG, Tag.TAG_STRING)) {
            String saved = persistent.getString(NBT_KEY_LANG).trim().toLowerCase(Locale.ROOT);
            if (saved.matches("[a-z]{2,3}")) {
                PLAYER_LANGUAGES.put(player.getUUID(), saved);
                return saved;
            }
        }

        String configured = RequiemConfig.speech.defaultLanguage;
        String fallback = configured == null ? "en" : configured.trim().toLowerCase(Locale.ROOT);
        if (!fallback.matches("[a-z]{2,3}")) fallback = "en";
        return fallback;
    }

    /**
     * Retrieves the language the player chose for Say It at the start of the game,
     * falling back to the player's reported language if Say It English is not active.
     */
    public static String getSayItLanguage(ServerPlayer player) {
        try {
            Class<?> cm = Class.forName("com.spicesfire.sayitenglish.ChallengeManager");
            Method m = cm.getMethod("progress", ServerPlayer.class);
            Object prog = m.invoke(null, player);
            if (prog != null) {
                Field f = prog.getClass().getField("language");
                Object lang = f.get(prog);
                if (lang instanceof String s && !s.isBlank()) {
                    String code = s.trim().toLowerCase(Locale.ROOT);
                    if (code.matches("[a-z]{2,3}")) {
                        return code;
                    }
                }
            }
        } catch (Throwable ignored) {
        }
        return languageFor(player);
    }

    /**
     * Prompts a Say It English challenge on the player's screen to speak the translated text.
     */
    public static boolean promptSayItChallenge(ServerPlayer player, String toSpeak, String original) {
        try {
            Class<?> cm = Class.forName("com.spicesfire.sayitenglish.ChallengeManager");
            Method stateMethod = cm.getDeclaredMethod("state", ServerPlayer.class);
            stateMethod.setAccessible(true);
            Object state = stateMethod.invoke(null, player);

            Class<?> stateClass = Class.forName("com.spicesfire.sayitenglish.ChallengeManager$State");
            Method startMethod = cm.getDeclaredMethod("start", ServerPlayer.class, stateClass, long.class, String.class, String.class, String.class, int.class);
            startMethod.setAccessible(true);

            long now = player.serverLevel().getGameTime();
            REQUIEM_PROMPTING.set(player.getUUID());
            try {
                startMethod.invoke(null, player, state, now, "word", toSpeak, original, 15);
            } finally {
                REQUIEM_PROMPTING.remove();
            }
            return true;
        } catch (Throwable t) {
            Requiem.LOGGER.debug("[Requiem] Could not prompt Say It challenge: {}", t.getMessage());
            return false;
        }
    }

    /**
     * Callback when Say It finishes scoring a challenge.
     * If the player passed the speech challenge, their speech bubble is spawned above their head.
     */
    public static void onChallengeCompleted(ServerPlayer player, Object state, int score, String heard) {
        PendingSpeech pending = PENDING_SPEECH.remove(player.getUUID());
        if (pending == null) return;

        int passScore = 50;
        try {
            Field activeField = state.getClass().getDeclaredField("active");
            activeField.setAccessible(true);
            Object challenge = activeField.get(state);
            if (challenge != null) {
                Method passMethod = challenge.getClass().getMethod("passScore");
                passScore = (int) passMethod.invoke(challenge);
            }
        } catch (Throwable ignored) {
        }

        if (score >= passScore) {
            Requiem.LOGGER.info("[Requiem] Player {} passed speech challenge for '{}' with score {} >= {}!",
                player.getGameProfile().getName(), pending.speechText(), score, passScore);
            MinecraftServer server = player.getServer();
            if (server != null) {
                broadcastSpeechBubble(server, player, pending.speechText(), pending.color());
            }
        } else {
            Requiem.LOGGER.info("[Requiem] Player {} failed speech challenge for '{}' (score {} < {})",
                player.getGameProfile().getName(), pending.speechText(), score, passScore);
        }
    }

    public static void onLogin(ServerPlayer player) {
        mode(player);
        languageFor(player);
    }

    public static void onLogout(ServerPlayer player) {
        Mode mode = PLAYER_MODES.get(player.getUUID());
        if (mode != null) {
            player.getPersistentData().putString(NBT_KEY_MODE, mode.name());
        }
        PLAYER_MODES.remove(player.getUUID());
        PLAYER_LANGUAGES.remove(player.getUUID());
        PENDING_SPEECH.remove(player.getUUID());
    }

    /**
     * Handles chat messages in Requiem mode:
     * 1. Cancels normal chat message.
     * 2. Translates the text into the player's chosen Say It language.
     * 3. Prompts the player to speak the translated text via Say It English.
     * 4. When spoken successfully, spawns the speech bubble above the player's head.
     */
    public static void onChat(ServerChatEvent event) {
        if (event.isCanceled()) return;
        ServerPlayer speaker = event.getPlayer();
        if (mode(speaker) != Mode.REQUIEM) return;

        // Requiem mode replaces global chat with speech bubble challenges
        event.setCanceled(true);
        String rawText = event.getMessage().getString();
        if (rawText == null || rawText.isBlank()) return;

        SpeechBubbleColor.ParsedMessage parsed = SpeechBubbleColor.parseMessage(rawText);
        int bubbleColor = parsed.outlineColor();
        String cleanText = parsed.text();
        if (cleanText.isBlank()) return;
        final String original = cleanText.length() > 256 ? cleanText.substring(0, 256) : cleanText;

        MinecraftServer server = speaker.getServer();
        if (server == null) return;

        String sayItTargetLang = getSayItLanguage(speaker);
        Requiem.LOGGER.info("[Requiem] Player {} typed: '{}' in Requiem mode (Say It target language: {})",
            speaker.getGameProfile().getName(), original, sayItTargetLang);

        // Translate typed text into player's Say It learning language
        SpeechTranslationService.translate(original, sayItTargetLang).whenComplete((promptText, error) -> {
            final String toSpeak = (error == null && promptText != null && !promptText.isBlank())
                ? promptText : original;

            server.execute(() -> {
                PENDING_SPEECH.put(speaker.getUUID(), new PendingSpeech(toSpeak, original, bubbleColor, System.currentTimeMillis()));
                boolean challenged = promptSayItChallenge(speaker, toSpeak, original);
                if (!challenged) {
                    // Fallback if Say It English is not installed or could not prompt
                    PENDING_SPEECH.remove(speaker.getUUID());
                    broadcastSpeechBubble(server, speaker, toSpeak, bubbleColor);
                }
            });
        });
    }

    /**
     * Broadcasts the speech bubble to the speaker and nearby viewers,
     * automatically translating the message into each viewer's chosen language.
     */
    public static void broadcastSpeechBubble(MinecraftServer server, ServerPlayer speaker, String text) {
        broadcastSpeechBubble(server, speaker, text, SpeechBubbleColor.DEFAULT_OUTLINE);
    }

    /**
     * Broadcasts the speech bubble to the speaker and nearby viewers with the specified theme color,
     * automatically translating the message into each viewer's chosen language.
     */
    public static void broadcastSpeechBubble(MinecraftServer server, ServerPlayer speaker, String text, int color) {
        UUID speakerId = speaker.getUUID();
        Map<String, List<UUID>> viewersByLanguage = new HashMap<>();

        for (ServerPlayer viewer : speaker.serverLevel().players()) {
            if (viewer != speaker && viewer.distanceToSqr(speaker) > SPEECH_BUBBLE_TRACKING_DISTANCE_SQR) {
                continue;
            }
            viewersByLanguage.computeIfAbsent(languageFor(viewer), ignored -> new ArrayList<>())
                .add(viewer.getUUID());
        }

        // Ensure the speaker always receives their own bubble
        String speakerLanguage = languageFor(speaker);
        List<UUID> speakerViewers = viewersByLanguage.computeIfAbsent(speakerLanguage, ignored -> new ArrayList<>());
        if (!speakerViewers.contains(speakerId)) {
            speakerViewers.add(speakerId);
        }

        // Deliver original speech text immediately
        int recipientCount = 0;
        for (List<UUID> viewerIds : viewersByLanguage.values()) {
            recipientCount += sendBubble(server, speakerId, viewerIds, text, color);
        }
        Requiem.LOGGER.info("[Requiem] Speech bubble from {} (entity {}) sent to {}/{} nearby viewer(s): '{}'",
            speaker.getGameProfile().getName(), speaker.getId(), recipientCount,
            viewersByLanguage.values().stream().mapToInt(List::size).sum(), text);

        if (!RequiemConfig.speech.translateBubbles) return;

        // Deliver per-viewer translations asynchronously
        for (Map.Entry<String, List<UUID>> group : viewersByLanguage.entrySet()) {
            String targetLanguage = group.getKey();
            if (targetLanguage.equalsIgnoreCase(speakerLanguage)) continue;

            List<UUID> viewerIds = List.copyOf(group.getValue());
            SpeechTranslationService.translate(text, targetLanguage).whenComplete((translated, error) -> {
                if (error != null || translated == null || translated.isBlank() || translated.equals(text)) {
                    return;
                }
                try {
                    server.execute(() -> sendBubble(server, speakerId, viewerIds, translated, color));
                } catch (RuntimeException ignored) {
                }
            });
        }
    }

    /**
     * Sends a bubble only to players who are still online, in the speaker's dimension,
     * and within the tracking range. This method is called on the logical server thread.
     */
    private static int sendBubble(MinecraftServer server, UUID speakerId, List<UUID> viewerIds, String text, int color) {
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
                new RequiemNetworking.SpeechBubblePayload(currentSpeaker.getId(), text, color));
            sent++;
        }
        return sent;
    }
}
