package baritone.api.fakeplayer;

import baritone.api.IBaritone;
import com.mojang.authlib.GameProfile;
import java.util.UUID;

public interface AutomatoneFakePlayer {
    GameProfile getDisplayProfile();
    void setDisplayProfile(GameProfile profile);
    default UUID getOwnerUuid() { return null; }
    default IBaritone getBaritone() {
        return IBaritone.KEY.maybeGet(this).orElse(null);
    }
}