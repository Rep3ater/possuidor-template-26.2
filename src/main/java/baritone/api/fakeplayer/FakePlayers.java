package baritone.api.fakeplayer;

import com.mojang.authlib.GameProfile;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;

import java.util.UUID;

public final class FakePlayers {
    public static final ResourceLocation SPAWN_PACKET_ID = ResourceLocation.fromNamespaceAndPath("automatone", "spawn");
    public static final ResourceLocation PROFILE_UPDATE_PACKET_ID = ResourceLocation.fromNamespaceAndPath("automatone", "profile_update");

    public static GameProfile generateGameProfile() {
        return new GameProfile(UUID.randomUUID(), "Mortal Shell");
    }

    @SuppressWarnings("unchecked")
    public static <P extends Player & AutomatoneFakePlayer> EntityType.EntityFactory<P> entityFactory(ServerFakePlayerFactory<? extends P> factory) {
        return (type, level) -> {
            if (level instanceof ServerLevel sl) {
                return factory.create(type, sl);
            }
            return (P) ClientPlayerShellHelper.createClientShell(level);
        };
    }

    @FunctionalInterface
    public interface ServerFakePlayerFactory<P extends Player & AutomatoneFakePlayer> {
        P create(EntityType<? extends Player> type, ServerLevel level);
    }
}
