package io.github.ladysnake.locki.impl;

import dev.onyxstudios.cca.api.v3.component.ComponentKey;
import dev.onyxstudios.cca.api.v3.component.ComponentRegistry;
import dev.onyxstudios.cca.api.v3.entity.EntityComponentFactoryRegistry;
import dev.onyxstudios.cca.api.v3.entity.EntityComponentInitializer;
import dev.onyxstudios.cca.api.v3.entity.RespawnCopyStrategy;
import net.minecraft.resources.ResourceLocation;

public final class LockiComponents implements EntityComponentInitializer {
    public static final ComponentKey<InventoryKeeperBase> INVENTORY_KEEPER = ComponentRegistry.getOrCreate(
        ResourceLocation.fromNamespaceAndPath("locki", "inventory_keeper"),
        InventoryKeeperBase.class
    );

    @Override
    public void registerEntityComponentFactories(EntityComponentFactoryRegistry registry) {
        registry.registerForPlayers(INVENTORY_KEEPER, PlayerInventoryKeeper::new, RespawnCopyStrategy.ALWAYS_COPY);
    }
}
