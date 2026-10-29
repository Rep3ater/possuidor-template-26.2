package dev.onyxstudios.cca.internal.neoforge;

import dev.onyxstudios.cca.api.v3.component.Component;
import dev.onyxstudios.cca.api.v3.component.ComponentContainer;
import dev.onyxstudios.cca.api.v3.component.ComponentFactory;
import dev.onyxstudios.cca.api.v3.component.ComponentKey;
import dev.onyxstudios.cca.api.v3.component.ComponentProvider;
import dev.onyxstudios.cca.api.v3.component.ComponentRegistry;
import dev.onyxstudios.cca.api.v3.component.sync.AutoSyncedComponent;
import dev.onyxstudios.cca.api.v3.component.sync.ComponentPacketWriter;
import dev.onyxstudios.cca.api.v3.component.sync.PlayerSyncPredicate;
import dev.onyxstudios.cca.api.v3.entity.EntityComponentFactoryRegistry;
import dev.onyxstudios.cca.api.v3.entity.EntityComponentInitializer;
import dev.onyxstudios.cca.api.v3.entity.PlayerCopyCallback;
import dev.onyxstudios.cca.api.v3.entity.PlayerSyncCallback;
import dev.onyxstudios.cca.api.v3.entity.RespawnCopyStrategy;
import dev.onyxstudios.cca.api.v3.entity.TrackingStartCallback;
import dev.onyxstudios.cca.api.v3.scoreboard.ScoreboardComponentFactory;
import dev.onyxstudios.cca.api.v3.scoreboard.ScoreboardComponentFactoryRegistry;
import dev.onyxstudios.cca.api.v3.scoreboard.ScoreboardComponentInitializer;
import dev.onyxstudios.cca.internal.base.AbstractComponentContainer;
import io.netty.buffer.Unpooled;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.scores.Scoreboard;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.attachment.IAttachmentHolder;
import net.neoforged.neoforge.attachment.IAttachmentSerializer;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class CcaNeoForgeEngine {
    public static final String MOD_ID = "requiem";
    public static final ResourceLocation ENTITY_ATTACHMENT_ID = ResourceLocation.fromNamespaceAndPath(MOD_ID, "cca_entity");
    private static final Logger LOGGER = LoggerFactory.getLogger("Requiem/CCAEngine");

    private static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
        DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, MOD_ID);

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<AbstractComponentContainer>> ENTITY_COMPONENTS =
        ATTACHMENT_TYPES.register("cca_entity", () -> AttachmentType.builder(CcaNeoForgeEngine::createEntityContainerForHolder)
            .serialize(new EntityAttachmentSerializer())
            .build());

    private static final Object REGISTRY_LOCK = new Object();
    private static final Map<ComponentKey<?>, List<EntityFactoryEntry<?, ?>>> ENTITY_FACTORIES = new LinkedHashMap<>();
    private static final Map<ComponentKey<?>, Set<ComponentKey<?>>> ENTITY_KEY_DEPENDENCIES = new LinkedHashMap<>();
    private static volatile List<ComponentKey<?>> sortedEntityKeys = List.of();

    private static final Map<ComponentKey<?>, ScoreboardComponentFactory<?>> SCOREBOARD_FACTORIES = new LinkedHashMap<>();
    private static final Map<Scoreboard, AbstractComponentContainer> CLIENT_SCOREBOARD_CONTAINERS =
        Collections.synchronizedMap(new WeakHashMap<>());
    private static final Set<Player> SWITCHING_CHARACTER_PLAYERS =
        Collections.synchronizedSet(Collections.newSetFromMap(new WeakHashMap<>()));

    private static final AtomicBoolean INITIALIZED = new AtomicBoolean(false);

    private CcaNeoForgeEngine() {
    }

    public static void init(IEventBus modBus) {
        if (!INITIALIZED.compareAndSet(false, true)) {
            return;
        }
        ATTACHMENT_TYPES.register(modBus);
        modBus.addListener(CcaNeoForgeEngine::onRegisterPayloads);

        IEventBus gameBus = NeoForge.EVENT_BUS;
        gameBus.addListener(CcaNeoForgeEngine::onEntityTickPost);
        gameBus.addListener(CcaNeoForgeEngine::onLevelTickPost);
        gameBus.addListener(CcaNeoForgeEngine::onServerTickPost);
        gameBus.addListener(CcaNeoForgeEngine::onServerStarted);
        gameBus.addListener(CcaNeoForgeEngine::onServerStopped);
        gameBus.addListener(CcaNeoForgeEngine::onPlayerLoggedIn);
        gameBus.addListener(CcaNeoForgeEngine::onPlayerRespawn);
        gameBus.addListener(CcaNeoForgeEngine::onPlayerChangedDimension);
        gameBus.addListener(CcaNeoForgeEngine::onPlayerClone);
        gameBus.addListener(CcaNeoForgeEngine::onStartTracking);
    }

    public static void registerEntityInitializer(EntityComponentInitializer initializer) {
        Objects.requireNonNull(initializer, "initializer").registerEntityComponentFactories(ENTITY_REGISTRY_IMPL);
    }

    public static void registerScoreboardInitializer(ScoreboardComponentInitializer initializer) {
        Objects.requireNonNull(initializer, "initializer").registerScoreboardComponentFactories(SCOREBOARD_REGISTRY_IMPL);
    }

    public static void markAsSwitchingCharacter(Player player) {
        SWITCHING_CHARACTER_PLAYERS.add(player);
    }

    public static void registerPayloads(PayloadRegistrar registrar) {
        registrar.playToClient(CcaEntitySyncPayload.TYPE, CcaEntitySyncPayload.STREAM_CODEC, CcaNeoForgeEngine::handleEntitySync);
        registrar.playToClient(CcaScoreboardSyncPayload.TYPE, CcaScoreboardSyncPayload.STREAM_CODEC, CcaNeoForgeEngine::handleScoreboardSync);
    }

    private static void onRegisterPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registerPayloads(registrar);
    }

    private static void handleEntitySync(CcaEntitySyncPayload payload, IPayloadContext context) {
        Player player = context.player();
        if (player == null) {
            return;
        }
        Entity entity = player.level().getEntity(payload.entityId());
        if (entity == null) {
            return;
        }
        ComponentKey<?> key = ComponentRegistry.get(payload.componentId());
        if (key == null) {
            return;
        }
        Component component = key.getNullable(entity);
        if (component instanceof AutoSyncedComponent synced) {
            RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.wrappedBuffer(payload.data()), player.registryAccess());
            try {
                synced.applySyncPacket(buf);
            } finally {
                buf.release();
            }
        }
    }

    private static void handleScoreboardSync(CcaScoreboardSyncPayload payload, IPayloadContext context) {
        Player player = context.player();
        if (player == null) {
            return;
        }
        ComponentKey<?> key = ComponentRegistry.get(payload.componentId());
        if (key == null) {
            return;
        }
        AbstractComponentContainer container = getScoreboardContainer(player.level().getScoreboard());
        Component component = container.get(key);
        if (component instanceof AutoSyncedComponent synced) {
            RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.wrappedBuffer(payload.data()), player.registryAccess());
            try {
                synced.applySyncPacket(buf);
            } finally {
                buf.release();
            }
        }
    }

    @Nullable
    public static ComponentContainer resolveContainer(@Nullable Object provider) {
        if (provider == null) {
            return null;
        }
        if (provider instanceof ComponentContainer cc) {
            return cc;
        }
        if (provider instanceof ComponentProvider cp) {
            return cp.getComponentContainer();
        }
        if (provider instanceof Entity entity) {
            return getEntityContainer(entity);
        }
        if (provider instanceof Scoreboard scoreboard) {
            return getScoreboardContainer(scoreboard);
        }
        if (provider instanceof Level level) {
            return getScoreboardContainer(level.getScoreboard());
        }
        if (provider instanceof MinecraftServer server) {
            return getServerScoreboardContainer(server);
        }
        return null;
    }

    public static AbstractComponentContainer getEntityContainer(Entity entity) {
        return entity.getData(ENTITY_COMPONENTS);
    }

    public static ComponentProvider getEntityProvider(Entity entity) {
        return new ComponentProvider() {
            @Override
            public ComponentContainer getComponentContainer() {
                return getEntityContainer(entity);
            }

            @Override
            public Iterable<ServerPlayer> getRecipientsForComponentSync() {
                return getEntityRecipients(entity);
            }
        };
    }

    public static AbstractComponentContainer getScoreboardContainer(Scoreboard scoreboard) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server != null && server.getScoreboard() == scoreboard) {
            return getServerScoreboardContainer(server);
        }
        return CLIENT_SCOREBOARD_CONTAINERS.computeIfAbsent(scoreboard, sc -> createScoreboardContainer(sc, null));
    }

    public static AbstractComponentContainer getServerScoreboardContainer(MinecraftServer server) {
        ServerLevel overworld = server.overworld();
        if (overworld != null) {
            return CcaScoreboardSavedData.get(overworld).getContainer();
        }
        return CLIENT_SCOREBOARD_CONTAINERS.computeIfAbsent(server.getScoreboard(), sc -> createScoreboardContainer(sc, server));
    }

    public static ComponentProvider getScoreboardProvider(Scoreboard scoreboard) {
        return new ComponentProvider() {
            @Override
            public ComponentContainer getComponentContainer() {
                return getScoreboardContainer(scoreboard);
            }

            @Override
            public Iterable<ServerPlayer> getRecipientsForComponentSync() {
                MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
                if (server != null && server.getScoreboard() == scoreboard) {
                    return server.getPlayerList().getPlayers();
                }
                return List.of();
            }
        };
    }

    /**
     * Helper for PlayerSplitter and entity NBT manipulation on NeoForge 26.2:
     * removes the given component key ID from both legacy top-level {@code cardinal_components}
     * and NeoForge's {@code neoforge:attachments -> requiem:cca_entity -> cardinal_components}.
     */
    public static void removeComponentFromEntityNbt(CompoundTag entityRootNbt, ComponentKey<?> key) {
        String keyId = key.getId().toString();
        if (entityRootNbt.contains(AbstractComponentContainer.NBT_KEY)) {
            entityRootNbt.getCompound(AbstractComponentContainer.NBT_KEY).remove(keyId);
        }
        if (entityRootNbt.contains("neoforge:attachments")) {
            CompoundTag attachments = entityRootNbt.getCompound("neoforge:attachments");
            String attachmentId = ENTITY_ATTACHMENT_ID.toString();
            if (attachments.contains(attachmentId)) {
                CompoundTag ccaAttachment = attachments.getCompound(attachmentId);
                if (ccaAttachment.contains(AbstractComponentContainer.NBT_KEY)) {
                    ccaAttachment.getCompound(AbstractComponentContainer.NBT_KEY).remove(keyId);
                }
            }
        }
    }

    public static <C extends Component> void syncComponent(
        Object provider,
        ComponentKey<C> key,
        ComponentPacketWriter writer,
        PlayerSyncPredicate predicate
    ) {
        if (provider instanceof Entity entity) {
            if (entity.level().isClientSide()) {
                return;
            }
            for (ServerPlayer recipient : getEntityRecipients(entity)) {
                if (predicate.shouldSyncWith(recipient)) {
                    byte[] bytes = encodePacket(writer, recipient);
                    PacketDistributor.sendToPlayer(recipient, new CcaEntitySyncPayload(entity.getId(), key.getId(), bytes));
                }
            }
            return;
        }

        Scoreboard scoreboard = null;
        if (provider instanceof Scoreboard sc) {
            scoreboard = sc;
        } else if (provider instanceof Level level && !level.isClientSide()) {
            scoreboard = level.getScoreboard();
        } else if (provider instanceof MinecraftServer server) {
            scoreboard = server.getScoreboard();
        }

        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (scoreboard != null && server != null && server.getScoreboard() == scoreboard) {
            for (ServerPlayer recipient : server.getPlayerList().getPlayers()) {
                if (predicate.shouldSyncWith(recipient)) {
                    byte[] bytes = encodePacket(writer, recipient);
                    PacketDistributor.sendToPlayer(recipient, new CcaScoreboardSyncPayload(key.getId(), bytes));
                }
            }
            return;
        }

        if (provider instanceof ComponentProvider cp) {
            for (ServerPlayer recipient : cp.getRecipientsForComponentSync()) {
                if (predicate.shouldSyncWith(recipient)) {
                    byte[] bytes = encodePacket(writer, recipient);
                    PacketDistributor.sendToPlayer(recipient, new CcaScoreboardSyncPayload(key.getId(), bytes));
                }
            }
        }
    }

    public static void syncAllEntityComponentsToPlayer(Entity entity, ServerPlayer recipient) {
        AbstractComponentContainer container = getEntityContainer(entity);
        for (Map.Entry<ComponentKey<?>, Component> entry : container.entries()) {
            if (entry.getValue() instanceof AutoSyncedComponent synced && synced.shouldSyncWith(recipient)) {
                byte[] bytes = encodePacket(synced, recipient);
                PacketDistributor.sendToPlayer(recipient, new CcaEntitySyncPayload(entity.getId(), entry.getKey().getId(), bytes));
            }
        }
    }

    public static void syncAllScoreboardComponentsToPlayer(MinecraftServer server, ServerPlayer recipient) {
        AbstractComponentContainer container = getServerScoreboardContainer(server);
        for (Map.Entry<ComponentKey<?>, Component> entry : container.entries()) {
            if (entry.getValue() instanceof AutoSyncedComponent synced && synced.shouldSyncWith(recipient)) {
                byte[] bytes = encodePacket(synced, recipient);
                PacketDistributor.sendToPlayer(recipient, new CcaScoreboardSyncPayload(entry.getKey().getId(), bytes));
            }
        }
    }

    private static byte[] encodePacket(ComponentPacketWriter writer, ServerPlayer recipient) {
        RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), recipient.registryAccess());
        try {
            writer.writeSyncPacket(buf, recipient);
            byte[] bytes = new byte[buf.readableBytes()];
            buf.readBytes(bytes);
            return bytes;
        } finally {
            buf.release();
        }
    }

    private static Iterable<ServerPlayer> getEntityRecipients(Entity entity) {
        if (entity.level().isClientSide()) {
            return List.of();
        }
        List<ServerPlayer> recipients = new ArrayList<>();
        if (entity instanceof ServerPlayer selfPlayer && selfPlayer.connection != null) {
            recipients.add(selfPlayer);
        }
        if (entity.level().getChunkSource() instanceof ServerChunkCache chunkCache) {
            for (ServerPlayer watcher : chunkCache.chunkMap.getPlayers(new net.minecraft.world.level.ChunkPos(entity.blockPosition()), false)) {
                if (watcher != entity) {
                    recipients.add(watcher);
                }
            }
        }
        return recipients;
    }

    @SuppressWarnings("unchecked")
    public static <C extends Component> RespawnCopyStrategy<? super C> getRespawnStrategy(ComponentKey<C> key) {
        return getRespawnStrategy(key, Player.class);
    }

    @SuppressWarnings("unchecked")
    public static <C extends Component> RespawnCopyStrategy<? super C> getRespawnStrategy(ComponentKey<C> key, Class<? extends Entity> entityClass) {
        synchronized (REGISTRY_LOCK) {
            List<EntityFactoryEntry<?, ?>> entries = ENTITY_FACTORIES.get(key);
            if (entries != null) {
                EntityFactoryEntry<?, ?> best = null;
                for (EntityFactoryEntry<?, ?> entry : entries) {
                    if (entry.targetClass.isAssignableFrom(entityClass) && entry.matchesClass(entityClass)) {
                        if (best == null || best.targetClass.isAssignableFrom(entry.targetClass)) {
                            best = entry;
                        }
                    }
                }
                if (best != null && best.respawnStrategy != null) {
                    return (RespawnCopyStrategy<? super C>) best.respawnStrategy;
                }
            }
        }
        return RespawnCopyStrategy.LOSSLESS_ONLY;
    }

    private static AbstractComponentContainer createEntityContainerForHolder(IAttachmentHolder holder) {
        if (holder instanceof Entity entity) {
            return createEntityContainer(entity);
        }
        return new AbstractComponentContainer(Map.of());
    }

    public static AbstractComponentContainer createEntityContainer(Entity entity) {
        List<ComponentKey<?>> keys = sortedEntityKeys;
        if (keys.isEmpty()) {
            return new AbstractComponentContainer(Map.of());
        }
        Map<ComponentKey<?>, Component> map = new LinkedHashMap<>();
        for (ComponentKey<?> key : keys) {
            List<EntityFactoryEntry<?, ?>> entries;
            synchronized (REGISTRY_LOCK) {
                entries = ENTITY_FACTORIES.get(key);
            }
            if (entries == null || entries.isEmpty()) {
                continue;
            }
            EntityFactoryEntry<?, ?> best = null;
            for (EntityFactoryEntry<?, ?> candidate : entries) {
                if (candidate.matches(entity)) {
                    if (best == null || best.targetClass.isAssignableFrom(candidate.targetClass)) {
                        best = candidate;
                    }
                }
            }
            if (best != null) {
                Component created = best.create(entity);
                if (created != null) {
                    map.put(key, created);
                }
            }
        }
        return new AbstractComponentContainer(map);
    }

    public static AbstractComponentContainer createScoreboardContainer(Scoreboard scoreboard, @Nullable MinecraftServer server) {
        Map<ComponentKey<?>, Component> map = new LinkedHashMap<>();
        synchronized (REGISTRY_LOCK) {
            for (Map.Entry<ComponentKey<?>, ScoreboardComponentFactory<?>> entry : SCOREBOARD_FACTORIES.entrySet()) {
                Component created = entry.getValue().createForScoreboard(scoreboard, server);
                if (created != null) {
                    map.put(entry.getKey(), created);
                }
            }
        }
        return new AbstractComponentContainer(map);
    }

    private static void onEntityTickPost(EntityTickEvent.Post event) {
        Entity entity = event.getEntity();
        if (!entity.hasData(ENTITY_COMPONENTS) && sortedEntityKeys.isEmpty()) {
            return;
        }
        AbstractComponentContainer container = getEntityContainer(entity);
        if (!container.hasComponents()) {
            return;
        }
        if (entity.level().isClientSide()) {
            container.tickClientComponents();
        } else {
            container.tickServerComponents();
        }
    }

    private static void onLevelTickPost(LevelTickEvent.Post event) {
        Level level = event.getLevel();
        if (level.isClientSide()) {
            AbstractComponentContainer container = getScoreboardContainer(level.getScoreboard());
            if (container.hasComponents()) {
                container.tickClientComponents();
            }
        }
    }

    private static void onServerTickPost(ServerTickEvent.Post event) {
        AbstractComponentContainer container = getServerScoreboardContainer(event.getServer());
        if (container.hasComponents()) {
            container.tickServerComponents();
        }
    }

    private static void onServerStarted(ServerStartedEvent event) {
        // Ensure scoreboard components are initialized and loaded at server startup
        getServerScoreboardContainer(event.getServer());
    }

    private static void onServerStopped(ServerStoppedEvent event) {
        CLIENT_SCOREBOARD_CONTAINERS.remove(event.getServer().getScoreboard());
    }

    private static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            MinecraftServer server = player.level().getServer();
            if (server != null) {
                syncAllScoreboardComponentsToPlayer(server, player);
            }
            syncAllEntityComponentsToPlayer(player, player);
            PlayerSyncCallback.EVENT.invoker().onPlayerSync(player);
        }
    }

    private static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            syncAllEntityComponentsToPlayer(player, player);
            PlayerSyncCallback.EVENT.invoker().onPlayerSync(player);
        }
    }

    private static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            syncAllEntityComponentsToPlayer(player, player);
            PlayerSyncCallback.EVENT.invoker().onPlayerSync(player);
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void onPlayerClone(PlayerEvent.Clone event) {
        Player original = event.getOriginal();
        Player clone = event.getEntity();
        boolean lossless = !event.isWasDeath();
        boolean switchingCharacter = SWITCHING_CHARACTER_PLAYERS.remove(original);
        boolean sameCharacter = !switchingCharacter;
        boolean keepInventory = false;
        if (clone instanceof ServerPlayer serverClone) {
            keepInventory = serverClone.level().getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY);
        }

        AbstractComponentContainer fromContainer = getEntityContainer(original);
        AbstractComponentContainer toContainer = getEntityContainer(clone);
        var registryLookup = clone.registryAccess();

        for (ComponentKey key : toContainer.keys()) {
            Component fromComp = fromContainer.get(key);
            Component toComp = toContainer.get(key);
            if (fromComp != null && toComp != null) {
                RespawnCopyStrategy strategy = getRespawnStrategy(key, clone.getClass());
                strategy.copyForRespawn(fromComp, toComp, registryLookup, lossless, keepInventory, sameCharacter);
            }
        }

        if (original instanceof ServerPlayer serverOrig && clone instanceof ServerPlayer serverClone) {
            PlayerCopyCallback.EVENT.invoker().copyData(serverOrig, serverClone, lossless);
        }
    }

    private static void onStartTracking(PlayerEvent.StartTracking event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            Entity target = event.getTarget();
            syncAllEntityComponentsToPlayer(target, player);
            TrackingStartCallback.EVENT.invoker().onPlayerStartTracking(player, target);
        }
    }

    private static void rebuildSortedEntityKeysLocked() {
        List<ComponentKey<?>> result = new ArrayList<>(ENTITY_FACTORIES.size());
        Set<ComponentKey<?>> visited = new LinkedHashSet<>();
        Set<ComponentKey<?>> visiting = new LinkedHashSet<>();
        for (ComponentKey<?> key : ENTITY_FACTORIES.keySet()) {
            visitEntityKeyLocked(key, visited, visiting, result);
        }
        sortedEntityKeys = List.copyOf(result);
    }

    private static void visitEntityKeyLocked(
        ComponentKey<?> key,
        Set<ComponentKey<?>> visited,
        Set<ComponentKey<?>> visiting,
        List<ComponentKey<?>> result
    ) {
        if (visited.contains(key)) {
            return;
        }
        if (!visiting.add(key)) {
            return;
        }
        Set<ComponentKey<?>> deps = ENTITY_KEY_DEPENDENCIES.get(key);
        if (deps != null) {
            for (ComponentKey<?> dep : deps) {
                if (ENTITY_FACTORIES.containsKey(dep)) {
                    visitEntityKeyLocked(dep, visited, visiting, result);
                }
            }
        }
        visiting.remove(key);
        visited.add(key);
        result.add(key);
    }

    private static final class EntityAttachmentSerializer implements IAttachmentSerializer<CompoundTag, AbstractComponentContainer> {
        @Override
        public AbstractComponentContainer read(IAttachmentHolder holder, CompoundTag input, HolderLookup.Provider registries) {
            AbstractComponentContainer container = createEntityContainerForHolder(holder);
            var lookup = holder instanceof Entity entity ? entity.registryAccess() : registries;
            if (input.contains(AbstractComponentContainer.NBT_KEY, CompoundTag.TAG_COMPOUND)) {
                container.fromComponentsTag(input.getCompound(AbstractComponentContainer.NBT_KEY), lookup);
            }
            return container;
        }

        @Override
        public @Nullable CompoundTag write(AbstractComponentContainer attachment, HolderLookup.Provider registries) {
            if (!attachment.hasComponents()) {
                return null;
            }
            // Note: ValueOutput carries registry context internally, while Component#writeToNbt takes lookup if available
            var lookup = registries;
            CompoundTag componentsTag = attachment.toComponentsTag(lookup);
            if (componentsTag.isEmpty()) {
                return null;
            }
            CompoundTag result = new CompoundTag();
            result.put(AbstractComponentContainer.NBT_KEY, componentsTag);
            return result;
        }
    }

    private static final class EntityFactoryEntry<E extends Entity, C extends Component> {
        final Class<E> targetClass;
        final Predicate<Class<? extends E>> filter;
        final ComponentFactory<? super E, ? extends C> factory;
        final @Nullable RespawnCopyStrategy<? super C> respawnStrategy;

        EntityFactoryEntry(
            Class<E> targetClass,
            Predicate<Class<? extends E>> filter,
            ComponentFactory<? super E, ? extends C> factory,
            @Nullable RespawnCopyStrategy<? super C> respawnStrategy
        ) {
            this.targetClass = targetClass;
            this.filter = filter;
            this.factory = factory;
            this.respawnStrategy = respawnStrategy;
        }

        @SuppressWarnings("unchecked")
        boolean matches(Entity entity) {
            return this.targetClass.isInstance(entity) && this.filter.test((Class<? extends E>) entity.getClass());
        }

        @SuppressWarnings("unchecked")
        boolean matchesClass(Class<? extends Entity> entityClass) {
            return this.targetClass.isAssignableFrom(entityClass) && this.filter.test((Class<? extends E>) entityClass);
        }

        @SuppressWarnings("unchecked")
        Component create(Entity entity) {
            return this.factory.createComponent((E) entity);
        }
    }

    private static final EntityComponentFactoryRegistry ENTITY_REGISTRY_IMPL = new EntityComponentFactoryRegistry() {
        @Override
        public <E extends Entity, C extends Component> void registerFor(
            Class<E> target,
            ComponentKey<C> key,
            ComponentFactory<? super E, ? extends C> factory
        ) {
            this.beginRegistration(target, key).end(factory);
        }

        @Override
        public <C extends Component> void registerForPlayers(
            ComponentKey<C> key,
            ComponentFactory<? super Player, ? extends C> factory,
            RespawnCopyStrategy<? super C> respawnStrategy
        ) {
            this.beginRegistration(Player.class, key).respawnStrategy(respawnStrategy).end(factory);
        }

        @Override
        public <E extends Entity, C extends Component> Registration<E, C> beginRegistration(Class<E> target, ComponentKey<C> key) {
            Objects.requireNonNull(target, "target");
            Objects.requireNonNull(key, "key");
            return new RegistrationImpl<>(target, key);
        }
    };

    private static final class RegistrationImpl<E extends Entity, C extends Component> implements EntityComponentFactoryRegistry.Registration<E, C> {
        private final Class<E> target;
        private final ComponentKey<C> key;
        private Predicate<Class<? extends E>> filter = cls -> true;
        private @Nullable RespawnCopyStrategy<? super C> respawnStrategy;
        private final List<ComponentKey<?>> dependencies = new ArrayList<>();

        RegistrationImpl(Class<E> target, ComponentKey<C> key) {
            this.target = target;
            this.key = key;
        }

        @Override
        public EntityComponentFactoryRegistry.Registration<E, C> filter(Predicate<Class<? extends E>> test) {
            this.filter = this.filter.and(Objects.requireNonNull(test, "test"));
            return this;
        }

        @Override
        public EntityComponentFactoryRegistry.Registration<E, C> respawnStrategy(RespawnCopyStrategy<? super C> strategy) {
            this.respawnStrategy = Objects.requireNonNull(strategy, "strategy");
            return this;
        }

        @Override
        public EntityComponentFactoryRegistry.Registration<E, C> after(ComponentKey<?>... keys) {
            Collections.addAll(this.dependencies, keys);
            return this;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <I extends C> EntityComponentFactoryRegistry.Registration<E, I> impl(Class<I> impl) {
            return (EntityComponentFactoryRegistry.Registration<E, I>) this;
        }

        @Override
        public void end(ComponentFactory<? super E, ? extends C> factory) {
            Objects.requireNonNull(factory, "factory");
            synchronized (REGISTRY_LOCK) {
                ENTITY_FACTORIES.computeIfAbsent(this.key, k -> new ArrayList<>())
                    .add(new EntityFactoryEntry<>(this.target, this.filter, factory, this.respawnStrategy));
                if (!this.dependencies.isEmpty()) {
                    ENTITY_KEY_DEPENDENCIES.computeIfAbsent(this.key, k -> new LinkedHashSet<>()).addAll(this.dependencies);
                }
                rebuildSortedEntityKeysLocked();
            }
        }
    }

    private static final ScoreboardComponentFactoryRegistry SCOREBOARD_REGISTRY_IMPL = new ScoreboardComponentFactoryRegistry() {
        @Override
        public <C extends Component> void registerScoreboardComponent(ComponentKey<C> key, ScoreboardComponentFactory<? extends C> factory) {
            Objects.requireNonNull(key, "key");
            Objects.requireNonNull(factory, "factory");
            synchronized (REGISTRY_LOCK) {
                SCOREBOARD_FACTORIES.put(key, factory);
            }
        }
    };
}
