package ladysnake.requiem.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;

/**
 * Plays Animal Crossing style gibberish chatter sound effects for speech bubbles.
 */
public final class SpeechBubbleSound {
    private SpeechBubbleSound() {}

    public static void play(char c) {
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc != null && mc.level != null && mc.getSoundManager() != null) {
                float pitch = calculatePitch(c);
                mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_BIT.value(), pitch, 0.32F));
            }
        } catch (Throwable ignored) {
            // Safe fallback in headless / unit-test environments
        }
    }

    public static float calculatePitch(char c) {
        char lower = Character.toLowerCase(c);
        return switch (lower) {
            case 'a' -> 1.25F;
            case 'e' -> 1.45F;
            case 'i' -> 1.65F;
            case 'o' -> 1.15F;
            case 'u' -> 1.05F;
            case '!', '?' -> 1.85F;
            case '.', ',' -> 0.95F;
            default -> 1.20F + ((lower * 23) % 9) * 0.05F;
        };
    }
}
