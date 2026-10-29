package ladysnake.requiem;

import io.github.ladysnake.blabber.Blabber;
import io.github.ladysnake.impersonate.Impersonate;
import io.github.ladysnake.locki.Locki;
import io.github.ladysnake.pal.impl.PalInternals;
import ladysnake.requiem.api.v1.RequiemPlugin;
import ladysnake.requiem.api.v1.entity.ability.MobAbilityRegistry;
import ladysnake.requiem.api.v1.event.minecraft.SyncServerResourcesCallback;
import ladysnake.requiem.api.v1.remnant.SoulbindingRegistry;
import ladysnake.requiem.api.v1.util.SubDataManagerHelper;
import ladysnake.requiem.common.ApiInitializer;
import ladysnake.requiem.common.RequiemComponents;
import ladysnake.requiem.common.VanillaRequiemPlugin;
import dev.onyxstudios.cca.internal.neoforge.CcaNeoForgeEngine;
import ladysnake.requiem.common.RequiemConfig;
import ladysnake.requiem.common.RequiemRecordTypes;
import ladysnake.requiem.common.RequiemRegistries;
import ladysnake.requiem.common.advancement.RequiemStats;
import ladysnake.requiem.common.advancement.criterion.RequiemCriteria;
import ladysnake.requiem.common.block.RequiemBlockEntities;
import ladysnake.requiem.common.block.RequiemBlocks;
import ladysnake.requiem.common.command.RemnantArgumentType;
import ladysnake.requiem.common.command.RequiemCommand;
import ladysnake.requiem.common.dialogue.RemnantChoiceDialogueAction;
import ladysnake.requiem.common.enchantment.RequiemEnchantments;
import ladysnake.requiem.common.entity.RequiemEntities;
import ladysnake.requiem.common.entity.RequiemEntityAttributes;
import ladysnake.requiem.common.entity.RequiemTrackedDataHandlers;
import ladysnake.requiem.common.entity.ai.brain.PandemoniumMemoryModules;
import ladysnake.requiem.common.entity.ai.brain.PandemoniumSensorTypes;
import ladysnake.requiem.common.entity.effect.RequiemStatusEffects;
import ladysnake.requiem.common.gamerule.RequiemGamerules;
import ladysnake.requiem.common.item.RequiemItems;
import ladysnake.requiem.common.loot.RequiemLootTables;
import ladysnake.requiem.common.potion.RequiemPotions;
import ladysnake.requiem.common.sharedbody.SharedBodyManager;
import ladysnake.requiem.common.network.RequiemNetworking;
import ladysnake.requiem.common.network.ServerMessageHandling;
import ladysnake.requiem.common.particle.RequiemParticleTypes;
import ladysnake.requiem.common.screen.RequiemScreenHandlers;
import ladysnake.requiem.common.sound.RequiemSoundEvents;
import ladysnake.requiem.common.structure.RequiemStructures;
import ladysnake.requiem.common.tag.RequiemEntityTypeTags;
import ladysnake.requiem.compat.RequiemCompatibilityManager;
import ladysnake.requiem.core.RequiemCore;
import ladysnake.requiem.core.remnant.VagrantInteractionRegistryImpl;
import ladysnake.requiem.core.resurrection.ResurrectionDataLoader;
import net.minecraft.commands.synchronization.ArgumentTypeInfos;
import net.minecraft.commands.synchronization.SingletonArgumentInfo;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddServerReloadListenersEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeModificationEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.entity.monster.piglin.PiglinBrute;
import ladysnake.requiem.common.entity.PlayerShellEntity;
import ladysnake.requiem.common.entity.MorticianEntity;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.neoforged.neoforge.registries.RegisterEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(Requiem.MOD_ID)
public final class Requiem {
    public static final String MOD_ID = "requiem";
    public static final Logger LOGGER = LogManager.getLogger("Requiem");

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    public Requiem(IEventBus modEventBus) {
        RequiemConfig.load();
        ApiInitializer.init();
        RequiemEnchantments.init();
        RequiemEntityTypeTags.init();
        RequiemRecordTypes.init();
        RequiemRegistries.init();
        ServerMessageHandling.init();

        PalInternals.init();
        Impersonate.init();
        Locki.init();

        try {
            Class<?> mut = Class.forName("ladysnake.requiem.core.remnant.MutableRemnantState");
            LOGGER.info("MutableRemnantState loaded successfully: {}", mut);
            Class<?> dem = Class.forName("ladysnake.requiem.common.remnant.DemonRemnantState");
            LOGGER.info("DemonRemnantState loaded successfully: {}", dem);
        } catch (Throwable t) {
            LOGGER.error("CRITICAL ERROR LOADING REMNANT CLASSES", t);
        }

        CcaNeoForgeEngine.init(modEventBus);
        RequiemComponents requiemComponents = new RequiemComponents();
        CcaNeoForgeEngine.registerEntityInitializer(requiemComponents);
        CcaNeoForgeEngine.registerScoreboardInitializer(requiemComponents);

        modEventBus.addListener(ServerMessageHandling::registerPayloadHandlers);
        modEventBus.addListener(io.github.ladysnake.blabber.impl.common.BlabberDynamicMetaregistry::onNewDatapackRegistry);

        modEventBus.addListener(this::onRegister);
        modEventBus.addListener(this::onCommonSetup);
        modEventBus.addListener(this::onEntityAttributeCreation);
        modEventBus.addListener(this::onEntityAttributeModification);
        modEventBus.addListener(RequiemItems::onBuildCreativeTab);

        NeoForge.EVENT_BUS.addListener(this::onRegisterCommands);
        NeoForge.EVENT_BUS.addListener(this::onAddReloadListeners);
        ladysnake.requiem.common.RequiemForgeEvents.init();
        SharedBodyManager.init();
        NeoForge.EVENT_BUS.addListener(RequiemPotions::onRegisterBrewingRecipes);

        if (FMLEnvironment.getDist().isClient()) {
            ladysnake.requiem.client.RequiemClient.init(modEventBus);
        }

        SyncServerResourcesCallback.EVENT.register(player -> RequiemNetworking.sendTo(player, RequiemNetworking.createDataSyncMessage(SubDataManagerHelper.getServerHelper())));
        ApiInitializer.setPluginCallback(this::registerPlugin);
    }

    private void onRegister(RegisterEvent event) {
        event.register(NeoForgeRegistries.Keys.ENTITY_DATA_SERIALIZERS, RequiemTrackedDataHandlers::register);
        event.register(Registries.TRIGGER_TYPE, RequiemCriteria::register);
        event.register(Registries.SOUND_EVENT, RequiemSoundEvents::register);
        event.register(Registries.ATTRIBUTE, RequiemEntityAttributes::register);
        event.register(Registries.MOB_EFFECT, RequiemStatusEffects::register);
        event.register(Registries.POTION, RequiemPotions::register);
        event.register(Registries.GAME_RULE, RequiemGamerules::register);
        event.register(Registries.BLOCK, RequiemBlocks::registerBlocks);
        event.register(Registries.ITEM, helper -> {
            RequiemBlocks.registerBlockItems(helper);
            RequiemItems.register(helper);
        });
        event.register(Registries.ENTITY_TYPE, RequiemEntities::register);
        event.register(Registries.BLOCK_ENTITY_TYPE, RequiemBlockEntities::register);
        event.register(Registries.PARTICLE_TYPE, RequiemParticleTypes::register);
        event.register(Registries.MENU, RequiemScreenHandlers::register);
        event.register(Registries.LOOT_CONDITION_TYPE, RequiemLootTables::register);
        event.register(Registries.STRUCTURE_TYPE, RequiemStructures::register);
        event.register(Registries.CUSTOM_STAT, RequiemStats::register);
        event.register(Registries.MEMORY_MODULE_TYPE, PandemoniumMemoryModules::register);
        event.register(Registries.SENSOR_TYPE, PandemoniumSensorTypes::register);
        event.register(Registries.COMMAND_ARGUMENT_TYPE, helper -> {
            SingletonArgumentInfo<RemnantArgumentType> remnantArgInfo = SingletonArgumentInfo.contextFree(RemnantArgumentType::remnantType);
            ArgumentTypeInfos.registerByClass(RemnantArgumentType.class, remnantArgInfo);
            helper.register(id("remnant"), remnantArgInfo);
        });
    }

    private void onEntityAttributeCreation(EntityAttributeCreationEvent event) {
        event.put(RequiemEntities.CURED_VILLAGER, Villager.createAttributes().build());
        event.put(RequiemEntities.CURED_PIGLIN, Piglin.createAttributes().build());
        event.put(RequiemEntities.CURED_PIGLIN_BRUTE, PiglinBrute.createAttributes().build());
        event.put(RequiemEntities.PLAYER_SHELL, PlayerShellEntity.createPlayerShellAttributes().build());
        event.put(RequiemEntities.MORTICIAN, MorticianEntity.createMorticianAttributes().build());
        event.put(RequiemEntities.NUCLEUS_SOVEREIGN, net.minecraft.world.entity.monster.zombie.Husk.createAttributes().build());
        event.put(RequiemEntities.ROBOTIC_CONSTRUCT, ladysnake.requiem.common.construct.entity.RoboticConstructEntity.createAttributes().build());
    }

    private void onEntityAttributeModification(EntityAttributeModificationEvent event) {
        for (EntityType<? extends LivingEntity> type : event.getTypes()) {
            if (type == EntityTypes.FROG) {
                event.add(type, RequiemEntityAttributes.SOUL_DEFENSE, -5.0);
            } else {
                event.add(type, RequiemEntityAttributes.SOUL_DEFENSE);
            }
        }
        event.add(EntityTypes.PLAYER, RequiemEntityAttributes.SOUL_OFFENSE);
    }

    private void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            RequiemBlocks.postRegister();
            RequiemStatusEffects.postRegister();
            RequiemItems.postRegister();
            RequiemEntities.postRegister();
            ApiInitializer.discoverEntryPoints();
            if (ladysnake.requiem.api.v1.RequiemApi.getRegisteredPlugins().isEmpty()) {
                ladysnake.requiem.api.v1.RequiemApi.registerPlugin(new VanillaRequiemPlugin());
            }
            Blabber.registerAction(id("remnant_choice"), RemnantChoiceDialogueAction.CODEC);
            RequiemCompatibilityManager.init();
        });
    }

    private void onRegisterCommands(RegisterCommandsEvent event) {
        RequiemCommand.register(event.getDispatcher());
    }

    private void onAddReloadListeners(AddServerReloadListenersEvent event) {
        event.addListener(id("resurrection_data"), ResurrectionDataLoader.INSTANCE);
    }

    private void registerPlugin(RequiemPlugin plugin) {
        LOGGER.info("Registering Requiem plugin: {}", plugin.getClass().getName());
        plugin.onRequiemInitialize();
        plugin.registerRemnantStates(RequiemRegistries.REMNANT_STATES);
        plugin.registerMobAbilities(MobAbilityRegistry.instance());
        plugin.registerSoulBindings(SoulbindingRegistry.instance());
        plugin.registerVagrantInteractions(VagrantInteractionRegistryImpl.INSTANCE);
        plugin.registerPossessionItemActions(RequiemRegistries.MOB_ACTIONS);
    }
}
