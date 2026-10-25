package net.fabricmc.fabric.api.event.registry;

import com.mojang.serialization.Lifecycle;
import net.minecraft.core.DefaultedMappedRegistry;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;

import java.util.EnumSet;
import java.util.Set;

public final class FabricRegistryBuilder<T, R extends Registry<T>> {
    private final ResourceKey<Registry<T>> registryKey;
    private final Identifier defaultId;
    private final Set<RegistryAttribute> attributes = EnumSet.noneOf(RegistryAttribute.class);
    private R existingRegistry;

    private FabricRegistryBuilder(ResourceKey<Registry<T>> key, Identifier defaultId) {
        this.registryKey = key;
        this.defaultId = defaultId;
    }

    @SuppressWarnings("unchecked")
    public static <T, R extends Registry<T>> FabricRegistryBuilder<T, R> from(R registry) {
        FabricRegistryBuilder<T, R> builder = new FabricRegistryBuilder<>((ResourceKey<Registry<T>>) (ResourceKey<?>) registry.key(), null);
        builder.existingRegistry = registry;
        return builder;
    }

    public static <T> FabricRegistryBuilder<T, MappedRegistry<T>> createSimple(Class<T> type, Identifier id) {
        return new FabricRegistryBuilder<>(ResourceKey.createRegistryKey(id), null);
    }

    public static <T> FabricRegistryBuilder<T, DefaultedMappedRegistry<T>> createDefaulted(Class<T> type, Identifier id, Identifier defaultId) {
        return new FabricRegistryBuilder<>(ResourceKey.createRegistryKey(id), defaultId);
    }

    public FabricRegistryBuilder<T, R> attribute(RegistryAttribute attribute) {
        this.attributes.add(attribute);
        return this;
    }

    @SuppressWarnings("unchecked")
    public R buildAndRegister() {
        if (this.existingRegistry != null) {
            return this.existingRegistry;
        }
        if (this.defaultId != null) {
            return (R) new DefaultedMappedRegistry<>(this.defaultId.toString(), this.registryKey, Lifecycle.stable(), false);
        } else {
            return (R) new MappedRegistry<>(this.registryKey, Lifecycle.stable());
        }
    }
}
