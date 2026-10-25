package dev.onyxstudios.cca.api.v3.entity;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

@FunctionalInterface
public interface TrackingStartCallback {
    Event<TrackingStartCallback> EVENT = EventFactory.createArrayBacked(TrackingStartCallback.class, listeners -> (player, tracked) -> {
        for (TrackingStartCallback listener : listeners) {
            listener.onPlayerStartTracking(player, tracked);
        }
    });

    void onPlayerStartTracking(ServerPlayer player, Entity tracked);
}
