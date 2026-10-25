package io.github.ladysnake.pal;

import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;

public interface AbilityTracker {
    void addSource(AbilitySource abilitySource);

    void removeSource(AbilitySource abilitySource);

    boolean isGrantedBy(AbilitySource abilitySource);

    @Nullable
    AbilitySource getActiveSource();

    void refresh(boolean sync);

    boolean isEnabled();

    default void save(CompoundTag tag) {
    }

    default void load(CompoundTag tag) {
    }
}
