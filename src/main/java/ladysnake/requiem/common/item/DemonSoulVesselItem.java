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

import ladysnake.requiem.api.v1.possession.PossessionComponent;
import ladysnake.requiem.api.v1.remnant.RemnantComponent;
import ladysnake.requiem.api.v1.remnant.RemnantType;
import ladysnake.requiem.common.network.RequiemNetworking;
import ladysnake.requiem.common.sound.RequiemSoundEvents;
import ladysnake.requiem.core.mixin.access.LivingEntityAccessor;
import ladysnake.requiem.core.possession.PossessionComponentImpl;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LecternBlock;
import net.minecraft.world.level.block.state.BlockState;


public class DemonSoulVesselItem extends Item {
    private final RemnantType remnantType;
    private final ChatFormatting color;
    private final String tooltip;

    public DemonSoulVesselItem(RemnantType remnantType, ChatFormatting color, Item.Properties settings, String tooltip) {
        super(settings);
        this.remnantType = remnantType;
        this.color = color;
        this.tooltip = tooltip;
    }

    public RemnantType getRemnantType() {
        return remnantType;
    }

    public ChatFormatting getTooltipColor() {
        return color;
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level world = ctx.getLevel();
        BlockPos pos = ctx.getClickedPos();
        BlockState state = world.getBlockState(pos);
        if (state.getBlock() == Blocks.LECTERN) {
            return LecternBlock.tryPlaceBook(ctx.getPlayer(), world, pos, state, ctx.getItemInHand()) ? InteractionResult.SUCCESS : InteractionResult.PASS;
        } else {
            return InteractionResult.PASS;
        }
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (world.isClientSide()) {
            return InteractionResultHolder.sidedSuccess(stack, world.isClientSide());
        }

        if (player instanceof ServerPlayer serverPlayer && stack.getItem() == this) {
            RemnantComponent remnantComponent = RemnantComponent.get(player);
            RemnantType currentState = remnantComponent.getRemnantType();

            if (currentState != this.remnantType) {
                PossessionComponent possessionComponent = PossessionComponent.get(player);
                Mob possessedEntity = possessionComponent.getHost();

                if (possessedEntity == null || possessionComponent.isCuring()) {
                    boolean wasVagrant = remnantComponent.isVagrant();
                    world.playSound(null,
                        player.getX(), player.getY(), player.getZ(),
                        RequiemSoundEvents.ITEM_OPUS_USE,
                        player.getSoundSource(), 1.0F, 0.1F
                    );
                    world.playSound(null,
                        player.getX(), player.getY(), player.getZ(),
                        this.remnantType.isDemon() ? RequiemSoundEvents.EFFECT_BECOME_REMNANT : RequiemSoundEvents.EFFECT_BECOME_MORTAL,
                        player.getSoundSource(), 1.4F, 0.1F
                    );
                    RequiemNetworking.sendTo(serverPlayer, RequiemNetworking.createOpusUsePacket(this.remnantType, true));

                    remnantComponent.become(this.remnantType, true);

                    if (possessedEntity != null) {
                        if (remnantComponent.canCurePossessed(possessedEntity)) {
                            remnantComponent.curePossessed(possessedEntity);
                        } else if (remnantComponent.isVagrant()) {
                            possessionComponent.startPossessing(possessedEntity);
                        } else {
                            PossessionComponentImpl.dropEquipment(possessedEntity, serverPlayer);
                        }
                    } else if (wasVagrant && !this.remnantType.isDemon()) {
                        // Becoming mortal while out of body -> restore physical body
                        remnantComponent.setVagrant(false);
                        serverPlayer.setHealth(serverPlayer.getMaxHealth());
                        serverPlayer.getAbilities().mayfly = false;
                        serverPlayer.getAbilities().flying = false;
                        serverPlayer.onUpdateAbilities();
                        ladysnake.requiem.api.v1.entity.MovementAlterer.get(serverPlayer).setConfig(null);
                        io.github.ladysnake.pal.Pal.revokeAbility(serverPlayer, io.github.ladysnake.pal.VanillaAbilities.INVULNERABLE, ladysnake.requiem.core.remnant.MutableRemnantState.SOUL_STATE);
                    } else if (remnantComponent.isIncorporeal() && !serverPlayer.isCreative() && !serverPlayer.isSpectator()) {
                        player.getInventory().dropAll();
                    }

                    player.awardStat(Stats.ITEM_USED.get(this));
                    ItemStack result = new ItemStack(RequiemItems.EMPTY_SOUL_VESSEL);
                    player.setItemInHand(hand, ItemUtils.createFilledResult(stack, player, result));
                    return InteractionResult.SUCCESS;
                }
            }

            return InteractionResult.SUCCESS;
        }

        return InteractionResult.FAIL;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, java.util.List<Component> tooltipComponents, TooltipFlag flag) {
        tooltipComponents.add(Component.translatable(this.tooltip).withStyle(this.getTooltipColor()));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }
}
