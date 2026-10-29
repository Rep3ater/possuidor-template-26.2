package ladysnake.requiem.common;

import ladysnake.requiem.api.v1.entity.MovementAlterer;
import ladysnake.requiem.api.v1.entity.MovementConfig;
import ladysnake.requiem.api.v1.entity.MovementRegistry;
import ladysnake.requiem.api.v1.event.minecraft.PlayerRespawnCallback;
import ladysnake.requiem.api.v1.event.minecraft.PrepareRespawnCallback;
import ladysnake.requiem.api.v1.possession.Possessable;
import ladysnake.requiem.api.v1.possession.PossessionComponent;
import ladysnake.requiem.api.v1.remnant.DeathSuspender;
import ladysnake.requiem.api.v1.remnant.RemnantComponent;
import ladysnake.requiem.api.v1.event.requiem.PlayerShellEvents;
import ladysnake.requiem.api.v1.event.requiem.PossessionEvents;
import ladysnake.requiem.common.entity.PlayerShellEntity;
import ladysnake.requiem.common.entity.effect.AttritionStatusEffect;
import ladysnake.requiem.common.entity.effect.RequiemStatusEffects;
import ladysnake.requiem.common.item.EmptySoulVesselItem;
import ladysnake.requiem.common.remnant.PlayerBodyTracker;
import ladysnake.requiem.common.locki.PhenexAbilitySystem;
import ladysnake.requiem.common.locki.PhenexStructureSystem;
import ladysnake.requiem.common.locki.NucleusManager;
import ladysnake.requiem.common.locki.MarbasAbilitySystem;
import ladysnake.requiem.common.remnant.PlayerSplitter;
import ladysnake.requiem.common.remnant.RemnantComponentImpl;
import ladysnake.requiem.core.record.EntityPositionClerk;
import ladysnake.requiem.common.tag.RequiemBlockTags;
import ladysnake.requiem.common.tag.RequiemEntityTypeTags;
import ladysnake.requiem.core.possession.PossessionComponentImpl;
import ladysnake.requiem.core.remnant.VagrantInteraction;
import ladysnake.requiem.core.remnant.VagrantInteractionRegistryImpl;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.TriState;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.ServerChatEvent;
import net.neoforged.neoforge.event.entity.EntityEvent;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingExperienceDropEvent;
import net.neoforged.neoforge.event.entity.living.LivingGetProjectileEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.item.ItemTossEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

public final class RequiemForgeEvents {

    private static boolean syncingDamage = false;

    private RequiemForgeEvents() {}

    public static void init() {
        NeoForge.EVENT_BUS.register(RequiemForgeEvents.class);
    }

    @SubscribeEvent
    public static void onServerChat(ServerChatEvent event) {
        NucleusManager.onChat(event);
        if (event.isCanceled()) return;
        MarbasAbilitySystem.onChat(event);
        if (event.isCanceled()) return;
        PhenexAbilitySystem.onChat(event);
    }

    @SubscribeEvent
    public static void onPhenexBlockPlaced(BlockEvent.EntityPlaceEvent event) {
        PhenexStructureSystem.onBlockPlaced(event);
    }

    @SubscribeEvent
    public static void onPhenexItemToss(ItemTossEvent event) {
        PhenexStructureSystem.onItemToss(event);
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (event.getOriginal() instanceof ServerPlayer original && event.getEntity() instanceof ServerPlayer clone) {
            boolean wasDeath = event.isWasDeath();
            boolean splitting = ((RemnantComponentImpl) RemnantComponent.get(original)).isSplitting();
            PrepareRespawnCallback.EVENT.invoker().prepareRespawn(original, clone, !wasDeath && !splitting);
        }
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            PlayerRespawnCallback.EVENT.invoker().onPlayerRespawn(player, false);
            player.onUpdateAbilities();
        }
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.getSource().getEntity() instanceof ladysnake.requiem.common.entity.ImmortalHomunculusEntity attacker
                && attacker.level() instanceof ServerLevel level) {
            // Yellow energy bursts only when the suffering form actually gets a kill.
            level.sendParticles(net.minecraft.core.particles.ParticleTypes.WAX_ON,
                attacker.getX(), attacker.getY() + attacker.getBbHeight() * 0.55D, attacker.getZ(),
                18, 0.45D, 0.5D, 0.45D, 0.035D);
        }
        NucleusManager.onLivingDeath(event);
        if (event.getEntity() instanceof ServerPlayer player) {
            if (player.level().dimension() == ladysnake.requiem.common.construct.ConstructWorkshopManager.WORKSHOP_DIMENSION) {
                event.setCanceled(true);
                player.setHealth(player.getMaxHealth());
                ladysnake.requiem.common.construct.ConstructWorkshopManager.exitWorkshop(player, null, false);
                player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§eYou returned to your anchor unharmed."));
                return;
            }
            ladysnake.requiem.common.locki.LockiSpiritSystem.onDeath(player);
            RemnantComponent remnant = RemnantComponent.get(player);
            if (!remnant.hasChosenRemnantType()) {
                DeathSuspender suspender = DeathSuspender.get(player);
                if (!suspender.isLifeTransient()) {
                    event.setCanceled(true);
                    suspender.suspendDeath(event.getSource());
                    return;
                }
            } else if (remnant.getRemnantType().isDemon() && !player.level().getLevelData().isHardcore()) {
                PossessionComponent possession = PossessionComponent.get(player);
                if (possession.isPossessionOngoing()) {
                    event.setCanceled(true);
                    Mob host = possession.getHost();
                    if (host != null) {
                        PossessionEvents.HOST_DEATH.invoker().onHostDeath(player, host, event.getSource());
                        if (!host.isDeadOrDying()) {
                            host.die(event.getSource());
                        }
                    }
                    possession.stopPossessing();
                    player.setHealth(player.getMaxHealth());
                    remnant.setVagrant(true);
                    return;
                } else if (!remnant.isVagrant()) {
                    event.setCanceled(true);
                    PlayerShellEntity shell = PlayerSplitter.createShell(player);
                    player.level().addFreshEntity(shell);
                    player.getInventory().clearContent();
                    PlayerSplitter.transferSoulboundItems(shell, player);
                    PlayerBodyTracker.get(player).setAnchor(EntityPositionClerk.get(shell).getOrCreateRecord());
                    player.setHealth(player.getMaxHealth());
                    remnant.setVagrant(true);
                    AttritionStatusEffect.apply(player);
                    PlayerShellEvents.PLAYER_SPLIT.invoker().onPlayerSplit(player, player, shell);
                    return;
                }
            }
        }

        if (event.getEntity() instanceof Mob mob && mob instanceof Possessable p && p.isBeingPossessed()) {
            Player possessor = p.getPossessor();
            if (possessor != null) {
                if (possessor instanceof ServerPlayer sp) {
                    PossessionEvents.HOST_DEATH.invoker().onHostDeath(sp, mob, event.getSource());
                }
                PossessionComponent.get(possessor).stopPossessing();
                possessor.setHealth(possessor.getMaxHealth());
                RemnantComponent.get(possessor).setVagrant(true);
            }
        }
    }

    @SubscribeEvent
    public static void onNucleusBossDrops(LivingDropsEvent event) {
        NucleusManager.onLivingDrops(event);
    }

    @SubscribeEvent
    public static void onNucleusBossExperience(LivingExperienceDropEvent event) {
        NucleusManager.onLivingExperienceDrop(event);
    }

    @SubscribeEvent
    public static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
    }

    @SubscribeEvent
    public static void onAttackEntity(AttackEntityEvent event) {
        Player player = event.getEntity();
        Entity target = event.getTarget();
        if (MarbasAbilitySystem.onAttackEntity(player, target)) {
            event.setCanceled(true);
            return;
        }
        PhenexAbilitySystem.onAttack(player, target);

        PossessionComponent possession = PossessionComponent.get(player);
        if (possession.isPossessionOngoing()) {
            Mob host = possession.getHost();
            if (target == host) {
                event.setCanceled(true);
                return;
            }
            if (host != null && target instanceof LivingEntity livingTarget) {
                if (!player.level().isClientSide() && player.level() instanceof ServerLevel serverLevel) {
                    host.doHurtTarget(serverLevel, livingTarget);
                    host.swing(InteractionHand.MAIN_HAND);
                } else {
                    host.swing(InteractionHand.MAIN_HAND);
                }
            }
            event.setCanceled(true);
            return;
        }

        if (RemnantComponent.isVagrant(player)) {
            if (target instanceof Mob mob) {
                if (!possession.isPossessionOngoing()) {
                    VagrantInteraction interaction = VagrantInteractionRegistryImpl.INSTANCE.getAction(mob, player);
                    if (interaction != null) {
                        if (!player.level().isClientSide()) {
                            interaction.action().accept(mob, player);
                        }
                    } else {
                        if (!player.level().isClientSide()) {
                            possession.startPossessing(mob);
                        }
                    }
                }
            }
            // Incorporeal / vagrant players cannot deal physical melee damage
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        Player player = event.getEntity();
        Entity target = event.getTarget();
        ItemStack held = event.getItemStack();

        if (!event.getLevel().isClientSide()
                && target instanceof ladysnake.requiem.common.entity.ImmortalHomunculusEntity) {
            // The suffering form ignores ordinary right-clicks, including ability handlers that
            // would otherwise emit capture/interaction messages.
            event.setCancellationResult(InteractionResult.PASS);
            event.setCanceled(true);
            return;
        }

        if (!event.getLevel().isClientSide() && MarbasAbilitySystem.onEntityInteract(player, target, held, player.isShiftKeyDown())) {
            event.setCancellationResult(InteractionResult.SUCCESS);
            event.setCanceled(true);
            return;
        }
        if (!event.getLevel().isClientSide() && PhenexAbilitySystem.onEntityInteract(player, target)) {
            event.setCancellationResult(InteractionResult.SUCCESS);
            event.setCanceled(true);
            return;
        }

        if (held.getItem() instanceof EmptySoulVesselItem) {
            return;
        }

        if (RemnantComponent.isVagrant(player)) {
            if (target instanceof PlayerShellEntity shell) {
                if (!event.getLevel().isClientSide()) {
                    RemnantComponent.get(player).merge(shell);
                }
                event.setCancellationResult(InteractionResult.SUCCESS);
                event.setCanceled(true);
                return;
            }

            if (target instanceof LivingEntity living) {
                VagrantInteraction interaction = VagrantInteractionRegistryImpl.INSTANCE.getAction(living, player);
                if (interaction != null) {
                    if (!event.getLevel().isClientSide()) {
                        interaction.action().accept(living, player);
                    }
                    event.setCancellationResult(InteractionResult.SUCCESS);
                    event.setCanceled(true);
                    return;
                } else if (living instanceof Mob mob) {
                    PossessionComponent possession = PossessionComponent.get(player);
                    if (!possession.isPossessionOngoing()) {
                        if (!event.getLevel().isClientSide()) {
                            possession.startPossessing(mob);
                        }
                        event.setCancellationResult(InteractionResult.SUCCESS);
                        event.setCanceled(true);
                        return;
                    }
                }
            }
            event.setCancellationResult(InteractionResult.FAIL);
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        Player player = event.getEntity();
        if (RemnantComponent.isIncorporeal(player) && !player.isCreative()) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onBreakBlock(BreakBlockEvent event) {
        Player player = event.getPlayer();
        if (player != null && RemnantComponent.isIncorporeal(player) && !player.isCreative()) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getEntity();
        if (!event.getLevel().isClientSide() && player instanceof ServerPlayer serverPlayer) {
            // This ritual bucket must never fall through to vanilla fluid/block placement.
            if (MarbasAbilitySystem.isSulfurBucket(event.getItemStack())) {
                boolean handled = MarbasAbilitySystem.onUseBlock(
                    serverPlayer, event.getItemStack(), event.getPos(), player.isShiftKeyDown());
                if (!handled) serverPlayer.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                    "The sulfur cube bucket cannot place a block. Use it on the prepared water cauldron as Marbas."));
                event.setCancellationResult(InteractionResult.SUCCESS);
                event.setCanceled(true);
                return;
            }
            if (MarbasAbilitySystem.tryPlaceStoredHomunculus(
                    serverPlayer, event.getItemStack(), event.getPos().relative(event.getFace()))) {
                event.setCancellationResult(InteractionResult.SUCCESS);
                event.setCanceled(true);
                return;
            }
            if (NucleusManager.tryEnter(serverPlayer, event.getPos())) {
                event.setCancellationResult(InteractionResult.SUCCESS);
                event.setCanceled(true);
                return;
            }
            if (PhenexAbilitySystem.onUseBlock(serverPlayer, event.getItemStack(), event.getPos(), player.isShiftKeyDown())) {
                event.setCancellationResult(InteractionResult.SUCCESS);
                event.setCanceled(true);
                return;
            }
            if (MarbasAbilitySystem.onUseBlock(serverPlayer, event.getItemStack(), event.getPos(), player.isShiftKeyDown())) {
                event.setCancellationResult(InteractionResult.SUCCESS);
                event.setCanceled(true);
                return;
            }
        }
        if (RemnantComponent.isIncorporeal(player) && !player.isCreative()) {
            BlockState state = event.getLevel().getBlockState(event.getPos());
            if (!state.is(RequiemBlockTags.SOUL_INTERACTABLE)) {
                event.setCancellationResult(InteractionResult.FAIL);
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        Player player = event.getEntity();
        if (!event.getLevel().isClientSide() && player instanceof ServerPlayer serverPlayer
                && PhenexAbilitySystem.onUseItem(serverPlayer, event.getItemStack(), player.isShiftKeyDown())) {
            event.setCancellationResult(InteractionResult.SUCCESS);
            event.setCanceled(true);
            return;
        }
        // Player heads are food for Locki only while her astral sight is active.
        if (!event.getLevel().isClientSide() && player instanceof ServerPlayer serverPlayer
                && ladysnake.requiem.common.locki.LockiSpiritSystem.isAstralVisionActive(serverPlayer)
                && event.getItemStack().is(Items.PLAYER_HEAD)) {
            if (!serverPlayer.isCreative()) event.getItemStack().shrink(1);
            serverPlayer.addEffect(new MobEffectInstance(RequiemStatusEffects.WHITE_VISION, 20 * 60 * 10, 0, false, true, true));
            serverPlayer.playSound(SoundEvents.GENERIC_EAT.value(), 0.8F, 0.65F);
            serverPlayer.sendSystemMessage(net.minecraft.network.chat.Component.literal("White Vision awakens. Corruption appears in yellow; the astral sky shifts to magenta."));
            event.setCancellationResult(InteractionResult.SUCCESS);
            event.setCanceled(true);
            return;
        }
        if (RemnantComponent.isIncorporeal(player) && !player.isCreative()) {
            ItemStack stack = event.getItemStack();
            if (!(stack.getItem() instanceof EmptySoulVesselItem)
                    && !(stack.getItem() instanceof ladysnake.requiem.common.item.FilledSoulVesselItem)
                    && !(stack.getItem() instanceof ladysnake.requiem.common.item.DemonSoulVesselItem)
                    && !(stack.getItem() instanceof ladysnake.requiem.common.item.IchorVesselItem)) {
                event.setCancellationResult(InteractionResult.FAIL);
                event.setCanceled(true);
                return;
            }
        }

        PossessionComponent possession = PossessionComponent.get(player);
        if (possession.isPossessionOngoing()) {
            var override = ladysnake.requiem.common.possession.item.PossessionItemOverrideWrapper.tryUseOverride(event.getLevel(), player, event.getItemStack(), event.getHand());
            if (override.isPresent()) {
                event.setCancellationResult(override.get());
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public static void onItemUseFinish(net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent.Finish event) {
        if (event.getEntity() instanceof Player player) {
            ItemStack stack = event.getItem();
            PossessionComponent possession = PossessionComponent.get(player);
            if (possession.isPossessionOngoing()) {
                var override = ladysnake.requiem.common.possession.item.PossessionItemOverrideWrapper.tryFinishUsingOverride(player.level(), player, stack, player.getUsedItemHand());
                if (override.isPresent() && override.get().consumesAction()) {
                    return;
                }

                Mob host = possession.getHost();
                if (host != null && (host.hasEffect(net.minecraft.world.effect.MobEffects.WEAKNESS) || player.hasEffect(net.minecraft.world.effect.MobEffects.WEAKNESS))) {
                    if (stack.is(ladysnake.requiem.core.tag.RequiemCoreTags.Item.UNDEAD_CURES) && RemnantComponent.get(player).canCurePossessed(host)) {
                        possession.startCuring();
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public static void onRightClickEmpty(PlayerInteractEvent.RightClickEmpty event) {
    }

    @SubscribeEvent
    public static void onItemPickup(ItemEntityPickupEvent.Pre event) {
        NucleusManager.onPickupPre(event);
        Player player = event.getPlayer();
        if (RemnantComponent.isIncorporeal(player)) {
            event.setCanPickup(TriState.FALSE);
        }
    }

    @SubscribeEvent
    public static void onLivingIncomingDamage(LivingIncomingDamageEvent event) {
        if (MarbasAbilitySystem.onBerserkDamage(event)) return;
        if (event.getEntity() instanceof Player player) {
            if (RemnantComponent.isIncorporeal(player)) {
                if (!event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
                    event.setCanceled(true);
                }
                return;
            }
            if (event.getSource().is(net.minecraft.world.damagesource.DamageTypes.IN_WALL) && MovementAlterer.get(player).isNoClipping()) {
                event.setCanceled(true);
                return;
            }
            PossessionComponent possession = PossessionComponent.get(player);
            if (possession.isPossessionOngoing()) {
                Mob host = possession.getHost();
                if (host != null && host.isAlive()) {
                    if (event.getSource().is(DamageTypeTags.IS_FALL)) {
                        if (host.getType().builtInRegistryHolder().is(net.minecraft.tags.EntityTypeTags.FALL_DAMAGE_IMMUNE)
                                || host.getType() == EntityTypes.CHICKEN
                                || host.getType() == EntityTypes.IRON_GOLEM
                                || host.isNoGravity()
                                || (host.getAttributes().hasAttribute(net.minecraft.world.entity.ai.attributes.Attributes.FALL_DAMAGE_MULTIPLIER)
                                    && host.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.FALL_DAMAGE_MULTIPLIER) <= 0.0)) {
                            event.setCanceled(true);
                            return;
                        }
                    }
                    if (!player.level().isClientSide() && !syncingDamage) {
                        try {
                            syncingDamage = true;
                            host.hurt(event.getSource(), event.getAmount());
                            if (host.isDeadOrDying() || !host.isAlive()) {
                                if (player instanceof ServerPlayer sp) {
                                    PossessionEvents.HOST_DEATH.invoker().onHostDeath(sp, host, event.getSource());
                                }
                                possession.stopPossessing();
                                player.setHealth(player.getMaxHealth());
                                RemnantComponent.get(player).setVagrant(true);
                            } else {
                                player.setHealth(Math.min(host.getHealth(), player.getMaxHealth()));
                            }
                        } finally {
                            syncingDamage = false;
                        }
                    }
                    event.setCanceled(true);
                    return;
                }
            }
        } else if (event.getEntity() instanceof Mob host && host instanceof Possessable p && p.isBeingPossessed()) {
            Player possessor = p.getPossessor();
            if (possessor != null && !possessor.level().isClientSide()) {
                if (event.getAmount() >= host.getHealth() || host.isDeadOrDying()) {
                    if (possessor instanceof ServerPlayer sp) {
                        PossessionEvents.HOST_DEATH.invoker().onHostDeath(sp, host, event.getSource());
                    }
                    PossessionComponent.get(possessor).stopPossessing();
                    possessor.setHealth(possessor.getMaxHealth());
                    RemnantComponent.get(possessor).setVagrant(true);
                } else if (!syncingDamage) {
                    float newHealth = Math.max(1.0F, host.getHealth() - event.getAmount());
                    possessor.setHealth(Math.min(newHealth, possessor.getMaxHealth()));
                }
            }
        }
    }

    @SubscribeEvent
    public static void onEntityJoinLevel(net.neoforged.neoforge.event.entity.EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof Mob mob)) return;

        // Restore brain-only control first; do not let a previously saved soul goal set override it.
        if (MarbasAbilitySystem.hasTransferredBrain(mob)) {
            MarbasAbilitySystem.activateTransferredBrain(mob);
            return;
        }

        if (mob instanceof net.minecraft.world.entity.PathfinderMob pathfinderMob
                && ladysnake.requiem.common.construct.ai.SoulAiManager.hasInjectedSoul(pathfinderMob)) {
            String soulType = ladysnake.requiem.common.construct.ai.SoulAiManager.getInjectedSoulType(pathfinderMob);
            java.util.UUID owner = ladysnake.requiem.core.util.NbtCompat.getUuid(mob.getPersistentData(), "requiem:soul_owner").orElse(null);
            ladysnake.requiem.common.construct.ai.SoulAiManager.applyInjectedGoals(pathfinderMob, soulType, owner);
        }
    }

    @SubscribeEvent
    public static void onLivingChangeTarget(LivingChangeTargetEvent event) {
        if (event.getNewAboutToBeSetTarget() instanceof Player player && RemnantComponent.isIncorporeal(player)) {
            event.setNewAboutToBeSetTarget(null);
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        PossessionComponent possession = PossessionComponent.get(player);
        MovementAlterer alterer = MovementAlterer.get(player);

        if (player.level().isClientSide()) {
            alterer.clientTick();
        } else {
            alterer.serverTick();
        }

        if (alterer.isNoClipping()) {
            player.noPhysics = true;
        }

        if (!player.level().isClientSide() && player instanceof ServerPlayer sp && sp.level().dimension() == ladysnake.requiem.common.construct.ConstructWorkshopManager.WORKSHOP_DIMENSION) {
            if (!sp.getAbilities().mayfly || !sp.getAbilities().flying) {
                sp.getAbilities().mayfly = true;
                sp.getAbilities().flying = true;
                sp.onUpdateAbilities();
            }
            if (sp.getY() < 20) {
                ladysnake.requiem.common.construct.ConstructWorkshopManager.exitWorkshop(sp, null, false);
                sp.sendSystemMessage(net.minecraft.network.chat.Component.literal("§eYou fell into the void and returned to your anchor unharmed."));
            }
        }

        if (possession.isPossessionOngoing()) {
            Mob host = possession.getHost();
            if (host != null) {
                if (!player.isCreative() && !player.isSpectator()) {
                    boolean hostFlies = PossessionComponentImpl.canHostFly(host);
                    if (player.getAbilities().mayfly != hostFlies) {
                        player.getAbilities().mayfly = hostFlies;
                        if (!hostFlies) {
                            player.getAbilities().flying = false;
                        }
                        if (player instanceof ServerPlayer sp) {
                            sp.onUpdateAbilities();
                        }
                    }
                }

                boolean slowFall = host.getType().builtInRegistryHolder().is(net.minecraft.tags.EntityTypeTags.FALL_DAMAGE_IMMUNE)
                        || host.getType() == EntityTypes.CHICKEN
                        || host.getType() == EntityTypes.IRON_GOLEM
                        || host.isNoGravity()
                        || (host.getAttributes().hasAttribute(net.minecraft.world.entity.ai.attributes.Attributes.FALL_DAMAGE_MULTIPLIER)
                            && host.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.FALL_DAMAGE_MULTIPLIER) <= 0.0);

                if (slowFall) {
                    player.resetFallDistance();
                    // Automatic slow falling glide (chicken-like descent)
                    if (!player.onGround() && player.getDeltaMovement().y < 0.0) {
                        player.setDeltaMovement(player.getDeltaMovement().multiply(1.0, 0.6, 1.0));
                    }
                }

                // Automatic fire immunity from host
                if (host.fireImmune() && player.isOnFire()) {
                    player.clearFire();
                }

                // Automatic water breathing / undead infinite lung capacity
                if (host.canBreatheUnderwater() || host.getType().builtInRegistryHolder().is(net.minecraft.tags.EntityTypeTags.UNDEAD)) {
                    player.setAirSupply(player.getMaxAirSupply());
                }

                // Automatic step height inheritance (horses, golems, large mobs)
                if (host.getAttributes().hasAttribute(net.minecraft.world.entity.ai.attributes.Attributes.STEP_HEIGHT)) {
                    double hostStep = host.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.STEP_HEIGHT);
                    var playerStep = player.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.STEP_HEIGHT);
                    if (playerStep != null && Math.abs(playerStep.getBaseValue() - hostStep) > 0.01) {
                        playerStep.setBaseValue(hostStep);
                    }
                }

                if ((host.getType() == EntityTypes.SPIDER || host.getType() == EntityTypes.CAVE_SPIDER || host.onClimbable()) && player.horizontalCollision) {
                    net.minecraft.world.phys.Vec3 delta = player.getDeltaMovement();
                    player.setDeltaMovement(delta.x, 0.2, delta.z);
                }

                if (!player.level().isClientSide() && Math.abs(player.getHealth() - host.getHealth()) > 0.05F) {
                    player.setHealth(Math.min(host.getHealth(), player.getMaxHealth()));
                }
            }
        } else if (RemnantComponent.isIncorporeal(player)) {
            if (!player.isCreative() && !player.isSpectator()
                    && !ladysnake.requiem.common.locki.LockiSpiritSystem.shouldSuppressFlight(player)) {
                if (!player.getAbilities().mayfly) {
                    player.getAbilities().mayfly = true;
                    if (player instanceof ServerPlayer sp) {
                        sp.onUpdateAbilities();
                    }
                }
            }
            if (player.getAbilities().flying) {
                player.resetFallDistance();
            }
        } else if (!RemnantComponent.isIncorporeal(player) && !player.isCreative() && !player.isSpectator()) {
            var flightAttr = player.getAttribute(net.neoforged.neoforge.common.NeoForgeMod.CREATIVE_FLIGHT);
            if (flightAttr != null && flightAttr.hasModifier(io.github.ladysnake.pal.VanillaAbilities.CREATIVE_FLIGHT_MODIFIER_ID)) {
                flightAttr.removeModifier(io.github.ladysnake.pal.VanillaAbilities.CREATIVE_FLIGHT_MODIFIER_ID);
            }
            if (player.getAbilities().mayfly) {
                player.getAbilities().mayfly = false;
                player.getAbilities().flying = false;
                if (player instanceof ServerPlayer sp) {
                    sp.onUpdateAbilities();
                }
            }
        }

        ladysnake.requiem.common.locki.LockiSpiritSystem.tick(player);
        if (!player.level().isClientSide() && player instanceof ServerPlayer serverPlayer && player.level() instanceof ServerLevel serverLevel) {
            PhenexAbilitySystem.tick(serverPlayer);
            MarbasAbilitySystem.onPlayerTick(serverPlayer);
            MarbasAbilitySystem.tick(serverLevel, serverPlayer.blockPosition());
        }
    }

    @SubscribeEvent
    public static void onChangeGameMode(PlayerEvent.PlayerChangeGameModeEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp && sp.level().getServer() != null) {
            sp.level().getServer().execute(() -> {
                if (RemnantComponent.isIncorporeal(sp)) {
                    sp.getAbilities().mayfly = true;
                    sp.onUpdateAbilities();
                }
            });
        }
    }

    @SubscribeEvent
    public static void onLivingGetProjectile(LivingGetProjectileEvent event) {
        if (event.getEntity() instanceof Player player) {
            if (event.getProjectileItemStack().isEmpty()) {
                Mob host = PossessionComponent.getHost(player);
                if (host != null && (host.getType().builtInRegistryHolder().is(RequiemEntityTypeTags.ARROW_GENERATORS) || host.getType().builtInRegistryHolder().is(RequiemEntityTypeTags.SKELETONS))) {
                    event.setProjectileItemStack(new ItemStack(Items.ARROW));
                }
            }
        }
    }

    @SubscribeEvent
    public static void onEntitySize(EntityEvent.Size event) {
        if (event.getEntity() instanceof Player player) {
            Mob host = PossessionComponent.getHost(player);
            if (host != null) {
                event.setNewSize(host.getDimensions(event.getPose()));
            }
        }
    }
}
