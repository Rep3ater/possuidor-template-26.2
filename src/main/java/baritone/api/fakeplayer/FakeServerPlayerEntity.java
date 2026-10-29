package baritone.api.fakeplayer;

import baritone.api.IBaritone;
import com.mojang.authlib.GameProfile;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.common.util.FakePlayer;

import java.util.UUID;

public class FakeServerPlayerEntity extends FakePlayer implements AutomatoneFakePlayer {
    private final EntityType<? extends Player> entityType;
    protected GameProfile displayProfile;
    private final IBaritone baritone;

    public FakeServerPlayerEntity(EntityType<? extends Player> type, ServerLevel world) {
        this(type, world, new GameProfile(UUID.randomUUID(), "Mortal Shell"));
    }

    public FakeServerPlayerEntity(EntityType<? extends Player> type, ServerLevel world, GameProfile profile) {
        super(world, profile);
        this.entityType = type;
        this.displayProfile = profile;
        this.baritone = new DummyBaritoneImpl(this);
    }

    @Override
    public EntityType<?> getType() {
        return this.entityType != null ? this.entityType : super.getType();
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket(ServerEntity serverEntity) {
        return new ClientboundAddEntityPacket(this, serverEntity);
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

    public void selectHotbarSlot(int slot) {
        this.getInventory().selected = slot;
    }

    public void swapHands() {
        // no-op
    }

    public void releaseActiveItem() {
        this.stopUsingItem();
    }
}
