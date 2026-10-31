package ladysnake.requiem.client;

import java.util.HashMap;
import java.util.Map;

/** Client-only state for short-lived, typewritten Requiem speech bubbles. */
public final class SpeechBubbleClientState {
    private static final Map<Integer, Bubble> BUBBLES = new HashMap<>();
    private static long clientTick;
    private SpeechBubbleClientState() { }

    public static void show(int entityId, String originalText) {
        if (originalText == null || originalText.isBlank()) return;
        String text = originalText.length() > 256 ? originalText.substring(0, 256) : originalText;
        BUBBLES.put(entityId, new Bubble(text, clientTick));
    }

    public static void tick() {
        clientTick++;
        BUBBLES.entrySet().removeIf(entry ->
            clientTick - entry.getValue().startedAt() > entry.getValue().text().length() * 2L + 52L);
    }

    public static String visibleText(int entityId) {
        Bubble bubble = BUBBLES.get(entityId);
        if (bubble == null) return "";
        int visible = (int) Math.min(bubble.text().length(), Math.max(0L, (clientTick - bubble.startedAt()) / 2L));
        return bubble.text().substring(0, visible);
    }

    public static boolean isGesturing(int entityId) {
        Bubble bubble = BUBBLES.get(entityId);
        return bubble != null && clientTick - bubble.startedAt() <= bubble.text().length() * 2L + 28L;
    }

    public static void clear() {
        BUBBLES.clear();
        clientTick = 0L;
    }

    private record Bubble(String text, long startedAt) { }
}
