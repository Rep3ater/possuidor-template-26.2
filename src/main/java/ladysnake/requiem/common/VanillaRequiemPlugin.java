/*
 * Requiem
 * Copyright (C) 2017-2023 Ladysnake
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses>.
 *
 * Linking this mod statically or dynamically with other
 * modules is making a combined work based on this mod.
 * Thus, the terms and conditions of the GNU General Public License cover the whole combination.
 *
 * In addition, as a special exception, the copyright holders of
 * this mod give you permission to combine this mod
 * with free software programs or libraries that are released under the GNU LGPL
 * and with code included in the standard release of Minecraft under All Rights Reserved (or
 * modified versions of such code, with unchanged license).
 * You may copy and distribute such a system following the terms of the GNU GPL for this mod
 * and the licenses of the other code concerned.
 *
 * Note that people who make modified versions of this mod are not obligated to grant
 * this special exception for their modified versions; it is their choice whether to do so.
 * The GNU General Public License gives permission to release a modified version without this exception;
 * this exception also makes it possible to release a modified version which carries forward this exception.
 */
package ladysnake.requiem.common;

import baritone.api.fakeplayer.AutomatoneFakePlayer;
import com.mojang.authlib.GameProfile;
import io.github.ladysnake.impersonate.Impersonate;
import io.github.ladysnake.impersonate.Impersonator;
import io.github.ladysnake.locki.DefaultInventoryNodes;
import io.github.ladysnake.locki.ModdedInventoryNodes;
import ladysnake.requiem.Requiem;
import ladysnake.requiem.api.v1.RequiemPlugin;
import ladysnake.requiem.api.v1.entity.CurableEntityComponent;
import ladysnake.requiem.api.v1.entity.InventoryLimiter;
import ladysnake.requiem.api.v1.entity.ability.AbilityType;
import ladysnake.requiem.api.v1.entity.ability.MobAbilityConfig;
import ladysnake.requiem.api.v1.entity.ability.MobAbilityController;
import ladysnake.requiem.api.v1.entity.ability.MobAbilityRegistry;
import ladysnake.requiem.api.v1.event.minecraft.AllowUseEntityCallback;
import ladysnake.requiem.api.v1.event.minecraft.*;
import ladysnake.requiem.api.v1.event.requiem.*;
import ladysnake.requiem.api.v1.possession.PossessedData;
import ladysnake.requiem.api.v1.possession.PossessionComponent;
import ladysnake.requiem.api.v1.possession.item.PossessionItemAction;
import ladysnake.requiem.api.v1.remnant.*;
import ladysnake.requiem.common.advancement.criterion.RequiemCriteria;
import ladysnake.requiem.common.enchantment.RequiemEnchantments;
import ladysnake.requiem.common.entity.PlayerShellEntity;
import ladysnake.requiem.common.entity.RequiemEntities;
import ladysnake.requiem.common.entity.SkeletonBoneComponent;
import ladysnake.requiem.common.entity.ability.*;
import ladysnake.requiem.common.entity.effect.ReclamationStatusEffect;
import ladysnake.requiem.common.entity.effect.RequiemStatusEffects;
import ladysnake.requiem.common.gamerule.PossessionDetection;
import ladysnake.requiem.common.gamerule.RequiemGamerules;
import ladysnake.requiem.common.network.RequiemNetworking;
import ladysnake.requiem.common.possession.MobRidingType;
import ladysnake.requiem.common.remnant.BasePossessionHandlers;
import ladysnake.requiem.common.remnant.MortalDysmorphiaDamageSource;
import ladysnake.requiem.common.remnant.PlayerSplitter;
import ladysnake.requiem.common.remnant.RemnantTypes;
import ladysnake.requiem.common.tag.RequiemBlockTags;
import ladysnake.requiem.common.tag.RequiemEntityTypeTags;
import ladysnake.requiem.core.ability.PlayerAbilityController;
import ladysnake.requiem.core.entity.SoulHolderComponent;
import ladysnake.requiem.core.entity.ability.AutoAimAbility;
import ladysnake.requiem.core.entity.ability.DelegatingDirectAbility;
import ladysnake.requiem.core.entity.ability.RangedAttackAbility;
import ladysnake.requiem.core.entity.ability.SnowmanSnowballAbility;
import ladysnake.requiem.core.record.EntityPositionClerk;
import ladysnake.requiem.core.resurrection.ResurrectionDataLoader;
import ladysnake.requiem.core.tag.RequiemCoreTags;
import ladysnake.requiem.core.util.RayHelper;
import net.fabricmc.fabric.api.entity.event.v1.EntitySleepEvents;
import net.fabricmc.fabric.api.event.player.*;
import net.fabricmc.fabric.api.util.TriState;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.network.protocol.game.ClientboundUpdateMobEffectPacket;
import net.minecraft.core.Registry;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class VanillaRequiemPlugin implements RequiemPlugin {

    public static final String INFINITY_SHOT_TAG = "requiem:infinity_shot";
    public static final MobAbilityConfig<Player> SOUL_ABILITY_CONFIG = MobAbilityConfig.<Player>builder()
        .directAttack(player -> new DelegatingDirectAbility<>(player, LivingEntity.class, AbilityType.INTERACT))
        .directInteract(VagrantPossessAbility::new)
        .build();

    @Override
    public void onRequiemInitialize() {
        registerEtherealEventHandlers();
        registerPossessionEventHandlers();
        EntityPositionClerk.registerCallbacks();
        ReclamationStatusEffect.registerEventHandlers();
        CanCurePossessedCallback.EVENT.register((body) -> {
            CurableEntityComponent curableEntityComponent = CurableEntityComponent.KEY.get(body);
            return (curableEntityComponent.canBeCured() || curableEntityComponent.canBeAssimilated()) ? TriState.TRUE : TriState.DEFAULT;
        });
        LivingEntityDropCallback.EVENT.register((dead, deathCause) -> {
            if (!(dead instanceof ServerPlayer lazarus)) {
                return false;
            }
            Mob secondLife = ResurrectionDataLoader.INSTANCE.getNextBody(lazarus, deathCause);
            if (secondLife != null) {
                ((MobResurrectable) lazarus).setResurrectionEntity(secondLife);
                return RemnantComponent.get(lazarus).getRemnantType().isDemon();
            }
            return false;
        });
        LivingEntityDropCallback.EVENT.register((dead, deathCause) -> {
            PossessedData.KEY.maybeGet(dead).ifPresent(PossessedData::dropItems);
            return false;
        });
        InitiateFractureCallback.EVENT.register(player -> {
            RemnantComponent remnantComponent = RemnantComponent.get(player);
            PossessionComponent possessionComponent = PossessionComponent.get(player);
            Mob host = possessionComponent.getHost();

            boolean success;

            if (remnantComponent.splitPlayer(true).isPresent()) {
                success = true;
            } else if (host != null) {
                Entity targetedEntity = RayHelper.getTargetedEntity(player);
                if (targetedEntity instanceof PlayerShellEntity shell && Objects.equals(player.getUUID(), ((PlayerShellEntity) targetedEntity).getOwnerUuid())) {
                    possessionComponent.stopPossessing();
                    remnantComponent.merge(shell);
                    RequiemNetworking.sendBodyCureMessage(player);
                    success = true;
                } else if (remnantComponent.canDissociateFrom(host)) {
                    possessionComponent.stopPossessing();
                    success = true;
                } else {
                    success = false;
                }
            } else {
                success = false;
            }

            if (success) {
                RequiemNetworking.sendEtherealAnimationMessage(player);
            }

            return success;
        });
        HumanityCheckCallback.EVENT.register(possessedEntity -> possessedEntity.level().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).get(RequiemEnchantments.HUMANITY).map(holder -> EnchantmentHelper.getEnchantmentLevel(holder, possessedEntity)).orElse(0));
        ConsumableItemEvents.POST_CONSUMED.register(RequiemCriteria.USED_TOTEM::trigger);
        SoulCaptureEvents.BEFORE_ATTEMPT.register((stealer, target, captureType) -> Optional.ofNullable(stealer.getEffect(RequiemStatusEffects.ATTRITION)).map(MobEffectInstance::getAmplifier).orElse(-1) < 3);
        SoulCaptureEvents.BEFORE_ATTEMPT.register((stealer, target, captureType) -> {
            if (stealer instanceof Player player && (player.isCreative() || player.getMainHandItem().getItem() instanceof ladysnake.requiem.common.item.CreativeSoulVesselItem || player.getOffhandItem().getItem() instanceof ladysnake.requiem.common.item.CreativeSoulVesselItem)) {
                return true;
            }
            return !SoulHolderComponent.isSoulless(target);
        });
    }

    private void registerEtherealEventHandlers() {
        // Prevent incorporeal players from breaking anything
        AttackBlockCallback.EVENT.register((player, world, hand, blockPos, facing) -> getInteractionResult(player));
        // Prevent incorporeal players from hitting anything
        AttackEntityCallback.EVENT.register((player, world, hand, target, hitResult) -> {
            // Proxy melee attacks
            InteractionResult result = MobAbilityController.get(player).useDirect(AbilityType.ATTACK, target);
            if (result.consumesAction()) {
                if (result instanceof InteractionResult.Success success && success.swingSource() != InteractionResult.SwingSource.NONE) {
                    player.resetAttackStrengthTicker();
                }
                return InteractionResult.SUCCESS;
            }
            return isInteractionForbidden(player, true) ? InteractionResult.FAIL : InteractionResult.PASS;
        });
        // Prevent incorporeal players from interacting with anything
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (isInteractionForbidden(player) && !world.getBlockState(hitResult.getBlockPos()).is(RequiemBlockTags.SOUL_INTERACTABLE)) {
                return InteractionResult.FAIL;
            }
            return InteractionResult.PASS;
        });
        AllowUseEntityCallback.EVENT.register((player, world, hand, target) -> !isInteractionForbidden(player));
        UseItemCallback.EVENT.register((player, world, hand) -> getInteractionResult(player));
        // Make players respawn in the right place with the right state
        PrepareRespawnCallback.EVENT.register((original, clone, returnFromEnd) -> {
            if (original.isDeadOrDying()) resetIdentity(clone);
            RemnantComponent.get(clone).prepareRespawn(original, returnFromEnd);
        });
        PlayerRespawnCallback.EVENT.register(((player, returnFromEnd) -> {
            player.onUpdateAbilities();
            if (player instanceof MobResurrectable mr) {
                mr.spawnResurrectionEntity();
            }

            // effects do not normally get synced after respawn, so we do it ourselves
            if (player instanceof ServerPlayer sp) {
                for (MobEffectInstance effect : player.getActiveEffects()) {
                    sp.connection.send(new ClientboundUpdateMobEffectPacket(player.getId(), effect, false));
                }
            }
        }));
        RemnantStateChangeCallback.EVENT.register((player, remnant, cause) -> {
            if (cause.isCharacterSwitch()) resetIdentity(player);

            Mob possessed = PossessionComponent.getHost(player);

            if (!remnant.isVagrant()) {
                PossessionComponent.get(player).stopPossessing(false);
                InventoryLimiter.instance().disable(player);
            } else {
                refreshInventoryLocks(player, possessed);
            }

            if (possessed != null) {
                PlayerAbilityController.get(player).usePossessedAbilities(possessed);
            } else {
                PlayerAbilityController.get(player).resetAbilities(remnant.isIncorporeal());
            }
        });
        RemnantTypeChangeCallback.EVENT.register((player, oldType, newType) -> {
            if (newType == RemnantTypes.MORTAL) {
                GameProfile bodyIdentity = Impersonator.get(player).getImpersonatedProfile(PlayerSplitter.BODY_IMPERSONATION);
                if (bodyIdentity != null) {
                    player.hurt(new MortalDysmorphiaDamageSource(player.level().damageSources().generic().typeHolder(), Component.literal(player.getGameProfile().name()), Component.literal(bodyIdentity.name())), 100F);
                }
            }
        });
    }

    private static void resetIdentity(Player player) {
        GameProfile previouslyImpersonated = Impersonate.IMPERSONATION.get(player).stopImpersonation(PlayerSplitter.BODY_IMPERSONATION);
        if (previouslyImpersonated != null && player instanceof ServerPlayer sp) {
            PlayerShellEvents.RESET_IDENTITY.invoker().resetIdentity(sp, previouslyImpersonated);
        }
    }

    @Nonnull
    private InteractionResult getInteractionResult(Player player) {
        return isInteractionForbidden(player) ? InteractionResult.FAIL : InteractionResult.PASS;
    }

    private boolean isInteractionForbidden(Player player) {
        return isInteractionForbidden(player, false);
    }

    private boolean isInteractionForbidden(Player player, boolean includeSouls) {
        RemnantComponent c = RemnantComponent.get(player);
        return !player.isCreative() && ((includeSouls && c.isVagrant()) || c.isIncorporeal()) || DeathSuspender.get(player).isLifeTransient();
    }

    private void registerPossessionEventHandlers() {
        BasePossessionHandlers.register();
        UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            LivingEntity possessed = PossessionComponent.get(player).getHost();
            if (possessed != null && MobRidingType.get(entity, possessed).canMount()) {
                if (!world.isClientSide()) {
                    possessed.startRiding(entity);
                }
                return InteractionResult.SUCCESS;
            }
            return InteractionResult.PASS;
        });
        MobTravelRidingCallback.EVENT.register((mount, rider) -> {
            Mob possessedEntity = PossessionComponent.getHost(rider);
            return possessedEntity != null && MobRidingType.get(mount, possessedEntity).canSteer();
        });
        UseItemCallback.EVENT.register((player, world, hand) -> {
            LivingEntity possessed = PossessionComponent.getHost(player);
            if (possessed != null && !possessed.getType().builtInRegistryHolder().is(RequiemCoreTags.Entity.ITEM_USERS) && !player.isCreative()) {
                return InteractionResult.FAIL;
            }
            return InteractionResult.PASS;
        });
        PossessionStateChangeCallback.EVENT.register(((player, possessed) -> {
            if (player instanceof ServerPlayer serverPlayer && possessed != null) {
                RequiemCriteria.PLAYER_POSSESSED_ENTITY.handle(serverPlayer, possessed);
            }
        }));
        PossessionStateChangeCallback.EVENT.register((player, possessed) -> {
                refreshInventoryLocks(player, possessed);

                if (possessed == null) {
                    PlayerAbilityController.get(player).resetAbilities(RemnantComponent.isIncorporeal(player));
                } else {
                    PlayerAbilityController.get(player).usePossessedAbilities(possessed);

                    if (!possessed.level().isClientSide()) {
                        PossessedData.KEY.get(possessed).giftFirstPossessionLoot(player);
                    }
                }

            }
        );
        EntitySleepEvents.ALLOW_SLEEPING.register((player, pos) -> {
            Mob host = PossessionComponent.getHost(player);
            if (host != null && !host.getType().builtInRegistryHolder().is(RequiemCoreTags.Entity.SLEEPERS)) {
                player.sendSystemMessage(Component.translatable("requiem:block.minecraft.bed.invalid_body"));
                return Player.BedSleepingProblem.OTHER_PROBLEM;
            }
            return null;
        });
        PossessionEvents.DETECTION_ATTEMPT.register((sensed, sensor, reason) -> {
            PossessionDetection cfg = sensed.level().getServer() != null ? sensed.level().getServer().getGameRules().get(RequiemGamerules.POSSESSION_DETECTION) : PossessionDetection.NORMAL;
            return switch (reason) {
                case BUMP, ATTACKING -> switch (cfg) {
                    case DISABLED -> PossessionEvents.DetectionAttempt.DetectionResult.UNDETECTED;
                    case NORMAL -> PossessionEvents.DetectionAttempt.DetectionResult.DETECTED;
                    case HARD -> PossessionEvents.DetectionAttempt.DetectionResult.CROWD_DETECTED;
                };
                case ATTACKED -> switch (cfg) {
                    case DISABLED -> PossessionEvents.DetectionAttempt.DetectionResult.UNDETECTED;
                    case NORMAL, HARD -> PossessionEvents.DetectionAttempt.DetectionResult.CROWD_DETECTED;
                };
            };
        });
    }

    private void refreshInventoryLocks(Player player, @Nullable Mob possessed) {
        InventoryLimiter inventoryLimiter = InventoryLimiter.instance();
        if (!player.level().isClientSide()) {
            inventoryLimiter.enable(player);

            if (possessed != null) {
                if (possessed.getType().builtInRegistryHolder().is(RequiemCoreTags.Entity.INVENTORY_CARRIERS)) {
                    inventoryLimiter.unlock(player, DefaultInventoryNodes.MAIN_INVENTORY);
                }
                if (canUseItems(possessed)) {
                    inventoryLimiter.unlock(player, DefaultInventoryNodes.HANDS);
                    inventoryLimiter.unlock(player, DefaultInventoryNodes.CRAFTING);
                }
                if (canWearArmor(possessed)) {
                    inventoryLimiter.unlock(player, DefaultInventoryNodes.ARMOR);
                    inventoryLimiter.unlock(player, ModdedInventoryNodes.TOOL_SPACE);
                }
                if (canCarryHotbar(possessed)) {
                    inventoryLimiter.unlock(player, DefaultInventoryNodes.HOTBAR);
                }
            }
        }
    }

    private static boolean canUseItems(Mob possessed) {
        if (possessed.getType().builtInRegistryHolder().is(RequiemCoreTags.Entity.ITEM_USERS)) {
            return true;
        }
        return possessed.canPickUpLoot();
    }

    private static boolean canCarryHotbar(Mob possessed) {
        return possessed.getType().builtInRegistryHolder().is(RequiemEntityTypeTags.HOTBAR_CARRIERS);
    }

    private static boolean canWearArmor(Mob possessed) {
        if (possessed.getType().builtInRegistryHolder().is(RequiemCoreTags.Entity.ARMOR_BANNED)) {
            return false;
        }
        if (possessed.getType().builtInRegistryHolder().is(RequiemEntityTypeTags.ARMOR_USERS)) {
            return true;
        }
        return !possessed.getItemBySlot(EquipmentSlot.HEAD).isEmpty() || possessed.canUseSlot(EquipmentSlot.HEAD);
    }

    @Override
    public void registerMobAbilities(MobAbilityRegistry reg) {
        reg.beginRegistration(EntityType.AXOLOTL).indirectInteract(AxolotlPlayingDeadAbility::new).build();
        reg.beginRegistration(EntityType.BLAZE).indirectAttack(BlazeFireballAbility::new).build();
        reg.beginRegistration(EntityType.CREEPER)
            .indirectAttack(CreeperPrimingAbility::new)
            .indirectInteract(CreeperPrimingAbility::new)
            .build();
        reg.beginRegistration(EntityType.ENDERMAN).indirectInteract(BlinkAbility::new).build();
        reg.beginRegistration(EntityType.EVOKER)
            .directAttack(EvokerFangAbility::new)
            .directInteract(EvokerWololoAbility::new)
            .indirectInteract(EvokerVexAbility::new)
            .build();
        reg.beginRegistration(EntityType.FROG)
            .directAttack(FrogCatchAbility::new)
            .build();
        reg.beginRegistration(EntityType.GHAST).indirectAttack(GhastFireballAbility::new).build();
        reg.beginRegistration(EntityType.GOAT).directAttack(GoatRamAbility::create).build();
        reg.beginRegistration(EntityType.GUARDIAN).directAttack(GuardianBeamAbility::new).build();
        reg.beginRegistration(EntityType.ELDER_GUARDIAN).directAttack(GuardianBeamAbility::new).build();
        reg.beginRegistration(EntityType.LLAMA).directAttack(RangedAttackAbility::new).build();
        reg.beginRegistration(EntityType.PUFFERFISH).indirectAttack(PufferfishInflationAbility::new).build();
        reg.beginRegistration(EntityType.SHULKER)
            .directAttack(ShulkerShootAbility::new)
            .indirectAttack(shulker -> new AutoAimAbility<>(shulker, AbilityType.ATTACK, 16.0, 4.0))
            .indirectInteract(ShulkerPeekAbility::new).build();
        reg.beginRegistration(EntityType.SNOW_GOLEM)
            .directAttack(e -> new RangedAttackAbility<>(e, 20, 10))
            .indirectInteract(SnowmanSnowballAbility::new).build();
        reg.beginRegistration(EntityType.TRADER_LLAMA).directAttack(RangedAttackAbility::new).build();
        reg.beginRegistration(EntityType.WITCH)
            .directAttack(owner -> new RangedAttackAbility<>(owner, 50, 10.)).build();
        reg.beginRegistration(EntityType.WITHER)
            .indirectAttack(WitherSkullAbility.BlueWitherSkullAbility::new)
            .directAttack(WitherSkullAbility.BlackWitherSkullAbility::new).build();
    }

    @Override
    public void registerRemnantStates(Registry<RemnantType> registry) {
        Registry.register(registry, Requiem.id("remnant"), RemnantTypes.REMNANT);
    }

    @Override
    public void registerSoulBindings(SoulbindingRegistry registry) {
        registry.registerSoulbound(RequiemStatusEffects.ATTRITION.value());
        registry.registerSoulbound(RequiemStatusEffects.EMANCIPATION.value());
        registry.registerSoulbound(RequiemStatusEffects.PENANCE.value());
        registry.registerSoulbound(RequiemStatusEffects.RECLAMATION.value());
    }

    @Override
    public void registerVagrantInteractions(VagrantInteractionRegistry registry) {
        registry.registerPossessionInteraction(
            EnderMan.class,
            (mob, player) -> !SoulHolderComponent.isSoulless(mob) && !PossessionComponent.get(player).startPossessing(mob, true),
            BasePossessionHandlers::performEndermanSoulAction,
            Requiem.id("textures/gui/enderjacking_icon.png")
        );
        registry.registerPossessionInteraction(Player.class,
            (target, possessor) -> target.getType() == RequiemEntities.PLAYER_SHELL && target instanceof AutomatoneFakePlayer shell && PlayerShellEvents.PRE_MERGE.invoker().canMerge(possessor, target, shell.getDisplayProfile()),
            (target, possessor) -> {
                if (target instanceof PlayerShellEntity shell && !RemnantComponent.get(possessor).merge(shell)) {
                    possessor.sendSystemMessage(Component.translatable("requiem:possess.incompatible_body"));
                }
            }
        );
        registry.registerPossessionInteraction(
            Mob.class,
            (mob, player) -> PossessionComponent.get(player).startPossessing(mob, true),
            (mob, player) -> PossessionComponent.get(player).startPossessing(mob)
        );
    }

    @Override
    public void registerPossessionItemActions(Registry<PossessionItemAction> registry) {
        Registry.register(registry, Requiem.id("pass"), (player, possessed, stack, world, hand) -> InteractionResult.PASS);
        Registry.register(registry, Requiem.id("fail"), (player, possessed, stack, world, hand) -> InteractionResult.FAIL);
        Registry.register(registry, Requiem.id("cure"), VanillaRequiemPlugin::cure);
        Registry.register(registry, Requiem.id("eat_to_heal"), VanillaRequiemPlugin::healWithFood);
        Registry.register(registry, Requiem.id("replace_bone"), VanillaRequiemPlugin::replaceBone);
        Registry.register(registry, Requiem.id("witch_eat"), VanillaRequiemPlugin::eatWitchFood);
    }

    public static InteractionResult cure(Player player, Mob possessed, ItemStack stack, Level world, InteractionHand hand) {
        if (RemnantComponent.get(player).canCurePossessed(possessed)) {
            PossessionComponent.get(player).startCuring();
            stack.shrink(1);
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.FAIL;
    }

    public static InteractionResult healWithFood(Player player, Mob possessed, ItemStack stack, Level world, InteractionHand hand) {
        var food = stack.get(net.minecraft.core.component.DataComponents.FOOD);

        if (food != null) {
            possessed.heal(food.nutrition());
            stack.finishUsingItem(world, player);
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.FAIL;
    }

    public static InteractionResult replaceBone(Player player, Mob possessed, ItemStack stack, Level world, InteractionHand hand) {
        if (SkeletonBoneComponent.KEY.get(possessed).replaceBone()) {
            stack.shrink(1);
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.FAIL;
    }

    public static InteractionResult eatWitchFood(Player player, Mob possessed, ItemStack stack, Level world, InteractionHand hand) {
        Map<Holder<MobEffect>, MobEffectInstance> before = new HashMap<>(possessed.getActiveEffectsMap());
        stack.finishUsingItem(world, player);
        Map<Holder<MobEffect>, MobEffectInstance> after = new HashMap<>(possessed.getActiveEffectsMap());
        // Remove all negative status effects from the food
        revertHarmfulEffects(player, before, after);
        return InteractionResult.SUCCESS;
    }

    private static void revertHarmfulEffects(Player player, Map<Holder<MobEffect>, MobEffectInstance> before, Map<Holder<MobEffect>, MobEffectInstance> after) {
        for (var entry : after.entrySet()) {
            Holder<MobEffect> statusEffect = entry.getKey();
            if (statusEffect.value().getCategory() == MobEffectCategory.HARMFUL) {
                MobEffectInstance previous = before.get(statusEffect);
                MobEffectInstance current = entry.getValue();
                if (!Objects.equals(previous, current)) {
                    player.removeEffect(statusEffect);
                    if (previous != null) {
                        player.addEffect(new MobEffectInstance(
                            statusEffect,
                            Math.min(previous.getDuration(), current.getDuration()),
                            Math.min(previous.getAmplifier(), current.getAmplifier()),
                            previous.isAmbient(),
                            previous.isVisible(),
                            previous.showIcon()
                        ));
                    }
                }
            }
        }
    }
}
