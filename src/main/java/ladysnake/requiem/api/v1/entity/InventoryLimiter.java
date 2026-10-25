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
package ladysnake.requiem.api.v1.entity;

import io.github.ladysnake.locki.InventoryLock;
import io.github.ladysnake.locki.InventoryNode;
import ladysnake.requiem.api.v1.internal.ApiInternals;
import net.minecraft.world.entity.player.Player;

public interface InventoryLimiter {
    static InventoryLimiter instance() {
        return ApiInternals.getInventoryLimiter();
    }

    InventoryLock getLock();

    void enable(Player player);
    void disable(Player player);

    void unlock(Player player, InventoryNode part);
    void lock(Player player, InventoryNode part);
    boolean isLocked(Player player, InventoryNode part);
    boolean isSlotLocked(Player player, int playerSlot);
    boolean isSlotInvisible(Player player, int playerSlot);
    InventoryShape getInventoryShape(Player player);
}

