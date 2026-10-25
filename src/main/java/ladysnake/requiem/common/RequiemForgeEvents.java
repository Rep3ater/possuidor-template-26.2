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
import ladysnake.requiem.common.entity.PlayerShellEntity;
import ladysnake.requiem.common.item.EmptySoulVesselItem;
import ladysnake.requiem.common.remnant.RemnantComponentImpl;
import ladysnake.requiem.common.tag.RequiemBlockTags;
import ladysnake.requiem.common.tag.RequiemEntityTypeTags;
import ladysnake.requiem.core.possession.PossessionComponentImpl;
import ladysnake.requiem.core.remnant.VagrantInteraction;
import ladysnake.requiem.core.remnant.VagrantInteractionRegistryImpl;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
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
import net.neoforged.neoforge.event.entity.EntityEvent;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingGetProjectileEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
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
        if (event.getEntity() instanceof ServerPlayer player) {
            RemnantComponent remnant = RemnantComponent.get(player);
            if (!remnant.hasChosenRemnantType()) {
                DeathSuspender suspender = DeathSuspender.get(player);
                if (!suspender.isLifeTransient()) {
                    event.setCanceled(true);
                    suspender.suspendDeath(event.getSource());
                    return;
                }
            }
        }

        if (event.getEntity() instanceof Mob mob && mob instanceof Possessable p && p.isBeingPossessed()) {
            Player possessor = p.getPossessor();
            if (possessor != null) {
                PossessionComponent.get(possessor).stopPossessing();
            }
        }
    }

    @SubscribeEvent
    public static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
    }

    @SubscribeEvent
    public static void onAttackEntity(AttackEntityEvent event) {
        Player player = event.getEntity();
        Entity target = event.getTarget();

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
        if (RemnantComponent.isIncorporeal(player) && !player.isCreative()) {
            ItemStack stack = event.getItemStack();
            if (!(stack.getItem() instanceof EmptySoulVesselItem)) {
                event.setCancellationResult(InteractionResult.FAIL);
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public static void onRightClickEmpty(PlayerInteractEvent.RightClickEmpty event) {
    }

    @SubscribeEvent
    public static void onItemPickup(ItemEntityPickupEvent.Pre event) {
        Player player = event.getPlayer();
        if (RemnantComponent.isIncorporeal(player)) {
            event.setCanPickup(TriState.FALSE);
        }
    }

    @SubscribeEvent
    public static void onLivingIncomingDamage(LivingIncomingDamageEvent event) {
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
                            player.setHealth(Math.min(host.getHealth(), player.getMaxHealth()));
                        } finally {
                            syncingDamage = false;
                        }
                    }
                }
            }
        } else if (event.getEntity() instanceof Mob host && host instanceof Possessable p && p.isBeingPossessed()) {
            Player possessor = p.getPossessor();
            if (possessor != null && !possessor.level().isClientSide() && !syncingDamage) {
                try {
                    syncingDamage = true;
                    possessor.hurt(event.getSource(), event.getAmount());
                } finally {
                    syncingDamage = false;
                }
            }
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
            if (!player.isCreative() && !player.isSpectator()) {
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
            if (player.getAbilities().mayfly) {
                player.getAbilities().mayfly = false;
                player.getAbilities().flying = false;
                if (player instanceof ServerPlayer sp) {
                    sp.onUpdateAbilities();
                }
            }
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
