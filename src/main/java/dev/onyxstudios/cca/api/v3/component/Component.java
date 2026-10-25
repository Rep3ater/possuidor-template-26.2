package dev.onyxstudios.cca.api.v3.component;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;

public interface Component {
    default void readFromNbt(CompoundTag tag) {
    }

    default void writeToNbt(CompoundTag tag) {
    }

    default void readFromNbt(CompoundTag tag, HolderLookup.Provider registryLookup) {
        this.readFromNbt(tag);
    }

    default void writeToNbt(CompoundTag tag, HolderLookup.Provider registryLookup) {
        this.writeToNbt(tag);
    }
}
