/*
 * Requiem
 * Copyright (C) 2017-2023 Ladysnake
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; If not, see <https://www.gnu.org/licenses>.
 */
package ladysnake.requiem.api.v1.record;

import com.mojang.serialization.Codec;
import net.fabricmc.fabric.api.event.registry.FabricRegistryBuilder;
import net.minecraft.core.Registry;
import net.minecraft.core.MappedRegistry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.function.BiPredicate;
import java.util.function.Function;

/**
 * A data type that can be stored in a {@link GlobalRecord}.
 *
 * @param <T> type for which the {@code RecordType} is being registered.
 * @since 2.0.0
 */
public final class RecordType<T> {
    public static final ResourceKey<Registry<RecordType<?>>> REGISTRY_KEY = ResourceKey.createRegistryKey(ResourceLocation.fromNamespaceAndPath("requiem", "record_type"));
    public static final Registry<RecordType<?>> REGISTRY = FabricRegistryBuilder.from(new MappedRegistry<>(REGISTRY_KEY, com.mojang.serialization.Lifecycle.experimental())).buildAndRegister();

    private final ResourceLocation id;
    private final Codec<T> codec;
    private final BiPredicate<MinecraftServer, T> validator;

    public RecordType(ResourceLocation id, Codec<T> codec, BiPredicate<MinecraftServer, T> validator) {
        this.id = id;
        this.codec = codec;
        this.validator = validator;
    }

    public ResourceLocation getId() {
        return this.id;
    }

    public Codec<T> getCodec() {
        return this.codec;
    }

    public <U> Optional<RecordType<U>> tryCast(Codec<U> witness) {
        if (this.codec == witness) {
            @SuppressWarnings("unchecked") RecordType<U> ret = (RecordType<U>) this;
            return Optional.of(ret);
        }
        return Optional.empty();
    }

    public boolean checkValidity(MinecraftServer context, T value) {
        return this.validator.test(context, value);
    }

    public boolean isValid(MinecraftServer server, T data) {
        return this.checkValidity(server, data);
    }

    @Override
    public String toString() {
        return this.id.toString();
    }

    /**
     * Creates and registers a new {@link RecordType} with the given id.
     *
     * @param id the unique identifier used to register the resulting record type
     * @param codec the {@link Codec} to use to (de)serialize associated data
     * @return a newly registered {@link RecordType} for encoding instances of {@code T}
     */
    public static <T> RecordType<T> register(ResourceLocation id, Codec<T> codec) {
        return register(id, codec, (Function<T, ResourceKey<Level>>) null);
    }

    /**
     * Creates and registers a new {@link RecordType} with the given id.
     *
     * @param id the unique identifier used to register the resulting record type
     * @param codec the {@link Codec} to use to (de)serialize associated data
     * @param validator a validator for recorded instances
     * @return a newly registered {@link RecordType} for encoding instances of {@code T}
     */
    public static <T> RecordType<T> register(ResourceLocation id, Codec<T> codec, BiPredicate<MinecraftServer, T> validator) {
        return Registry.register(REGISTRY, id, new RecordType<>(id, codec, validator));
    }

    /**
     * Creates and registers a new {@link RecordType} with the given id.
     *
     * @param id the unique identifier used to register the resulting record type
     * @param codec the {@link Codec} to use to (de)serialize associated data
     * @param worldGetter a getter for referenced worlds which absence could invalidate the record
     * @return a newly registered {@link RecordType} for encoding instances of {@code T}
     */
    public static <T> RecordType<T> register(ResourceLocation id, Codec<T> codec, @Nullable Function<T, ResourceKey<Level>> worldGetter) {
        return Registry.register(REGISTRY, id, new RecordType<>(
            id,
            codec,
            worldGetter == null ? (s, t) -> true : (s, t) -> s.getLevel(worldGetter.apply(t)) != null)
        );
    }

    @Deprecated(forRemoval = true)
    public static <T> RecordType<T> register(ResourceLocation id, Codec<T> codec, @Nullable Function<T, ResourceKey<Level>> worldGetter, @SuppressWarnings("unused") boolean required) {
        return register(id, codec, worldGetter);
    }
}
