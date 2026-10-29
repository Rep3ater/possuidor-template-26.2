package ladysnake.requiem.client.network;

import baritone.api.fakeplayer.ClientPlayerShellEntity;
import ladysnake.requiem.common.network.RequiemNetworking;
import ladysnake.requiem.core.RequiemCoreNetworking;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.client.network.RegisterClientPayloadHandlersEvent;

/**
 * Physical-client-only packet handlers. Keeping UI and client-shell references in this
 * class prevents the dedicated server from resolving Screen or other client classes.
 */
public final class ClientMessageHandling {
    private ClientMessageHandling() {}

    public static void registerPayloadHandlers(RegisterClientPayloadHandlersEvent event) {
        event.register(RequiemNetworking.AnchorDamagePayload.TYPE, (payload, context) -> {});
        event.register(RequiemNetworking.DataSyncPayload.TYPE, (payload, context) -> {});
        event.register(RequiemNetworking.OpusUsePayload.TYPE, (payload, context) -> {});
        event.register(RequiemNetworking.EtherealAnimationPayload.TYPE, (payload, context) -> {});
        event.register(RequiemNetworking.BodyCurePayload.TYPE, (payload, context) -> {});
        event.register(RequiemNetworking.ObeliskPowerUpdatePayload.TYPE, (payload, context) -> {});
        event.register(RequiemNetworking.RiftWitnessedPayload.TYPE, (payload, context) -> {});
        event.register(RequiemNetworking.OpenDialoguePayload.TYPE, ClientMessageHandling::handleOpenDialogue);
        event.register(RequiemNetworking.ShellProfileSyncPayload.TYPE, ClientMessageHandling::handleShellProfileSync);
        event.register(RequiemNetworking.LockiStatePayload.TYPE,
            (payload, context) -> context.enqueueWork(() -> ladysnake.requiem.client.locki.LockiClientState.accept(payload)));
        event.register(RequiemNetworking.HomunculusBrainOpenPayload.TYPE, ClientMessageHandling::handleHomunculusBrainOpen);
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
