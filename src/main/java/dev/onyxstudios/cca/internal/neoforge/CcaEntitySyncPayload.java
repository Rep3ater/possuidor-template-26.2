package dev.onyxstudios.cca.internal.neoforge;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record CcaEntitySyncPayload(int entityId, Identifier componentId, byte[] data) implements CustomPacketPayload {
    public static final Type<CcaEntitySyncPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath("requiem", "cca_entity_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, CcaEntitySyncPayload> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_INT,
        CcaEntitySyncPayload::entityId,
        Identifier.STREAM_CODEC,
        CcaEntitySyncPayload::componentId,
        ByteBufCodecs.BYTE_ARRAY,
        CcaEntitySyncPayload::data,
        CcaEntitySyncPayload::new
    );

    @Override
    public Type<CcaEntitySyncPayload> type() {
        return TYPE;
    }
}
