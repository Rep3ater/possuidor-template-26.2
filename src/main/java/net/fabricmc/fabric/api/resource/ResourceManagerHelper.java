package net.fabricmc.fabric.api.resource;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddReloadListenerEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Fabric API shim: registers server data reload listeners through NeoForge's {@link AddReloadListenerEvent}.
 * {@link #init()} must be called during mod construction so the event listener is present before the first reload.
 */
public final class ResourceManagerHelper {
    private static final List<PreparableReloadListener> SERVER_LISTENERS = new ArrayList<>();
    private static final ResourceManagerHelper SERVER_HELPER = new ResourceManagerHelper(PackType.SERVER_DATA);
    private static final ResourceManagerHelper CLIENT_HELPER = new ResourceManagerHelper(PackType.CLIENT_RESOURCES);
    private static boolean initialized;

    public static synchronized void init() {
        if (initialized) return;
        initialized = true;
        NeoForge.EVENT_BUS.addListener(AddReloadListenerEvent.class, event -> {
            for (PreparableReloadListener listener : SERVER_LISTENERS) {
                event.addListener(keyOf(listener), listener);
            }
        });
    }

    private static ResourceLocation keyOf(PreparableReloadListener listener) {
        if (listener instanceof SimpleResourceReloadListener<?> simple) {
            return simple.getFabricId();
        }
        return ResourceLocation.fromNamespaceAndPath("requiem", listener.getClass().getSimpleName().toLowerCase(java.util.Locale.ROOT));
    }

    private final PackType type;

    private ResourceManagerHelper(PackType type) {
        this.type = type;
    }

    public static ResourceManagerHelper get(PackType type) {
        init();
        return type == PackType.SERVER_DATA ? SERVER_HELPER : CLIENT_HELPER;
    }

    public void registerReloadListener(PreparableReloadListener listener) {
        if (this.type == PackType.SERVER_DATA) {
            SERVER_LISTENERS.add(listener);
        }
    }
}
