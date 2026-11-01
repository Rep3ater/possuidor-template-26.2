package ladysnake.requiem.client;

import ladysnake.requiem.common.SpeechBubbleColor;

import java.util.HashMap;
import java.util.Map;

/** Client-only state for short-lived, typewritten Requiem speech bubbles with Animal Crossing sounds, pitch control, and custom dye colors. */
public final class SpeechBubbleClientState {
    private static final Map<Integer, Bubble> BUBBLES = new HashMap<>();
    private static long clientTick;

    private SpeechBubbleClientState() { }

    public static void show(int entityId, String originalText) {
        show(entityId, originalText, SpeechBubbleColor.DEFAULT_OUTLINE, SpeechBubbleColor.DEFAULT_PITCH);
    }

    public static void show(int entityId, String originalText, int color) {
        show(entityId, originalText, color, SpeechBubbleColor.DEFAULT_PITCH);
    }

    public static void show(int entityId, String originalText, int color, float pitch) {
        if (originalText == null || originalText.isBlank()) return;
        String text = originalText.length() > 256 ? originalText.substring(0, 256) : originalText;
        BUBBLES.put(entityId, new Bubble(text, color, pitch, clientTick));
    }

    public static void tick() {
        clientTick++;

        // Animal Crossing (Animalese) gibberish chatter sound effect on character reveal with regulated pitch
        for (Bubble bubble : BUBBLES.values()) {
            int prevVisible = (int) Math.min(bubble.text().length(), Math.max(0L, (clientTick - 1L - bubble.startedAt()) / 2L));
            int currVisible = (int) Math.min(bubble.text().length(), Math.max(0L, (clientTick - bubble.startedAt()) / 2L));

            if (currVisible > prevVisible && currVisible <= bubble.text().length()) {
                char c = bubble.text().charAt(currVisible - 1);
                if (!Character.isWhitespace(c)) {
                    try {
                        SpeechBubbleSound.play(c, bubble.pitch());
                    } catch (Throwable ignored) {
                    }
                }
            }
        }

        BUBBLES.entrySet().removeIf(entry ->
            clientTick - entry.getValue().startedAt() > entry.getValue().text().length() * 2L + 52L);
    }

    public static String visibleText(int entityId) {
        Bubble bubble = BUBBLES.get(entityId);
        if (bubble == null) return "";
        int visible = (int) Math.min(bubble.text().length(), Math.max(0L, (clientTick - bubble.startedAt()) / 2L));
        return bubble.text().substring(0, visible);
    }

    public static int color(int entityId) {
        Bubble bubble = BUBBLES.get(entityId);
        return bubble != null ? bubble.color() : SpeechBubbleColor.DEFAULT_OUTLINE;
    }

    public static float pitch(int entityId) {
        Bubble bubble = BUBBLES.get(entityId);
        return bubble != null ? bubble.pitch() : SpeechBubbleColor.DEFAULT_PITCH;
    }

    public static boolean isGesturing(int entityId) {
        Bubble bubble = BUBBLES.get(entityId);
        return bubble != null && clientTick - bubble.startedAt() <= bubble.text().length() * 2L + 28L;
    }

    public static java.util.Set<Integer> activeEntityIds() {
        return new java.util.HashSet<>(BUBBLES.keySet());
    }

    public static void clear() {
        BUBBLES.clear();
        clientTick = 0L;
    }

    private record Bubble(String text, int color, float pitch, long startedAt) { }
}
