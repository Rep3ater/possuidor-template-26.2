package ladysnake.requiem.common;

import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Palette mapping, pitch tag extraction (0.0 - 1.0), and bracket color parser for speech bubbles.
 */
public final class SpeechBubbleColor {
    public static final int DEFAULT_OUTLINE = 0xFF77D7E7;      // Classic cyan
    public static final int DEFAULT_BACKGROUND = 0xB91A1727;   // Classic dark translucent
    public static final int EMISSIVE_WHITE_TEXT = 0xFFFFFFFF;  // Emissive bright white
    public static final float DEFAULT_PITCH = 0.5F;            // Standard medium pitch on 0.0 - 1.0 scale

    public record Theme(int outlineColor, int backgroundColor) {}
    public record ParsedMessage(int outlineColor, float pitch, String text) {}

    private static final Pattern COLOR_PREFIX_PATTERN = Pattern.compile("^\\[([a-zA-Z_\\- ]+)\\]\\s*(.*)$");
    private static final Pattern PITCH_PATTERN = Pattern.compile(
        "(?i)[\\(\\[](?:pitch\\s*(?:from\\s*)?[:= ]?\\s*)?([0-2](?:\\.\\d+)?|\\.\\d+)[\\)\\]]"
    );
    private static final Pattern PITCH_LITERAL_RANGE_PATTERN = Pattern.compile(
        "(?i)[\\(\\[]pitch\\s*(?:from\\s*)?0[\\s\\-_/to]+1[\\)\\]]"
    );

    private static final Map<String, Theme> THEMES = new HashMap<>();

    static {
        // 16 Vanilla Minecraft dye colors with matching dark tinted bubble backgrounds
        registerTheme("white", 0xFFFFFFFF, 0xCC2B2D32);
        registerTheme("orange", 0xFFF9801D, 0xCC3D200E);
        registerTheme("magenta", 0xFFC74EBD, 0xCC391436);
        registerTheme("light_blue", 0xFF3AB3DA, 0xCC112938);
        registerTheme("yellow", 0xFFFED83D, 0xCC3B320C);
        registerTheme("lime", 0xFF80C71F, 0xCC1E330D);
        registerTheme("pink", 0xFFF38BAA, 0xCC3B1D27);
        registerTheme("gray", 0xFF585F63, 0xCC181C1E);
        registerTheme("light_gray", 0xFF9D9D97, 0xCC262829);
        registerTheme("cyan", 0xFF169C9C, 0xCC0E2A2E);
        registerTheme("purple", 0xFF8932B8, 0xCC261036);
        registerTheme("blue", 0xFF3C44AA, 0xCC111438);
        registerTheme("brown", 0xFF835432, 0xCC281A10);
        registerTheme("green", 0xFF5E7C16, 0xCC18230B);
        registerTheme("red", 0xFFC92828, 0xCC381111);
        registerTheme("black", 0xFF2A2A32, 0xCC111114);

        // Aliases
        alias("light_blue", "light blue", "lightblue", "light-blue");
        alias("light_gray", "light gray", "lightgray", "light_grey", "light grey", "lightgrey", "silver");
        alias("gray", "grey");
    }

    private SpeechBubbleColor() {}

    private static void registerTheme(String name, int outline, int background) {
        THEMES.put(name.toLowerCase(Locale.ROOT), new Theme(outline, background));
    }

    private static void alias(String canonical, String... aliases) {
        Theme canonicalTheme = THEMES.get(canonical.toLowerCase(Locale.ROOT));
        if (canonicalTheme != null) {
            for (String alias : aliases) {
                THEMES.put(alias.toLowerCase(Locale.ROOT), canonicalTheme);
            }
        }
    }

    public static Theme getTheme(int outlineColor) {
        for (Theme theme : THEMES.values()) {
            if (theme.outlineColor() == outlineColor) {
                return theme;
            }
        }
        if (outlineColor == DEFAULT_OUTLINE) {
            return new Theme(DEFAULT_OUTLINE, DEFAULT_BACKGROUND);
        }
        // Fallback: compute matching dark translucent background from outline color
        int r = (outlineColor >> 16) & 0xFF;
        int g = (outlineColor >> 8) & 0xFF;
        int b = outlineColor & 0xFF;
        int bgR = Math.min(255, (r * 3) / 16 + 10);
        int bgG = Math.min(255, (g * 3) / 16 + 10);
        int bgB = Math.min(255, (b * 3) / 16 + 14);
        int bgAlpha = 0xCC;
        int bg = (bgAlpha << 24) | (bgR << 16) | (bgG << 8) | bgB;
        return new Theme(outlineColor, bg);
    }

    /**
     * Parses a chat message:
     * 1. Strips any pitch regulation tag e.g. "(pitch 0.8)", "(0.8)", "(pitch from 0-1)" and extracts pitch factor [0.0 - 1.0].
     * 2. Checks for a starting bracketed Minecraft dye color e.g. "[red]", extracts color, and strips tag.
     * 3. Returns the cleaned message text free of tags, along with the color and pitch factor.
     */
    public static ParsedMessage parseMessage(String input) {
        if (input == null || input.isBlank()) {
            return new ParsedMessage(DEFAULT_OUTLINE, DEFAULT_PITCH, input == null ? "" : input);
        }

        String workingText = input;
        float pitch = DEFAULT_PITCH;

        // 1. Check for literal "pitch from 0-1" in parens/brackets
        Matcher rangeMatcher = PITCH_LITERAL_RANGE_PATTERN.matcher(workingText);
        if (rangeMatcher.find()) {
            workingText = rangeMatcher.replaceAll(" ");
            pitch = DEFAULT_PITCH;
        }

        // 2. Check for "(pitch 0.8)", "(0.8)", "(pitch: 0.5)" in parens/brackets
        Matcher pitchMatcher = PITCH_PATTERN.matcher(workingText);
        if (pitchMatcher.find()) {
            try {
                float val = Float.parseFloat(pitchMatcher.group(1));
                pitch = Math.max(0.0F, Math.min(1.0F, val));
            } catch (NumberFormatException ignored) {
            }
            workingText = pitchMatcher.replaceAll(" ");
        }

        workingText = workingText.trim();
        int outlineColor = DEFAULT_OUTLINE;

        // 3. Check for "[red]" dye color at start of message
        Matcher colorMatcher = COLOR_PREFIX_PATTERN.matcher(workingText);
        if (colorMatcher.matches()) {
            String colorTag = colorMatcher.group(1).trim().toLowerCase(Locale.ROOT);
            Theme theme = THEMES.get(colorTag);
            if (theme == null) {
                theme = THEMES.get(colorTag.replace(' ', '_').replace('-', '_'));
            }
            if (theme != null) {
                outlineColor = theme.outlineColor();
                workingText = colorMatcher.group(2).trim();
            }
        }

        // Clean up redundant internal spaces left by tag removal
        workingText = workingText.replaceAll("\\s{2,}", " ").trim();
        return new ParsedMessage(outlineColor, pitch, workingText);
    }

    public static Map<String, Theme> getAllThemes() {
        return Collections.unmodifiableMap(THEMES);
    }
}
