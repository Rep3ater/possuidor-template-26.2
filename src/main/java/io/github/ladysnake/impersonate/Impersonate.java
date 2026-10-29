package io.github.ladysnake.impersonate;

import com.mojang.authlib.GameProfile;
import dev.onyxstudios.cca.api.v3.component.ComponentKey;
import dev.onyxstudios.cca.api.v3.component.ComponentRegistryV3;
import dev.onyxstudios.cca.api.v3.entity.EntityComponentFactoryRegistry;
import dev.onyxstudios.cca.api.v3.entity.EntityComponentInitializer;
import dev.onyxstudios.cca.api.v3.entity.RespawnCopyStrategy;
import dev.onyxstudios.cca.internal.neoforge.CcaNeoForgeEngine;
import io.github.ladysnake.impersonate.impl.PlayerImpersonator;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class Impersonate implements EntityComponentInitializer {
    public static final Logger LOGGER = LogManager.getLogger("Impersonate");
    public static final ComponentKey<Impersonator> IMPERSONATION = ComponentRegistryV3.INSTANCE
        .getOrCreate(ResourceLocation.fromNamespaceAndPath("impersonate", "impersonation"), Impersonator.class);

    private static final AtomicBoolean INITIALIZED = new AtomicBoolean(false);

    public static void init() {
        if (!INITIALIZED.compareAndSet(false, true)) {
            return;
        }
        CcaNeoForgeEngine.registerEntityInitializer(new Impersonate());
        NeoForge.EVENT_BUS.addListener(Impersonate::onNameFormat);
        NeoForge.EVENT_BUS.addListener(Impersonate::onTabListNameFormat);
    }

    @Override
    public void registerEntityComponentFactories(EntityComponentFactoryRegistry registry) {
        registry.registerForPlayers(IMPERSONATION, PlayerImpersonator::new, RespawnCopyStrategy.ALWAYS_COPY);
    }

    private static void onNameFormat(PlayerEvent.NameFormat event) {
        Impersonator impersonator = IMPERSONATION.getNullable(event.getEntity());
        if (impersonator != null) {
            GameProfile profile = impersonator.getImpersonatedProfile();
            if (profile != null && !profile.getName().isEmpty()) {
                event.setDisplayname(Component.literal(profile.getName()));
            }
        }
    }

    private static void onTabListNameFormat(PlayerEvent.TabListNameFormat event) {
        Impersonator impersonator = IMPERSONATION.getNullable(event.getEntity());
        if (impersonator != null) {
            GameProfile profile = impersonator.getImpersonatedProfile();
            if (profile != null && !profile.getName().isEmpty()) {
                event.setDisplayName(Component.literal(profile.getName()));
            }
        }
    }
}
