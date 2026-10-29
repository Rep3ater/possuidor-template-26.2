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

import ladysnake.requiem.api.v1.event.requiem.EntityRecordUpdateCallback;
import ladysnake.requiem.common.RequiemRecordTypes;
import ladysnake.requiem.common.entity.ReleasedSoulEntity;
import ladysnake.requiem.common.entity.RequiemEntities;
import ladysnake.requiem.common.sound.RequiemSoundEvents;
import ladysnake.requiem.core.entity.SoulHolderComponent;
import ladysnake.requiem.core.util.NbtCompat;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import java.util.function.Consumer;

public class FilledSoulVesselItem extends Item {
    public static final String SOUL_FRAGMENT_NBT = "requiem:soul_fragment";

    public static ItemStack forEntityType(EntityType<?> type) {
        ItemStack result = new ItemStack(RequiemItems.FILLED_SOUL_VESSEL);
        CustomData.update(DataComponents.CUSTOM_DATA, result, tag -> {
            CompoundTag soulFragment = new CompoundTag();
            soulFragment.putString("type", BuiltInRegistries.ENTITY_TYPE.getKey(type).toString());
            tag.put(SOUL_FRAGMENT_NBT, soulFragment);
        });
        return result;
    }

    private final EmptySoulVesselItem emptySoulVessel;

    public FilledSoulVesselItem(Item.Properties settings, EmptySoulVesselItem emptySoulVessel) {
        super(settings);
        this.emptySoulVessel = emptySoulVessel;
    }

    @Override
    public ItemStackTemplate getCraftingRemainder(ItemInstance instance) {
        return new ItemStackTemplate(this.emptySoulVessel);
    }

    public void registerCallbacks() {
        EntityRecordUpdateCallback.EVENT.register((entity, linkedRecord) ->
            linkedRecord.get(RequiemRecordTypes.RELEASED_SOUL).flatMap(u -> SoulHolderComponent.KEY.maybeGet(entity)).ifPresent(soulHolder -> {
                soulHolder.giveSoulBack();
                linkedRecord.invalidate();
            }));
    }

    @Override
    public Component getName(ItemStack stack) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        java.util.Optional<EntityType<?>> entityType = tag.getCompound(SOUL_FRAGMENT_NBT)
            .flatMap(c -> c.getString("type"))
            .map(Identifier::tryParse)
            .flatMap(BuiltInRegistries.ENTITY_TYPE::getOptional);
        if (entityType.isPresent()) {
            return Component.translatable("item.requiem.filled_soul_vessel.entity", entityType.get().getDescription());
        }
        return super.getName(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> tooltipAdder, TooltipFlag flag) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        tag.getCompound(SOUL_FRAGMENT_NBT).ifPresent(fragmentData -> {
            fragmentData.getString("type")
                .map(Identifier::tryParse)
                .flatMap(BuiltInRegistries.ENTITY_TYPE::getOptional)
                .ifPresent(contained -> tooltipAdder.accept(Component.translatable("requiem:tooltip.filled_vessel", contained.getDescription()).withStyle(ChatFormatting.GRAY)));
        });
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player user, LivingEntity target, InteractionHand hand) {
        if (target instanceof PathfinderMob mob) {
            if (!user.level().isClientSide()) {
                CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
                String soulType = tag.getCompound(SOUL_FRAGMENT_NBT)
                    .flatMap(frag -> frag.getString("type"))
                    .orElse("minecraft:zombie");

                ladysnake.requiem.common.construct.ai.SoulAiManager.injectSoul(mob, soulType, user);
                user.setItemInHand(hand, ItemUtils.createFilledResult(stack, user, this.getEmptiedStack()));
            }
            user.playSound(RequiemSoundEvents.ITEM_FILLED_VESSEL_USE, 2.0f, 1.0f);
            return InteractionResult.SUCCESS;
        }
        return super.interactLivingEntity(stack, user, target, hand);
    }

    @Override
    public InteractionResult use(Level world, Player user, InteractionHand hand) {
        ItemStack stack = user.getItemInHand(hand);
        if (!world.isClientSide()) {
            CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
            UUID uuid = tag.getCompound(SOUL_FRAGMENT_NBT)
                .flatMap(frag -> NbtCompat.getUuid(frag, "uuid"))
                .orElse(null);
            releaseSoul(user, uuid);
            user.setItemInHand(hand, ItemUtils.createFilledResult(stack, user, this.getEmptiedStack()));
            return InteractionResult.SUCCESS;
        }
        user.playSound(RequiemSoundEvents.ITEM_FILLED_VESSEL_USE, 3f, 0.6F + user.getRandom().nextFloat() * 0.4F);
        return InteractionResult.SUCCESS;
    }

    public ItemStack getEmptiedStack() {
        return new ItemStack(this.emptySoulVessel);
    }

    public static void releaseSoul(LivingEntity user, @Nullable UUID ownerRecord) {
        ReleasedSoulEntity releasedSoul = new ReleasedSoulEntity(RequiemEntities.RELEASED_SOUL, user.level(), ownerRecord);
        releasedSoul.setPos(user.getX(), user.getY(0.8D), user.getZ());
        releasedSoul.setDeltaMovement(user.getLookAngle().normalize().scale(0.15));
        releasedSoul.setYRot(user.getYRot());
        releasedSoul.setXRot(user.getXRot());
        user.level().addFreshEntity(releasedSoul);
    }
}
