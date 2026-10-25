package io.github.ladysnake.pal.impl;

import dev.onyxstudios.cca.api.v3.component.ComponentKey;
import dev.onyxstudios.cca.api.v3.component.ComponentRegistry;
import dev.onyxstudios.cca.api.v3.component.sync.AutoSyncedComponent;
import dev.onyxstudios.cca.api.v3.entity.EntityComponentFactoryRegistry;
import dev.onyxstudios.cca.api.v3.entity.EntityComponentInitializer;
import dev.onyxstudios.cca.api.v3.entity.RespawnCopyStrategy;
import dev.onyxstudios.cca.internal.neoforge.CcaNeoForgeEngine;
import io.github.ladysnake.pal.AbilitySource;
import io.github.ladysnake.pal.AbilityTracker;
import io.github.ladysnake.pal.PlayerAbility;
import io.github.ladysnake.pal.VanillaAbilities;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiFunction;
import javax.annotation.Nullable;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityInvulnerabilityCheckEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

public final class PalInternals implements EntityComponentInitializer {
    private static final Map<Identifier, AbilitySource> SOURCES = new ConcurrentHashMap<>();
    private static final Map<Identifier, PlayerAbility> ABILITIES = new LinkedHashMap<>();
    public static final ComponentKey<PalPlayerComponent> PAL_COMPONENT =
        ComponentRegistry.getOrCreate(Identifier.fromNamespaceAndPath("pal", "abilities"), PalPlayerComponent.class);

    private static final AtomicBoolean INITIALIZED = new AtomicBoolean(false);

    public static void init() {
        if (!INITIALIZED.compareAndSet(false, true)) {
            return;
        }
        VanillaAbilities.init();
        CcaNeoForgeEngine.registerEntityInitializer(new PalInternals());

        IEventBus gameBus = NeoForge.EVENT_BUS;
        gameBus.addListener(PalInternals::onInvulnerabilityCheck);
        gameBus.addListener(PalInternals::onPlayerLoggedIn);
        gameBus.addListener(PalInternals::onPlayerRespawn);
        gameBus.addListener(PalInternals::onPlayerChangedDimension);
        gameBus.addListener(PalInternals::onPlayerChangeGameMode);
    }

    public static synchronized PlayerAbility registerAbility(
        Identifier id,
        BiFunction<PlayerAbility, Player, AbilityTracker> factory
    ) {
        if (ABILITIES.containsKey(id)) {
            throw new IllegalStateException("Duplicate PlayerAbility registration: " + id);
        }
        PlayerAbility ability = new PlayerAbility(id, factory);
        ABILITIES.put(id, ability);
        return ability;
    }

    @Nullable
    public static synchronized PlayerAbility getAbility(Identifier id) {
        return ABILITIES.get(id);
    }

    public static synchronized Collection<PlayerAbility> getAbilities() {
        return Collections.unmodifiableCollection(ABILITIES.values());
    }

    public static AbilitySource getSource(Identifier id, int priority) {
        AbilitySource existing = SOURCES.computeIfAbsent(id, k -> new AbilitySource(k, priority));
        if (existing.getPriority() != priority && priority != AbilitySource.DEFAULT) {
            throw new IllegalStateException("AbilitySource " + id + " registered with conflicting priorities: "
                + existing.getPriority() + " vs " + priority);
        }
        return existing;
    }

    public static AbilityTracker getTracker(Player player, PlayerAbility ability) {
        return PAL_COMPONENT.get(player).getOrCreateTracker(ability);
    }

    public static void revokeAll(Player player, AbilitySource source) {
        PalPlayerComponent comp = PAL_COMPONENT.getNullable(player);
        if (comp != null) {
            comp.revokeAll(source);
        }
    }

    public static void refreshAll(Player player, boolean sync) {
        PalPlayerComponent comp = PAL_COMPONENT.getNullable(player);
        if (comp != null) {
            comp.refreshAll(sync);
        }
    }

    public static void syncAbility(Player player) {
        if (!player.level().isClientSide()) {
            PAL_COMPONENT.sync(player);
        }
    }

    @Override
    public void registerEntityComponentFactories(EntityComponentFactoryRegistry registry) {
        registry.registerForPlayers(PAL_COMPONENT, PalPlayerComponent::new, RespawnCopyStrategy.LOSSLESS_ONLY);
    }

    private static void onInvulnerabilityCheck(EntityInvulnerabilityCheckEvent event) {
        if (!event.isInvulnerable()
            && event.getEntity() instanceof Player player
            && !event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)
            && VanillaAbilities.INVULNERABLE.isEnabledFor(player)) {
            event.setInvulnerable(true);
        }
    }

    private static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        refreshAll(event.getEntity(), true);
    }

    private static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        refreshAll(event.getEntity(), true);
    }

    private static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        refreshAll(event.getEntity(), true);
    }

    private static void onPlayerChangeGameMode(PlayerEvent.PlayerChangeGameModeEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer && serverPlayer.level().getServer() != null) {
            serverPlayer.level().getServer().execute(() -> refreshAll(serverPlayer, true));
        }
    }

    public static final class PalPlayerComponent implements AutoSyncedComponent {
        private final Player player;
        private final Map<PlayerAbility, AbilityTracker> trackers = new LinkedHashMap<>();
        private final Map<PlayerAbility, Boolean> clientSyncedStates = new ConcurrentHashMap<>();

        public PalPlayerComponent(Player player) {
            this.player = player;
            for (PlayerAbility ability : getAbilities()) {
                this.trackers.put(ability, ability.createTracker(player));
            }
        }

        public AbilityTracker getOrCreateTracker(PlayerAbility ability) {
            return this.trackers.computeIfAbsent(ability, a -> a.createTracker(this.player));
        }

        public void revokeAll(AbilitySource source) {
            for (AbilityTracker tracker : this.trackers.values()) {
                tracker.removeSource(source);
            }
        }

        public void refreshAll(boolean sync) {
            for (AbilityTracker tracker : this.trackers.values()) {
                tracker.refresh(sync);
            }
        }

        @Override
        public boolean shouldSyncWith(ServerPlayer recipient) {
            return recipient == this.player;
        }

        @Override
        public void writeSyncPacket(RegistryFriendlyByteBuf buf, ServerPlayer recipient) {
            buf.writeVarInt(this.trackers.size());
            for (Map.Entry<PlayerAbility, AbilityTracker> entry : this.trackers.entrySet()) {
                buf.writeIdentifier(entry.getKey().getId());
                buf.writeBoolean(entry.getValue().isEnabled());
            }
        }

        @Override
        public void applySyncPacket(RegistryFriendlyByteBuf buf) {
            int count = buf.readVarInt();
            for (int i = 0; i < count; i++) {
                Identifier id = buf.readIdentifier();
                boolean enabled = buf.readBoolean();
                PlayerAbility ability = getAbility(id);
                if (ability != null) {
                    this.clientSyncedStates.put(ability, enabled);
                    AbilityTracker tracker = this.trackers.get(ability);
                    if (tracker != null) {
                        tracker.refresh(false);
                    }
                }
            }
        }

        public boolean isSyncedEnabled(PlayerAbility ability) {
            return Boolean.TRUE.equals(this.clientSyncedStates.get(ability));
        }

        @Override
        public void readFromNbt(CompoundTag tag, HolderLookup.Provider registryLookup) {
            CompoundTag abilitiesTag = tag.getCompoundOrEmpty("abilities");
            for (PlayerAbility ability : getAbilities()) {
                AbilityTracker tracker = this.getOrCreateTracker(ability);
                String key = ability.getId().toString();
                if (abilitiesTag.contains(key)) {
                    tracker.load(abilitiesTag.getCompoundOrEmpty(key));
                }
            }
            if (!this.player.level().isClientSide()) {
                this.refreshAll(false);
            }
        }

        @Override
        public void writeToNbt(CompoundTag tag, HolderLookup.Provider registryLookup) {
            CompoundTag abilitiesTag = new CompoundTag();
            for (Map.Entry<PlayerAbility, AbilityTracker> entry : this.trackers.entrySet()) {
                CompoundTag abilityTag = new CompoundTag();
                entry.getValue().save(abilityTag);
                if (!abilityTag.isEmpty()) {
                    abilitiesTag.put(entry.getKey().getId().toString(), abilityTag);
                }
            }
            if (!abilitiesTag.isEmpty()) {
                tag.put("abilities", abilitiesTag);
            }
        }
    }
}
