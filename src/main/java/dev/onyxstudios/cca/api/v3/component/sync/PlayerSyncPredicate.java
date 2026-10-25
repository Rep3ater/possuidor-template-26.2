package dev.onyxstudios.cca.api.v3.component.sync;

import net.minecraft.server.level.ServerPlayer;

@FunctionalInterface
public interface PlayerSyncPredicate {
    boolean shouldSyncWith(ServerPlayer player);
}
