package ladysnake.requiem.client.network;

import baritone.api.fakeplayer.ClientPlayerShellEntity;
import ladysnake.requiem.common.network.RequiemNetworking;
import ladysnake.requiem.core.RequiemCoreNetworking;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * Physical-client-only packet handlers. Keeping UI and client-shell references in this
 * class prevents the dedicated server from resolving Screen or other client classes.
 */
public final class ClientMessageHandling {
    private ClientMessageHandling() {}

    public static void registerPayloadHandlers(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1.0.0");
        registrar.playToClient(RequiemNetworking.SpeechBubblePayload.TYPE, RequiemNetworking.SpeechBubblePayload.STREAM_CODEC,
            (payload, context) -> context.enqueueWork(() -> {
                Minecraft minecraft = Minecraft.getInstance();
                boolean entityLoaded = minecraft.level != null && minecraft.level.getEntity(payload.entityId()) != null;
                ladysnake.requiem.Requiem.LOGGER.info(
                    "[Requiem] Client received speech bubble packet: entity={}, loadedLocally={}, text='{}'",
                    payload.entityId(), entityLoaded, payload.text());
                ladysnake.requiem.client.SpeechBubbleClientState.show(payload.entityId(), payload.text());
            })));
        registrar.playToClient(RequiemNetworking.AnchorDamagePayload.TYPE, RequiemNetworking.AnchorDamagePayload.STREAM_CODEC, (payload, context) -> {});
        registrar.playToClient(RequiemNetworking.DataSyncPayload.TYPE, RequiemNetworking.DataSyncPayload.STREAM_CODEC, (payload, context) -> {});
        registrar.playToClient(RequiemNetworking.OpusUsePayload.TYPE, RequiemNetworking.OpusUsePayload.STREAM_CODEC, (payload, context) -> {});
        registrar.playToClient(RequiemNetworking.EtherealAnimationPayload.TYPE, RequiemNetworking.EtherealAnimationPayload.STREAM_CODEC, (payload, context) -> {});
        registrar.playToClient(RequiemNetworking.BodyCurePayload.TYPE, RequiemNetworking.BodyCurePayload.STREAM_CODEC, (payload, context) -> {});
        registrar.playToClient(RequiemNetworking.ObeliskPowerUpdatePayload.TYPE, RequiemNetworking.ObeliskPowerUpdatePayload.STREAM_CODEC, (payload, context) -> {});
        registrar.playToClient(RequiemNetworking.RiftWitnessedPayload.TYPE, RequiemNetworking.RiftWitnessedPayload.STREAM_CODEC, (payload, context) -> {});
        registrar.playToClient(RequiemNetworking.OpenDialoguePayload.TYPE, RequiemNetworking.OpenDialoguePayload.STREAM_CODEC, ClientMessageHandling::handleOpenDialogue);
        registrar.playToClient(RequiemNetworking.ShellProfileSyncPayload.TYPE, RequiemNetworking.ShellProfileSyncPayload.STREAM_CODEC, ClientMessageHandling::handleShellProfileSync);
        registrar.playToClient(RequiemNetworking.LockiStatePayload.TYPE, RequiemNetworking.LockiStatePayload.STREAM_CODEC,
            (payload, context) -> context.enqueueWork(() -> ladysnake.requiem.client.locki.LockiClientState.accept(payload)));
        registrar.playToClient(RequiemNetworking.HomunculusBrainOpenPayload.TYPE, RequiemNetworking.HomunculusBrainOpenPayload.STREAM_CODEC, ClientMessageHandling::handleHomunculusBrainOpen);
        registrar.playToClient(RequiemNetworking.ParmenidesStatePayload.TYPE, RequiemNetworking.ParmenidesStatePayload.STREAM_CODEC,
            (payload, context) -> context.enqueueWork(() -> ladysnake.requiem.client.ParmenidesClientState.update(payload.entityId(), payload.active())));
        registrar.playToClient(RequiemNetworking.OutOfBodyBodyPayload.TYPE, RequiemNetworking.OutOfBodyBodyPayload.STREAM_CODEC,
            (payload, context) -> context.enqueueWork(() -> ladysnake.requiem.client.ParmenidesClientState.updateBody(payload.entityId(), payload.active())));
        registrar.playToClient(RequiemNetworking.ParmenidesPreviewDataPayload.TYPE, RequiemNetworking.ParmenidesPreviewDataPayload.STREAM_CODEC,
            (payload, context) -> context.enqueueWork(() -> ladysnake.requiem.client.ParmenidesClientState.acceptPreview(
                payload.chunkX(), payload.chunkZ(), payload.playerInNether(), payload.minY(), payload.height(), payload.states())));
    }

    private static void handleOpenDialogue(RequiemNetworking.OpenDialoguePayload payload, net.neoforged.neoforge.network.handling.IPayloadContext context) {
        context.enqueueWork(() -> Minecraft.getInstance().setScreen(new ladysnake.requiem.client.gui.RemnantChoiceScreen()));
    }

    private static void handleShellProfileSync(RequiemNetworking.ShellProfileSyncPayload payload, net.neoforged.neoforge.network.handling.IPayloadContext context) {
        context.enqueueWork(() -> ClientPlayerShellEntity.handleProfileSync(payload.entityId(), payload.profile()));
    }

    private static void handleHomunculusBrainOpen(RequiemNetworking.HomunculusBrainOpenPayload payload, net.neoforged.neoforge.network.handling.IPayloadContext context) {
        context.enqueueWork(() -> Minecraft.getInstance().setScreen(
            new ladysnake.requiem.client.gui.HomunculusBrainScreen(payload.entityId(), payload.program())));
    }
}
