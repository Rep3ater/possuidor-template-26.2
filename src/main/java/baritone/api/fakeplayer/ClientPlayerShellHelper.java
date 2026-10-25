package baritone.api.fakeplayer;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public final class ClientPlayerShellHelper {
    public static Player createClientShell(Level level) {
        return new ClientPlayerShellEntity((ClientLevel) level, FakePlayers.generateGameProfile());
    }
}
