package ladysnake.requiem.common.sharedbody;

import ladysnake.requiem.Requiem;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.util.HashSet;
import java.util.List;

/**
 * Payloads used by the Shared Body mechanic.
 *
 * <ul>
 *     <li>{@link InputPayload} (client to server): held state of the attack and use keys, which the server cannot otherwise know.</li>
 *     <li>{@link SyncPayload} (server to client): ids of the mobs that are currently being driven by the receiving player.</li>
 * </ul>
 */
public final class SharedBodyNetworking {
    public static final Identifier INPUT = Requiem.id("shared_body_input");
    public static final Identifier SYNC = Requiem.id("shared_body_sync");

    private SharedBodyNetworking() {}

    public static void register(PayloadRegistrar registrar) {
        registrar.playToServer(InputPayload.TYPE, InputPayload.STREAM_CODEC, SharedBodyNetworking::handleInput);
        registrar.playToClient(SyncPayload.TYPE, SyncPayload.STREAM_CODEC, SharedBodyNetworking::handleSync);
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        register(event.registrar("1.0.0"));
    }

    private static void handleInput(InputPayload payload, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player) {
            SharedBodyManager.setHeldKeys(player, payload.attack(), payload.use());
        }
    }

    private static void handleSync(SyncPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> SharedBodyClientState.setControlled(new HashSet<>(payload.entityIds())));
    }

    public static void sendSync(ServerPlayer player, List<Integer> entityIds) {
        PacketDistributor.sendToPlayer(player, new SyncPayload(entityIds));
    }

    public record InputPayload(boolean attack, boolean use) implements CustomPacketPayload {
        public static final Type<InputPayload> TYPE = new Type<>(INPUT);
        public static final StreamCodec<RegistryFriendlyByteBuf, InputPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, InputPayload::attack,
            ByteBufCodecs.BOOL, InputPayload::use,
            InputPayload::new
        );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record SyncPayload(List<Integer> entityIds) implements CustomPacketPayload {
        public static final Type<SyncPayload> TYPE = new Type<>(SYNC);
        public static final StreamCodec<RegistryFriendlyByteBuf, SyncPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list()), SyncPayload::entityIds,
            SyncPayload::new
        );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }
}
