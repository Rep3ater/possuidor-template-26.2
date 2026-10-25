/*
 * Requiem
 * Copyright (C) 2017-2023 Ladysnake
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; If not, see <https://www.gnu.org/licenses>.
 */
package ladysnake.requiem.api.v1.event.minecraft;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionResult;

@FunctionalInterface
public interface ItemPickupCallback {

    InteractionResult onItemPickup(Player player, ItemEntity pickedUp);

    Event<ItemPickupCallback> EVENT = EventFactory.createArrayBacked(ItemPickupCallback.class,
            (listeners) -> (player, pickedUp) -> {
                for (ItemPickupCallback handler : listeners) {
                    InteractionResult actionResult = handler.onItemPickup(player, pickedUp);
                    if (actionResult != InteractionResult.PASS) {
                        return actionResult;
                    }
                }
                return InteractionResult.PASS;
            });
}

