package net.fabricmc.fabric.api.event;

import net.minecraft.resources.Identifier;

public abstract class Event<T> {
    public static final Identifier DEFAULT_PHASE = Identifier.fromNamespaceAndPath("fabric", "default");

    protected volatile T invoker;

    public final T invoker() {
        return this.invoker;
    }

    public abstract void register(T listener);

    public void register(Identifier phase, T listener) {
        this.register(listener);
    }

    public void addPhaseOrdering(Identifier firstPhase, Identifier secondPhase) {
        // Implemented by phased ArrayBackedEvent
    }
}
