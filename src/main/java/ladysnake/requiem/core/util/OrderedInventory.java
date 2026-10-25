/*
 * Requiem
 * Copyright (C) 2017-2023 Ladysnake
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU License
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
package ladysnake.requiem.core.util;

import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.world.ItemStackWithSlot;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class OrderedInventory extends SimpleContainer {
    public OrderedInventory(int size) {
        super(size);
    }

    public void readNbtList(ListTag tags) {
        this.clearContent();
        ItemStackWithSlot.CODEC.listOf().parse(NbtOps.INSTANCE, tags).result().ifPresent(list -> {
            for (ItemStackWithSlot slotItem : list) {
                if (slotItem.isValidInContainer(this.getContainerSize())) {
                    this.setItem(slotItem.slot(), slotItem.stack());
                }
            }
        });
    }

    public ListTag toNbtList() {
        List<ItemStackWithSlot> list = new ArrayList<>();
        for (int slot = 0; slot < this.getContainerSize(); ++slot) {
            ItemStack stack = this.getItem(slot);
            if (!stack.isEmpty()) {
                list.add(new ItemStackWithSlot(slot, stack));
            }
        }
        return (ListTag) ItemStackWithSlot.CODEC.listOf().encodeStart(NbtOps.INSTANCE, list).result().orElseGet(ListTag::new);
    }

    public List<ItemStack> clearToList() {
        return this.removeAllItems();
    }
}
