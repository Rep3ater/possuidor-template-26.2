package ladysnake.requiem.common;

import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Palette mapping and bracket color parser for Minecraft dye-colored speech bubbles.
 */
public final class SpeechBubbleColor {
    public static final int DEFAULT_OUTLINE = 0xFF77D7E7;      // Classic cyan
    public static final int DEFAULT_BACKGROUND = 0xB91A1727;   // Classic dark translucent
    public static final int EMISSIVE_WHITE_TEXT = 0xFFFFFFFF;  // Emissive bright white

    public record Theme(int outlineColor, int backgroundColor) {}
    public record ParsedMessage(int outlineColor, String text) {}

    private static final Pattern COLOR_PREFIX_PATTERN = Pattern.compile("^\\[([a-zA-Z_\\- ]+)\\]\\s*(.*)$");
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
     * Parses a chat message starting with a bracketed Minecraft dye color (e.g. "[red] Hello").
     * If matched, the bracket tag is stripped from the text and the corresponding dye color is returned.
     * If no color prefix matches, the default outline color and untouched text are returned.
     */
    public static ParsedMessage parseMessage(String input) {
        if (input == null || input.isBlank()) {
            return new ParsedMessage(DEFAULT_OUTLINE, input == null ? "" : input);
        }

        Matcher matcher = COLOR_PREFIX_PATTERN.matcher(input);
        if (matcher.matches()) {
            String colorTag = matcher.group(1).trim().toLowerCase(Locale.ROOT);
            Theme theme = THEMES.get(colorTag);
            if (theme == null) {
                // Try normalizing spaces/hyphens to underscore
                theme = THEMES.get(colorTag.replace(' ', '_').replace('-', '_'));
            }
            if (theme != null) {
                String cleanText = matcher.group(2).trim();
                return new ParsedMessage(theme.outlineColor(), cleanText);
            }
        }

        return new ParsedMessage(DEFAULT_OUTLINE, input);
    }

    public static Map<String, Theme> getAllThemes() {
        return Collections.unmodifiableMap(THEMES);
    }
}
