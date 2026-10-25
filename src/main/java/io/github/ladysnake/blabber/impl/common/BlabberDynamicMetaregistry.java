package io.github.ladysnake.blabber.impl.common;

import com.mojang.serialization.Codec;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;

public final class BlabberDynamicMetaregistry {
    public static <T> void registerSynced(ResourceKey<Registry<T>> key, Codec<T> codec, Codec<T> networkCodec) {
        // Dynamic registry shim for NeoForge
    }
}
