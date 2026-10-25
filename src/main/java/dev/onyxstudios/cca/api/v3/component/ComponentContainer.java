package dev.onyxstudios.cca.api.v3.component;

import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;

public interface ComponentContainer {
    Set<ComponentKey<?>> keys();

    boolean hasComponents();

    void tickServerComponents();

    void tickClientComponents();

    void copyFrom(ComponentContainer other, @Nullable HolderLookup.Provider registryLookup);

    default void copyFrom(ComponentContainer other) {
        this.copyFrom(other, null);
    }

    void fromTag(CompoundTag tag, @Nullable HolderLookup.Provider registryLookup);

    CompoundTag toTag(CompoundTag tag, @Nullable HolderLookup.Provider registryLookup);

    default void fromTag(CompoundTag tag) {
        this.fromTag(tag, null);
    }

    default CompoundTag toTag(CompoundTag tag) {
        return this.toTag(tag, null);
    }

    @Nullable
    <C extends Component> C get(ComponentKey<C> key);
}
