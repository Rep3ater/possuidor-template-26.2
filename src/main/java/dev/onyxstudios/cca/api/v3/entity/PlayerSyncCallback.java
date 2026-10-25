package dev.onyxstudios.cca.api.v3.entity;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.server.level.ServerPlayer;

@FunctionalInterface
public interface PlayerSyncCallback {
    Event<PlayerSyncCallback> EVENT = EventFactory.createArrayBacked(PlayerSyncCallback.class, listeners -> player -> {
        for (PlayerSyncCallback listener : listeners) {
            listener.onPlayerSync(player);
        }
    });

    void onPlayerSync(ServerPlayer player);
}
