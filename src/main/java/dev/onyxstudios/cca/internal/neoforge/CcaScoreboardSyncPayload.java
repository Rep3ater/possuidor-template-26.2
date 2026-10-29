package dev.onyxstudios.cca.internal.neoforge;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record CcaScoreboardSyncPayload(ResourceLocation componentId, byte[] data) implements CustomPacketPayload {
    public static final Type<CcaScoreboardSyncPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("requiem", "cca_scoreboard_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, CcaScoreboardSyncPayload> STREAM_CODEC = StreamCodec.composite(
        ResourceLocation.STREAM_CODEC,
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
