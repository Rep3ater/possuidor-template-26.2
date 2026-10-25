package net.fabricmc.fabric.api.event.player;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

@FunctionalInterface
public interface AllowUseEntityCallback {
    Event<AllowUseEntityCallback> EVENT = EventFactory.createArrayBacked(AllowUseEntityCallback.class,
        listeners -> (player, world, hand, entity) -> {
            for (AllowUseEntityCallback listener : listeners) {
                if (!listener.allowUseEntity(player, world, hand, entity)) return false;
            }
            return true;
        }
    );

    boolean allowUseEntity(Player player, Level world, InteractionHand hand, Entity entity);
}
