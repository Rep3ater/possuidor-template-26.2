package net.fabricmc.fabric.api.event;

import net.minecraft.resources.ResourceLocation;

public abstract class Event<T> {
    public static final ResourceLocation DEFAULT_PHASE = ResourceLocation.fromNamespaceAndPath("fabric", "default");

    protected volatile T invoker;

    public final T invoker() {
        return this.invoker;
    }

    public abstract void register(T listener);

    public void register(ResourceLocation phase, T listener) {
        this.register(listener);
    }

    public void addPhaseOrdering(ResourceLocation firstPhase, ResourceLocation secondPhase) {
        // Implemented by phased ArrayBackedEvent
    }
}
