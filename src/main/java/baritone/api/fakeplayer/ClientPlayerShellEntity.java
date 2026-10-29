package baritone.api.fakeplayer;

import baritone.api.IBaritone;
import com.mojang.authlib.GameProfile;
import ladysnake.requiem.common.entity.RequiemEntities;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.client.resources.PlayerSkin;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public class ClientPlayerShellEntity extends RemotePlayer implements AutomatoneFakePlayer {
    private static final Map<Integer, GameProfile> PENDING_PROFILES = new ConcurrentHashMap<>();

    protected GameProfile displayProfile;
    private final IBaritone baritone;
    private Supplier<PlayerSkin> skinLookup;
    private GameProfile lastProfile;

    public static void handleProfileSync(int entityId, GameProfile profile) {
        PENDING_PROFILES.put(entityId, profile);
        if (Minecraft.getInstance().level != null) {
            Entity entity = Minecraft.getInstance().level.getEntity(entityId);
            if (entity instanceof AutomatoneFakePlayer fakePlayer) {
                fakePlayer.setDisplayProfile(profile);
            }
        }
    }

    public ClientPlayerShellEntity(ClientLevel level, GameProfile profile) {
        super(level, profile);
        this.displayProfile = profile;
        this.baritone = new DummyBaritoneImpl(this);
        this.getEntityData().set(DATA_PLAYER_MODE_CUSTOMISATION, (byte) 0xFF);
    }

    @Override
    public void setId(int id) {
        super.setId(id);
        GameProfile pending = PENDING_PROFILES.remove(id);
        if (pending != null) {
            this.setDisplayProfile(pending);
        }
    }

    @Override
    public EntityType<?> getType() {
        return RequiemEntities.PLAYER_SHELL;
    }

    @Override
    public GameProfile getDisplayProfile() {
        return this.displayProfile != null ? this.displayProfile : this.getGameProfile();
    }

    @Override
    public void setDisplayProfile(GameProfile profile) {
        this.displayProfile = profile;
        this.skinLookup = null;
        this.lastProfile = null;
        this.getEntityData().set(DATA_PLAYER_MODE_CUSTOMISATION, (byte) 0xFF);
    }

    @Override
    public IBaritone getBaritone() {
        return this.baritone;
    }

    @Override
    public net.minecraft.network.chat.Component getName() {
        GameProfile profile = this.getDisplayProfile();
        if (profile != null && profile.name() != null && !profile.name().isEmpty() && !profile.name().startsWith("[") && !profile.name().equals("Mortal Shell")) {
            return net.minecraft.network.chat.Component.literal(profile.name() + "'s Shell");
        }
        return net.minecraft.network.chat.Component.translatable("entity.requiem.player_shell");
    }

    @Override
    public net.minecraft.network.chat.Component getDisplayName() {
        return this.getName();
    }

    @Override
    public PlayerSkin getSkin() {
        GameProfile profile = this.getDisplayProfile();
        if (profile != null) {
            if (this.skinLookup == null || this.lastProfile != profile) {
                this.lastProfile = profile;
                // Shells can use a profile copied from a body/impersonation that has the texture
                // property but not a fresh session signature (offline servers and imported bodies
                // are common examples). Requiring a secure signature silently fell back to Steve/Alex.
                this.skinLookup = Minecraft.getInstance().getSkinManager().lookupInsecure(profile);
            }
            return this.skinLookup.get();
        }
        return DefaultPlayerSkin.get(this.getUUID());
    }
}
