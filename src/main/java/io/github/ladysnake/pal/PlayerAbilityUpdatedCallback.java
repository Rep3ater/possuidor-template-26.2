package io.github.ladysnake.pal;

import net.fabricmc.fabric.api.event.Event;
import net.minecraft.world.entity.player.Player;

@FunctionalInterface
public interface PlayerAbilityUpdatedCallback {
    static Event<PlayerAbilityUpdatedCallback> event(PlayerAbility ability) {
        return ability.getUpdatedEvent();
    }

    void onAbilityUpdated(Player player, boolean nowEnabled);
}
