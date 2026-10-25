package net.fabricmc.fabric.api.event.player;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

@FunctionalInterface
public interface UseItemCallback {
    Event<UseItemCallback> EVENT = EventFactory.createArrayBacked(UseItemCallback.class,
        listeners -> (player, world, hand) -> {
            for (UseItemCallback listener : listeners) {
                InteractionResult result = listener.interact(player, world, hand);
                if (result != InteractionResult.PASS) return result;
            }
            return InteractionResult.PASS;
        }
    );

    InteractionResult interact(Player player, Level world, InteractionHand hand);
}
