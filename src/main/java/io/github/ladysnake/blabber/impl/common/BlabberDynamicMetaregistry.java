package io.github.ladysnake.blabber.impl.common;

import com.mojang.serialization.Codec;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.neoforged.neoforge.registries.DataPackRegistryEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public final class BlabberDynamicMetaregistry {
    private static final List<Consumer<DataPackRegistryEvent.NewRegistry>> REGISTRATIONS = new ArrayList<>();

    public static <T> void registerSynced(ResourceKey<Registry<T>> key, Codec<T> codec, Codec<T> networkCodec) {
        REGISTRATIONS.add(event -> event.dataPackRegistry(key, codec, networkCodec));
    }

    public static void onNewDatapackRegistry(DataPackRegistryEvent.NewRegistry event) {
        for (Consumer<DataPackRegistryEvent.NewRegistry> reg : REGISTRATIONS) {
            reg.accept(event);
        }
    }
}

