package io.github.ladysnake.pal;

import io.github.ladysnake.pal.impl.PalInternals;
import java.util.Objects;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;

public final class AbilitySource implements Comparable<AbilitySource> {
    public static final int FREE = 1000;
    public static final int RENEWABLE = 2000;
    public static final int DEFAULT = 0;
    public static final int CONSUMABLE = -1000;

    private final Identifier id;
    private final int priority;

    public AbilitySource(Identifier id, int priority) {
        this.id = Objects.requireNonNull(id, "id");
        this.priority = priority;
    }

    public Identifier getId() {
        return this.id;
    }

    public int getPriority() {
        return this.priority;
    }

    public void grantTo(Player player, PlayerAbility ability) {
        if (!player.level().isClientSide()) {
            ability.getTracker(player).addSource(this);
        }
    }

    public void revokeFrom(Player player, PlayerAbility ability) {
        if (!player.level().isClientSide()) {
            ability.getTracker(player).removeSource(this);
        }
    }

    public boolean grants(Player player, PlayerAbility ability) {
        return ability.getTracker(player).isGrantedBy(this);
    }

    public boolean isActivelyGranting(Player player, PlayerAbility ability) {
        return this.equals(ability.getTracker(player).getActiveSource());
    }

    public void revokeAllFrom(Player player) {
        PalInternals.revokeAll(player, this);
    }

    @Override
    public int compareTo(AbilitySource other) {
        int c = Integer.compare(other.priority, this.priority);
        return c != 0 ? c : this.id.compareTo(other.id);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AbilitySource that)) return false;
        return this.id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return this.id.hashCode();
    }

    @Override
    public String toString() {
        return "AbilitySource{" + this.id + ", priority=" + this.priority + "}";
    }
}
