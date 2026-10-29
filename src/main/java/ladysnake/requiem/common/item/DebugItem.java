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

import ladysnake.requiem.api.v1.remnant.DeathSuspender;
import ladysnake.requiem.common.network.RequiemNetworking;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class DebugItem extends Item {
    private int debugMode;

    public DebugItem(Item.Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand hand) {
        if (player.isShiftKeyDown()) {
            if (!world.isClientSide()) {
                debugMode = (debugMode + 1) % 2;
                player.sendSystemMessage(Component.literal("Switched mode to " + debugMode));
            }
        } else {
            switch (debugMode) {
                case 0:
                    if (world.isClientSide()) {
                        if (DeathSuspender.get(player).isLifeTransient()) {
                            RequiemNetworking.sendDialogueActionMessage(0);
                            RequiemNetworking.sendDialogueActionMessage(1);
                            RequiemNetworking.sendDialogueActionMessage(0);
                            DeathSuspender.get(player).setLifeTransient(false);
                        } else {
                            DeathSuspender.get(player).setLifeTransient(true);
                        }
                    }
                    break;
                case 1:
                    if (!world.isClientSide()) {
                        RequiemNetworking.sendBodyCureMessage(player);
                    }
                    break;
            }
        }
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), world.isClientSide());
    }
}
