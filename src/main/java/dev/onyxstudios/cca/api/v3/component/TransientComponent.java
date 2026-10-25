package dev.onyxstudios.cca.api.v3.component;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;

public interface TransientComponent extends Component {
    @Override
    default void readFromNbt(CompoundTag tag) {
    }

    @Override
    default void writeToNbt(CompoundTag tag) {
    }

    @Override
    default void readFromNbt(CompoundTag tag, HolderLookup.Provider registryLookup) {
    }

    @Override
    default void writeToNbt(CompoundTag tag, HolderLookup.Provider registryLookup) {
    }
}
