package ladysnake.requiem.client;

import ladysnake.requiem.api.v1.possession.Possessable;
import net.minecraft.client.Minecraft;

public final class ClientPossessionHelper {
    public static boolean isLocalPlayerPossessor(Possessable possessable) {
        try {
            Minecraft mc = Minecraft.getInstance();
            return mc.player != null && possessable.getPossessor() == mc.player;
        } catch (Throwable t) {
            return false;
        }
    }
}
