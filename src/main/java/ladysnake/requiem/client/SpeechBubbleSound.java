package ladysnake.requiem.client;

import ladysnake.requiem.common.sound.RequiemSoundEvents;
import net.minecraft.sounds.SoundEvent;

/**
 * Plays Animal Crossing (Animalese) synthesized phoneme chatter for speech bubbles.
 */
public final class SpeechBubbleSound {
    private SpeechBubbleSound() {}

    public static void play(char c) {
        try {
            SoundPlayer.playUI(c);
        } catch (Throwable ignored) {
            // Safe fallback in headless / unit-test environments
        }
    }

    public static char normalizePhoneme(char c) {
        char lower = Character.toLowerCase(c);
        return switch (lower) {
            case 'á', 'à', 'â', 'ä', 'ã', 'å' -> 'a';
            case 'é', 'è', 'ê', 'ë' -> 'e';
            case 'í', 'ì', 'î', 'ï' -> 'i';
            case 'ó', 'ò', 'ô', 'ö', 'õ' -> 'o';
            case 'ú', 'ù', 'û', 'ü' -> 'u';
            case 'ñ' -> 'n';
            case 'ç' -> 'c';
            default -> lower;
        };
    }

    public static float calculatePitch(char c) {
        char lower = normalizePhoneme(c);
        return switch (lower) {
            case 'a' -> 1.15F;
            case 'e' -> 1.35F;
            case 'i' -> 1.55F;
            case 'o' -> 1.05F;
            case 'u' -> 0.95F;
            case 'y' -> 1.40F;
            case '!', '?' -> 1.65F;
            case '.', ',' -> 0.85F;
            default -> {
                if (lower >= 'a' && lower <= 'z') {
                    yield 1.10F + ((lower - 'a') % 7) * 0.05F;
                } else if (Character.isDigit(lower)) {
                    yield 1.25F + ((lower - '0') % 5) * 0.04F;
                }
                yield 1.20F;
            }
        };
    }

    /** Inner class isolated from classloading in headless environments. */
    private static final class SoundPlayer {
        private static void playUI(char c) {
            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
            if (mc != null && mc.level != null && mc.getSoundManager() != null) {
                char phoneme = normalizePhoneme(c);
                SoundEvent event = null;
                if (phoneme >= 'a' && phoneme <= 'z') {
                    event = RequiemSoundEvents.ANIMALESE_LETTERS[phoneme - 'a'];
                }
                if (event == null) {
                    event = RequiemSoundEvents.ANIMALESE_GENERAL;
                }
                float pitch = calculatePitch(c);
                mc.getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(event, pitch, 0.42F));
            }
        }
    }
}
