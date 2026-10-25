package dev.onyxstudios.cca.internal.base;

import dev.onyxstudios.cca.api.v3.component.Component;
import dev.onyxstudios.cca.api.v3.component.ComponentContainer;
import dev.onyxstudios.cca.api.v3.component.ComponentKey;
import dev.onyxstudios.cca.api.v3.component.CopyableComponent;
import dev.onyxstudios.cca.api.v3.component.TransientComponent;
import dev.onyxstudios.cca.api.v3.component.tick.ClientTickingComponent;
import dev.onyxstudios.cca.api.v3.component.tick.ServerTickingComponent;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AbstractComponentContainer implements ComponentContainer {
    public static final String NBT_KEY = "cardinal_components";
    private static final Logger LOGGER = LoggerFactory.getLogger("Requiem/CCA");

    private final Map<ComponentKey<?>, Component> components;
    private final Set<ComponentKey<?>> unmodifiableKeys;
    private final List<ServerTickingComponent> serverTickers;
    private final List<ClientTickingComponent> clientTickers;

    public AbstractComponentContainer(Map<ComponentKey<?>, Component> components) {
        this.components = new LinkedHashMap<>(components);
        this.unmodifiableKeys = Collections.unmodifiableSet(this.components.keySet());
        this.serverTickers = this.components.values().stream()
            .filter(ServerTickingComponent.class::isInstance)
            .map(ServerTickingComponent.class::cast)
            .toList();
        this.clientTickers = this.components.values().stream()
            .filter(ClientTickingComponent.class::isInstance)
            .map(ClientTickingComponent.class::cast)
            .toList();
    }

    @Override
    public Set<ComponentKey<?>> keys() {
        return this.unmodifiableKeys;
    }

    @Override
    public boolean hasComponents() {
        return !this.components.isEmpty();
    }

    @Nullable
    @Override
    @SuppressWarnings("unchecked")
    public <C extends Component> C get(ComponentKey<C> key) {
        return (C) this.components.get(key);
    }

    public Iterable<Map.Entry<ComponentKey<?>, Component>> entries() {
        return this.components.entrySet();
    }

    @Override
    public void tickServerComponents() {
        for (ServerTickingComponent ticker : this.serverTickers) {
            ticker.serverTick();
        }
    }

    @Override
    public void tickClientComponents() {
        for (ClientTickingComponent ticker : this.clientTickers) {
            ticker.clientTick();
        }
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void copyFrom(ComponentContainer other, @Nullable HolderLookup.Provider registryLookup) {
        for (Map.Entry<ComponentKey<?>, Component> entry : this.components.entrySet()) {
            ComponentKey key = entry.getKey();
            Component ours = entry.getValue();
            Component theirs = other.get(key);
            if (theirs != null) {
                if (ours instanceof CopyableComponent copyable) {
                    if (registryLookup != null) {
                        copyable.copyFrom(theirs, registryLookup);
                    } else {
                        copyable.copyFrom(theirs);
                    }
                } else if (!(ours instanceof TransientComponent)) {
                    CompoundTag tag = new CompoundTag();
                    if (registryLookup != null) {
                        theirs.writeToNbt(tag, registryLookup);
                        ours.readFromNbt(tag, registryLookup);
                    } else {
                        theirs.writeToNbt(tag);
                        ours.readFromNbt(tag);
                    }
                }
            }
        }
    }

    @Override
    public void fromTag(CompoundTag tag, @Nullable HolderLookup.Provider registryLookup) {
        CompoundTag componentMap = tag.contains(NBT_KEY) ? tag.getCompoundOrEmpty(NBT_KEY) : tag;
        this.fromComponentsTag(componentMap, registryLookup);
    }

    public void fromComponentsTag(CompoundTag componentMap, @Nullable HolderLookup.Provider registryLookup) {
        for (Map.Entry<ComponentKey<?>, Component> entry : this.components.entrySet()) {
            Component component = entry.getValue();
            if (component instanceof TransientComponent) {
                continue;
            }
            String id = entry.getKey().getId().toString();
            if (componentMap.contains(id)) {
                try {
                    CompoundTag componentTag = componentMap.getCompoundOrEmpty(id);
                    if (registryLookup != null) {
                        component.readFromNbt(componentTag, registryLookup);
                    } else {
                        component.readFromNbt(componentTag);
                    }
                } catch (Exception e) {
                    LOGGER.error("Failed to read CCA component {} from NBT", id, e);
                }
            }
        }
    }

    @Override
    public CompoundTag toTag(CompoundTag tag, @Nullable HolderLookup.Provider registryLookup) {
        CompoundTag componentMap = this.toComponentsTag(registryLookup);
        if (!componentMap.isEmpty()) {
            tag.put(NBT_KEY, componentMap);
        }
        return tag;
    }

    public CompoundTag toComponentsTag(@Nullable HolderLookup.Provider registryLookup) {
        CompoundTag componentMap = new CompoundTag();
        for (Map.Entry<ComponentKey<?>, Component> entry : this.components.entrySet()) {
            Component component = entry.getValue();
            if (component instanceof TransientComponent) {
                continue;
            }
            String id = entry.getKey().getId().toString();
            try {
                CompoundTag componentTag = new CompoundTag();
                if (registryLookup != null) {
                    component.writeToNbt(componentTag, registryLookup);
                } else {
                    component.writeToNbt(componentTag);
                }
                if (!componentTag.isEmpty()) {
                    componentMap.put(id, componentTag);
                }
            } catch (Exception e) {
                LOGGER.error("Failed to write CCA component {} to NBT", id, e);
            }
        }
        return componentMap;
    }
}
