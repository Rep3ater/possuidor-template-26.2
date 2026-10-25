package dev.onyxstudios.cca.api.v3.component.tick;

public interface CommonTickingComponent extends ClientTickingComponent, ServerTickingComponent, TickingComponent {
    @Override
    void tick();

    @Override
    default void clientTick() {
        this.tick();
    }

    @Override
    default void serverTick() {
        this.tick();
    }
}
