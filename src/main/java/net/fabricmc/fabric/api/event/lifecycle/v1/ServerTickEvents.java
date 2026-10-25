package net.fabricmc.fabric.api.event.lifecycle.v1;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.server.MinecraftServer;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

public final class ServerTickEvents {
    private ServerTickEvents() {}

    public static final Event<StartTick> START_SERVER_TICK = EventFactory.createArrayBacked(StartTick.class, callbacks -> server -> {
        for (StartTick callback : callbacks) {
            callback.onStartTick(server);
        }
    });

    public static final Event<EndTick> END_SERVER_TICK = EventFactory.createArrayBacked(EndTick.class, callbacks -> server -> {
        for (EndTick callback : callbacks) {
            callback.onEndTick(server);
        }
    });

    static {
        NeoForge.EVENT_BUS.addListener((ServerTickEvent.Pre event) -> START_SERVER_TICK.invoker().onStartTick(event.getServer()));
        NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post event) -> END_SERVER_TICK.invoker().onEndTick(event.getServer()));
    }

    @FunctionalInterface
    public interface StartTick {
        void onStartTick(MinecraftServer server);
    }

    @FunctionalInterface
    public interface EndTick {
        void onEndTick(MinecraftServer server);
    }
}
