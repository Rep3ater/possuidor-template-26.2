package io.github.ladysnake.locki;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.world.entity.player.Player;

@FunctionalInterface
public interface InventoryLockingChangeCallback {
    Event<InventoryLockingChangeCallback> EVENT = EventFactory.createArrayBacked(
        InventoryLockingChangeCallback.class,
        callbacks -> (player, part, locked) -> {
            for (InventoryLockingChangeCallback callback : callbacks) {
                callback.onLockingStateChanged(player, part, locked);
            }
        }
    );

    void onLockingStateChanged(Player player, InventoryNode part, boolean locked);
}
