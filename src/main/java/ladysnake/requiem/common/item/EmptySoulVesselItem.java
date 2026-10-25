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
 */
package ladysnake.requiem.common.item;

import ladysnake.requiem.core.entity.SoulHolderComponent;
import ladysnake.requiem.api.v1.event.requiem.SoulCaptureEvents;
import ladysnake.requiem.api.v1.record.GlobalRecord;
import ladysnake.requiem.common.block.RequiemBlocks;
import ladysnake.requiem.common.block.obelisk.InertRunestoneBlock;
import ladysnake.requiem.common.block.obelisk.RunestoneBlock;
import ladysnake.requiem.common.entity.RequiemEntityAttributes;
import ladysnake.requiem.common.entity.effect.AttritionStatusEffect;
import ladysnake.requiem.common.particle.RequiemEntityParticleEffect;
import ladysnake.requiem.common.particle.RequiemParticleTypes;
import ladysnake.requiem.common.remnant.WandererRemnantState;
import ladysnake.requiem.common.sound.RequiemSoundEvents;
import ladysnake.requiem.common.tag.RequiemEntityTypeTags;
import ladysnake.requiem.core.record.EntityPositionClerk;
import ladysnake.requiem.core.util.NbtCompat;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.gameevent.GameEvent;

import java.util.Optional;
import java.util.UUID;

public class EmptySoulVesselItem extends Item {

    public static final String ACTIVE_DATA_TAG = "requiem:soul_capture";
    public static final int SOUL_AGGREGATE_STEALING_DAMAGE = 20;
    public static final int POST_CAPTURE_COOLDOWN = 100;

    public EmptySoulVesselItem(Item.Properties settings) {
        super(settings);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level world = context.getLevel();
        BlockPos pos = context.getClickedPos();
        ItemStack stack = context.getItemInHand();
        Player player = context.getPlayer();

        if (world.getBlockState(pos).getBlock() instanceof RunestoneBlock runestone) {
            InteractionResult result = useOnBlock(world, pos, stack);
            if (result.consumesAction() && player != null) {
                IchorVesselItem filledVessel = RequiemItems.getVesselByEffect(runestone.getEffect());
                if (filledVessel != null) {
                    player.setItemInHand(context.getHand(), ItemUtils.createFilledResult(stack, player, new ItemStack(filledVessel)));
                    player.awardStat(Stats.ITEM_USED.get(stack.getItem()));
                }
            }
            return result;
        }
        return InteractionResult.PASS;
    }

    public static InteractionResult useOnBlock(Level world, BlockPos pos, ItemStack stack) {
        if (world.getBlockState(pos).getBlock() instanceof RunestoneBlock runestone) {
            if (!world.isClientSide()) {
                IchorVesselItem filledVessel = RequiemItems.getVesselByEffect(runestone.getEffect());
                if (filledVessel == null) return InteractionResult.FAIL;

                world.setBlock(pos, RequiemBlocks.TACHYLITE_RUNESTONE.defaultBlockState(), Block.UPDATE_ALL);
                InertRunestoneBlock.tryActivateObelisk((ServerLevel) world, pos, false);

                world.playSound(null, pos, RequiemSoundEvents.BLOCK_RUNESTONE_CLEAR, SoundSource.BLOCKS, 1.0F, 1.0F);
                world.gameEvent(null, GameEvent.FLUID_PICKUP, pos);
                return InteractionResult.SUCCESS;
            }

            return InteractionResult.SUCCESS;
        }

        return InteractionResult.PASS;
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player user, LivingEntity entity, InteractionHand hand) {
        Optional<SoulCaptureEvents.CaptureType> captureType = checkCapturePreconditions(user, entity);
        if (captureType.isPresent()) {
            CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
                CompoundTag activeData = new CompoundTag();
                activeData.putInt("use_time", computeCaptureTime(entity, user, captureType.get()));
                NbtCompat.putUuid(activeData, "target", entity.getUUID());
                tag.put(ACTIVE_DATA_TAG, activeData);
            });
            user.startUsingItem(hand);
            return InteractionResult.CONSUME;
        }

        return super.interactLivingEntity(stack, user, entity, hand);
    }

    public static Optional<SoulCaptureEvents.CaptureType> checkCapturePreconditions(LivingEntity user, LivingEntity entity) {
        return Optional.of(entity)
            .filter(e -> e instanceof Mob)
            .flatMap(EmptySoulVesselItem::getCaptureType)
            .filter(captureType -> SoulCaptureEvents.BEFORE_ATTEMPT.invoker().canAttemptCapturing(user, entity, captureType));
    }

    private static Optional<SoulCaptureEvents.CaptureType> getCaptureType(LivingEntity entity) {
        if (entity.getType().builtInRegistryHolder().is(RequiemEntityTypeTags.SOUL_AGGREGATES)) {
            return Optional.of(SoulCaptureEvents.CaptureType.AGGREGATE);
        } else if (!entity.getType().builtInRegistryHolder().is(RequiemEntityTypeTags.SOUL_CAPTURE_BLACKLIST)) {
            return Optional.of(SoulCaptureEvents.CaptureType.NORMAL);
        }

        return Optional.empty();
    }

    private int computeCaptureTime(LivingEntity target, LivingEntity user, SoulCaptureEvents.CaptureType captureType) {
        int targetSoulStrength = computeSoulDefense(target, captureType);
        int playerSoulStrength = computeSoulOffense(user);
        float max = switch (captureType) {
            case NORMAL -> 3.0f;
            case AGGREGATE -> 1.5f;
        };
        return Math.round(96.0f * Mth.clamp((float) targetSoulStrength / playerSoulStrength, 1.0f, max));
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level world, LivingEntity user) {
        if (!(world instanceof ServerLevel serverWorld)) return stack;
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        CompoundTag activeData = tag.getCompound(ACTIVE_DATA_TAG).orElse(null);
        if (activeData == null) return stack;
        CustomData.update(DataComponents.CUSTOM_DATA, stack, t -> t.remove(ACTIVE_DATA_TAG));
        UUID targetId = NbtCompat.getUuid(activeData, "target").orElse(null);
        if (targetId == null) return stack;
        Entity entity = serverWorld.getEntity(targetId);

        if (!(entity instanceof LivingEntity target)) return stack;
        if (!(user instanceof ServerPlayer remnant)) return stack;
        Optional<SoulCaptureEvents.CaptureType> captureType = checkCapturePreconditions(remnant, target);
        if (captureType.isEmpty()) return stack;

        ItemStack result;
        remnant.awardStat(Stats.ITEM_USED.get(this));

        if (wins(remnant, target, captureType.get())) {
            result = FilledSoulVesselItem.forEntityType(entity.getType());
            UUID recordUuid = switch(captureType.get()) {
                case NORMAL -> setupRecord(target).getUuid();
                case AGGREGATE -> UUID.randomUUID();
            };
            CustomData.update(DataComponents.CUSTOM_DATA, result, resTag -> {
                CompoundTag soulFragment = resTag.getCompound(FilledSoulVesselItem.SOUL_FRAGMENT_NBT).orElseGet(CompoundTag::new);
                NbtCompat.putUuid(soulFragment, "uuid", recordUuid);
                soulFragment.putString("type", net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString());
                resTag.put(FilledSoulVesselItem.SOUL_FRAGMENT_NBT, soulFragment);
            });
            switch (captureType.get()) {
                case NORMAL -> SoulHolderComponent.get(target).removeSoul();
                case AGGREGATE -> target.hurt(target.damageSources().magic(), SOUL_AGGREGATE_STEALING_DAMAGE);
            }
            remnant.getCooldowns().addCooldown(result, POST_CAPTURE_COOLDOWN);
        } else {
            AttritionStatusEffect.apply(remnant, 1, 20 * 60 * 5);
            WandererRemnantState.spawnAttritionParticles(remnant, remnant);
            remnant.awardStat(Stats.ITEM_BROKEN.get(this));
            remnant.onEquippedItemBroken(this, user.getUsedItemHand() == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
            result = new ItemStack(RequiemItems.SHATTERED_SOUL_VESSEL);
        }

        return ItemUtils.createFilledResult(stack, remnant, result);
    }

    public static GlobalRecord setupRecord(LivingEntity target) {
        return EntityPositionClerk.get(target).getOrCreateRecord();
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        return tag.getCompound(ACTIVE_DATA_TAG).flatMap(c -> c.getInt("use_time")).orElse(0);
    }

    @Override
    public void onUseTick(Level world, LivingEntity user, ItemStack stack, int remainingUseTicks) {
        if (world instanceof ServerLevel serverWorld) {
            CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
            UUID targetId = tag.getCompound(ACTIVE_DATA_TAG).flatMap(c -> NbtCompat.getUuid(c, "target")).orElse(null);
            if (targetId != null) {
                Entity target = serverWorld.getEntity(targetId);
                if (target instanceof LivingEntity) {
                    playSoulCaptureEffects(user, target);
                }
            }
        }
    }

    public static void playSoulCaptureEffects(LivingEntity user, Entity target) {
        if (!(user.level() instanceof ServerLevel world)) throw new IllegalStateException("Must be called serverside");
        if (world.getRandom().nextFloat() < 0.75f) {
            world.sendParticles(
                new RequiemEntityParticleEffect(RequiemParticleTypes.ENTITY_DUST, target.getId(), user.getId()),
                target.getX(), target.getY(0.5), target.getZ(),
                world.getRandom().nextInt(6) + 4,
                target.getBbWidth() * 0.2,
                target.getBbHeight() * 0.2,
                target.getBbWidth() * 0.2,
                1.0
            );
        }
        user.playSound(RequiemSoundEvents.ITEM_EMPTY_VESSEL_USE, 1, 1);
    }

    public static boolean wins(LivingEntity user, LivingEntity target, SoulCaptureEvents.CaptureType captureType) {
        int soulOffense = computeSoulOffense(user);
        int soulDefense = computeSoulDefense(target, captureType);

        if (soulOffense > soulDefense) {
            return true;
        }

        float strengthRatio = (float) soulOffense / soulDefense;
        return user.getRandom().nextFloat() < (strengthRatio * strengthRatio);
    }

    static int computeSoulOffense(LivingEntity user) {
        var instance = user.getAttribute(RequiemEntityAttributes.SOUL_OFFENSE);
        return instance != null ? (int) Math.round(instance.getValue()) : 15;
    }

    static int computeSoulDefense(LivingEntity entity, SoulCaptureEvents.CaptureType captureType) {
        var instance = entity.getAttribute(RequiemEntityAttributes.SOUL_DEFENSE);
        double base = instance != null ? instance.getValue() : 0.0;
        double maxHealth = switch (captureType) {
            case NORMAL -> entity.getMaxHealth();
            case AGGREGATE -> 1;
        };
        double intrinsicArmor = getAttributeBaseValue(entity, Attributes.ARMOR);
        double intrinsicArmorToughness = getAttributeBaseValue(entity, Attributes.ARMOR_TOUGHNESS);
        double intrinsicStrength = getAttributeBaseValue(entity, Attributes.ATTACK_DAMAGE);
        double invertedHealthRatio = 1 - entity.getHealth() / entity.getMaxHealth();
        double woundedModifier = 1 - (invertedHealthRatio * invertedHealthRatio);
        double healthModifier = woundedModifier * 0.8 + 0.2;
        double strengthModifier = computeStrengthModifier(intrinsicStrength);
        double physicalModifier = strengthModifier * healthModifier;
        return (int) Math.round(base + physicalModifier * (maxHealth + intrinsicArmor * 2 + intrinsicArmorToughness * 3));
    }

    private static double computeStrengthModifier(double intrinsicStrength) {
        if (intrinsicStrength == 0) return 0.5;
        else if (intrinsicStrength < 2) return 0.75;
        else return Math.sqrt(intrinsicStrength * 0.5);
    }

    private static double getAttributeBaseValue(LivingEntity entity, Holder<Attribute> attribute) {
        var instance = entity.getAttribute(attribute);
        return instance == null ? 0 : instance.getBaseValue();
    }

    @Override
    public ItemUseAnimation getUseAnimation(ItemStack stack) {
        return ItemUseAnimation.BOW;
    }
}
