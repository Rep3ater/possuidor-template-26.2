package dev.onyxstudios.cca.api.v3.entity;

import dev.onyxstudios.cca.api.v3.component.Component;
import dev.onyxstudios.cca.api.v3.component.ComponentKey;
import dev.onyxstudios.cca.api.v3.component.CopyableComponent;
import dev.onyxstudios.cca.internal.neoforge.CcaNeoForgeEngine;
import javax.annotation.Nullable;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;

@FunctionalInterface
public interface RespawnCopyStrategy<C extends Component> {
    RespawnCopyStrategy<Component> ALWAYS_COPY = (from, to, registryLookup, lossless, keepInventory, sameCharacter) -> copy(from, to, registryLookup);

    RespawnCopyStrategy<Component> INVENTORY = (from, to, registryLookup, lossless, keepInventory, sameCharacter) -> {
        if (lossless || keepInventory) {
            copy(from, to, registryLookup);
        }
    };

    RespawnCopyStrategy<Component> CHARACTER = (from, to, registryLookup, lossless, keepInventory, sameCharacter) -> {
        if (lossless || sameCharacter) {
            copy(from, to, registryLookup);
        }
    };

    RespawnCopyStrategy<Component> LOSSLESS_ONLY = (from, to, registryLookup, lossless, keepInventory, sameCharacter) -> {
        if (lossless) {
            copy(from, to, registryLookup);
        }
    };

    RespawnCopyStrategy<Component> NEVER_COPY = (from, to, registryLookup, lossless, keepInventory, sameCharacter) -> {
    };

    void copyForRespawn(C from, C to, @Nullable HolderLookup.Provider registryLookup, boolean lossless, boolean keepInventory, boolean sameCharacter);

    @SuppressWarnings("unchecked")
    static <C extends Component> void copy(C from, C to, @Nullable HolderLookup.Provider registryLookup) {
        if (to instanceof CopyableComponent<?> copyable) {
            if (registryLookup != null) {
                ((CopyableComponent<C>) copyable).copyFrom(from, registryLookup);
            } else {
                ((CopyableComponent<C>) copyable).copyFrom(from);
            }
        } else {
            CompoundTag tag = new CompoundTag();
            if (registryLookup != null) {
                from.writeToNbt(tag, registryLookup);
                to.readFromNbt(tag, registryLookup);
            } else {
                from.writeToNbt(tag);
                to.readFromNbt(tag);
            }
        }
    }

    static <C extends Component> RespawnCopyStrategy<? super C> get(ComponentKey<C> key) {
        return CcaNeoForgeEngine.getRespawnStrategy(key);
    }

    static <C extends Component> RespawnCopyStrategy<? super C> get(ComponentKey<C> key, Class<? extends Entity> entityClass) {
        return CcaNeoForgeEngine.getRespawnStrategy(key, entityClass);
    }
}
