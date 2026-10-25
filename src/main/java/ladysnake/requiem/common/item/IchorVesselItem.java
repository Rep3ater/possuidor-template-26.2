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

import ladysnake.requiem.common.block.RequiemBlocks;
import ladysnake.requiem.common.block.obelisk.InertRunestoneBlock;
import ladysnake.requiem.common.block.obelisk.RunestoneBlock;
import ladysnake.requiem.common.sound.RequiemSoundEvents;
import net.minecraft.advancements.triggers.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.gameevent.GameEvent;

import java.util.Optional;

public class IchorVesselItem extends Item {
    private static final int MAX_USE_TIME = 20;

    private final MobEffectInstance effect;

    public IchorVesselItem(Item.Properties settings, MobEffectInstance effect) {
        super(settings);
        this.effect = effect;
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level world, LivingEntity user) {
        Player player = user instanceof Player ? (Player)user : null;
        if (player instanceof ServerPlayer serverPlayer) {
            CriteriaTriggers.CONSUME_ITEM.trigger(serverPlayer, stack);
        }

        if (!world.isClientSide()) {
            user.addEffect(new MobEffectInstance(this.effect));
        }

        if (player != null) {
            player.awardStat(Stats.ITEM_USED.get(this));
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
        }

        if (player == null || !player.getAbilities().instabuild) {
            if (stack.isEmpty()) {
                return new ItemStack(RequiemItems.EMPTY_SOUL_VESSEL);
            }

            if (player != null) {
                player.getInventory().add(new ItemStack(RequiemItems.EMPTY_SOUL_VESSEL));
            }
        }

        user.gameEvent(GameEvent.DRINK);
        return stack;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return MAX_USE_TIME;
    }

    @Override
    public ItemUseAnimation getUseAnimation(ItemStack stack) {
        return ItemUseAnimation.BOW;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level world = context.getLevel();
        BlockPos pos = context.getClickedPos();
        ItemStack stack = context.getItemInHand();
        Player player = context.getPlayer();

        if (world.getBlockState(pos).is(RequiemBlocks.TACHYLITE_RUNESTONE)) {
            InteractionResult result = useOnBlock(world, pos, stack);
            if (result.consumesAction() && player != null) {
                player.setItemInHand(context.getHand(), ItemUtils.createFilledResult(stack, player, new ItemStack(RequiemItems.EMPTY_SOUL_VESSEL)));
                player.awardStat(Stats.ITEM_USED.get(stack.getItem()));
            }
            return result;
        }
        return InteractionResult.PASS;
    }

    public InteractionResult useOnBlock(Level world, BlockPos pos, ItemStack stack) {
        if (world.getBlockState(pos).is(RequiemBlocks.TACHYLITE_RUNESTONE)) {
            if (!world.isClientSide()) {
                Optional<Block> runestone = RunestoneBlock.getByEffect(this.effect.getEffect());
                if (runestone.isEmpty()) return InteractionResult.FAIL;

                world.setBlock(pos, runestone.get().defaultBlockState(), Block.UPDATE_ALL);
                InertRunestoneBlock.tryActivateObelisk((ServerLevel) world, pos, false);

                world.playSound(null, pos, RequiemSoundEvents.BLOCK_RUNESTONE_CARVE, SoundSource.BLOCKS, 1.0F, 1.0F);
                world.gameEvent(null, GameEvent.FLUID_PLACE, pos);
                return InteractionResult.SUCCESS;
            }

            return InteractionResult.SUCCESS;
        }

        return InteractionResult.PASS;
    }

    @Override
    public InteractionResult use(Level world, Player user, InteractionHand hand) {
        return ItemUtils.startUsingInstantly(world, user, hand);
    }
}
