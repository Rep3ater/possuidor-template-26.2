package dev.onyxstudios.cca.api.v3.component.sync;

import dev.onyxstudios.cca.api.v3.component.Component;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

public interface AutoSyncedComponent extends Component, ComponentPacketWriter, PlayerSyncPredicate {
    @Override
    default boolean shouldSyncWith(ServerPlayer player) {
        return true;
    }

    @Override
    void writeSyncPacket(RegistryFriendlyByteBuf buf, ServerPlayer recipient);

    void applySyncPacket(RegistryFriendlyByteBuf buf);
}
