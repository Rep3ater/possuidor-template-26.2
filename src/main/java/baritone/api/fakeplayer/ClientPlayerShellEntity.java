package baritone.api.fakeplayer;

import baritone.api.IBaritone;
import com.mojang.authlib.GameProfile;
import ladysnake.requiem.common.entity.RequiemEntities;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.PlayerSkin;

public class ClientPlayerShellEntity extends RemotePlayer implements AutomatoneFakePlayer {
    protected GameProfile displayProfile;
    private final IBaritone baritone;

    public ClientPlayerShellEntity(ClientLevel level, GameProfile profile) {
        super(level, profile);
        this.displayProfile = profile;
        this.baritone = new DummyBaritoneImpl(this);
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
            return Minecraft.getInstance().getSkinManager().createLookup(profile, false).get();
        }
        return DefaultPlayerSkin.get(this.getUUID());
    }
}
