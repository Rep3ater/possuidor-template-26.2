package ladysnake.requiem.client;

import ladysnake.requiem.common.network.RequiemNetworking;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Locale;
import java.util.UUID;

/** Reports the selected Minecraft language so the server can localize speech bubbles per viewer. */
public final class SpeechLanguageSync {
    private static UUID lastPlayer;
    private static String lastLanguage;

    private SpeechLanguageSync() { }

    public static void tick(Minecraft minecraft) {
        if (minecraft.player == null || minecraft.getConnection() == null) return;

        String selected = minecraft.getLanguageManager().getSelected();
        String language = selected == null ? "en" : selected.trim().toLowerCase(Locale.ROOT).replace('_', '-');
        int region = language.indexOf('-');
        if (region >= 0) language = language.substring(0, region);
        if (!language.matches("[a-z]{2,3}")) language = "en";

        UUID playerId = minecraft.player.getUUID();
        if (!playerId.equals(lastPlayer) || !language.equals(lastLanguage)) {
            lastPlayer = playerId;
            lastLanguage = language;
            PacketDistributor.sendToServer(new RequiemNetworking.SpeechLanguagePayload(language));
        }
    }

    public static void reset() {
        lastPlayer = null;
        lastLanguage = null;
    }
}