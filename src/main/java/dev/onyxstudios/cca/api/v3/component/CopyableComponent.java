package dev.onyxstudios.cca.api.v3.component;

import net.minecraft.core.HolderLookup;

public interface CopyableComponent<C extends Component> extends Component {
    void copyFrom(C other);

    default void copyFrom(C other, HolderLookup.Provider registryLookup) {
        this.copyFrom(other);
    }
}
