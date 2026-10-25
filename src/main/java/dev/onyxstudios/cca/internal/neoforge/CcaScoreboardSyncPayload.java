package dev.onyxstudios.cca.internal.neoforge;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record CcaScoreboardSyncPayload(Identifier componentId, byte[] data) implements CustomPacketPayload {
    public static final Type<CcaScoreboardSyncPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath("requiem", "cca_scoreboard_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, CcaScoreboardSyncPayload> STREAM_CODEC = StreamCodec.composite(
        Identifier.STREAM_CODEC,
        CcaScoreboardSyncPayload::componentId,
        ByteBufCodecs.BYTE_ARRAY,
        CcaScoreboardSyncPayload::data,
        CcaScoreboardSyncPayload::new
    );

    @Override
    public Type<CcaScoreboardSyncPayload> type() {
        return TYPE;
    }
}
