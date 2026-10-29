package dev.onyxstudios.cca.api.v3.component;

import dev.onyxstudios.cca.api.v3.component.sync.AutoSyncedComponent;
import dev.onyxstudios.cca.api.v3.component.sync.ComponentPacketWriter;
import dev.onyxstudios.cca.api.v3.component.sync.PlayerSyncPredicate;
import dev.onyxstudios.cca.internal.neoforge.CcaNeoForgeEngine;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.resources.ResourceLocation;

public final class ComponentKey<C extends Component> {
    private final ResourceLocation id;
    private final Class<C> componentClass;

    ComponentKey(ResourceLocation id, Class<C> componentClass) {
        this.id = Objects.requireNonNull(id, "id");
        this.componentClass = Objects.requireNonNull(componentClass, "componentClass");
    }

    public ResourceLocation getId() {
        return this.id;
    }

    public Class<C> getComponentClass() {
        return this.componentClass;
    }

    public C get(Object provider) {
        C component = this.getNullable(Objects.requireNonNull(provider, "provider"));
        if (component == null) {
            throw new NoSuchElementException("Provider " + provider + " (" + provider.getClass().getName() + ") does not provide component " + this.id);
        }
        return component;
    }

    @Nullable
    public C getNullable(@Nullable Object provider) {
        if (provider == null) {
            return null;
        }
        ComponentContainer container = CcaNeoForgeEngine.resolveContainer(provider);
        return container == null ? null : container.get(this);
    }

    public Optional<C> maybeGet(@Nullable Object provider) {
        return Optional.ofNullable(this.getNullable(provider));
    }

    public boolean isProvidedBy(@Nullable Object provider) {
        return this.getNullable(provider) != null;
    }

    public void sync(Object provider) {
        C component = this.get(provider);
        if (component instanceof AutoSyncedComponent synced) {
            this.sync(provider, synced, synced);
        }
    }

    public void sync(Object provider, ComponentPacketWriter writer) {
        C component = this.get(provider);
        PlayerSyncPredicate predicate = component instanceof PlayerSyncPredicate p ? p : player -> true;
        this.sync(provider, writer, predicate);
    }

    public void sync(Object provider, ComponentPacketWriter writer, PlayerSyncPredicate predicate) {
        CcaNeoForgeEngine.syncComponent(provider, this, writer, predicate);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ComponentKey<?> that)) return false;
        return this.id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return this.id.hashCode();
    }

    @Override
    public String toString() {
        return "ComponentKey[" + this.id + "]";
    }
}
