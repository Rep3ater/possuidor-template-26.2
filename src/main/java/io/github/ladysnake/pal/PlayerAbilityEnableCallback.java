package io.github.ladysnake.pal;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.world.entity.player.Player;

@FunctionalInterface
public interface PlayerAbilityEnableCallback {
    Event<PlayerAbilityEnableCallback> EVENT = EventFactory.createArrayBacked(
        PlayerAbilityEnableCallback.class,
        listeners -> (player, ability, abilitySource) -> {
            for (PlayerAbilityEnableCallback listener : listeners) {
                if (!listener.allow(player, ability, abilitySource)) {
                    return false;
                }
            }
            return true;
        }
    );

    boolean allow(Player player, PlayerAbility ability, AbilitySource abilitySource);
}
