package dev.onyxstudios.cca.api.v3.entity;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.server.level.ServerPlayer;

@FunctionalInterface
public interface PlayerCopyCallback {
    Event<PlayerCopyCallback> EVENT = EventFactory.createArrayBacked(PlayerCopyCallback.class, listeners -> (original, clone, lossless) -> {
        for (PlayerCopyCallback listener : listeners) {
            listener.copyData(original, clone, lossless);
        }
    });

    void copyData(ServerPlayer original, ServerPlayer clone, boolean lossless);
}
