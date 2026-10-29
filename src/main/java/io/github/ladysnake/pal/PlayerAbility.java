package io.github.ladysnake.pal;

import io.github.ladysnake.pal.impl.PalInternals;
import java.util.Objects;
import java.util.function.BiFunction;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

public final class PlayerAbility {
    private final ResourceLocation id;
    private final BiFunction<PlayerAbility, Player, AbilityTracker> trackerFactory;
    private final Event<PlayerAbilityUpdatedCallback> updatedEvent = EventFactory.createArrayBacked(
        PlayerAbilityUpdatedCallback.class,
        listeners -> (player, nowEnabled) -> {
            for (PlayerAbilityUpdatedCallback listener : listeners) {
                listener.onAbilityUpdated(player, nowEnabled);
            }
        }
    );

    public PlayerAbility(ResourceLocation id, BiFunction<PlayerAbility, Player, AbilityTracker> trackerFactory) {
        this.id = Objects.requireNonNull(id, "id");
        this.trackerFactory = Objects.requireNonNull(trackerFactory, "trackerFactory");
    }

    public ResourceLocation getId() {
        return this.id;
    }

    public AbilityTracker createTracker(Player player) {
        return this.trackerFactory.apply(this, player);
    }

    public AbilityTracker getTracker(Player player) {
        return PalInternals.getTracker(player, this);
    }

    public boolean isEnabledFor(Player player) {
        return this.getTracker(player).isEnabled();
    }

    public Event<PlayerAbilityUpdatedCallback> getUpdatedEvent() {
        return this.updatedEvent;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PlayerAbility that)) return false;
        return this.id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return this.id.hashCode();
    }

    @Override
    public String toString() {
        return "PlayerAbility[" + this.id + "]";
    }
}
