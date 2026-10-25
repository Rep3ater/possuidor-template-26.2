package dev.onyxstudios.cca.api.v3.component.sync;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

@FunctionalInterface
public interface ComponentPacketWriter {
    void writeSyncPacket(RegistryFriendlyByteBuf buf, ServerPlayer recipient);
}
