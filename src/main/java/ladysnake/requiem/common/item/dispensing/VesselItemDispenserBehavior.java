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
package ladysnake.requiem.common.item.dispensing;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.core.dispenser.DefaultDispenseItemBehavior;
import net.minecraft.core.dispenser.OptionalDispenseItemBehavior;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.entity.DispenserBlockEntity;

import java.util.function.BiFunction;

public class VesselItemDispenserBehavior extends OptionalDispenseItemBehavior {
    private final DefaultDispenseItemBehavior fallbackBehavior = new DefaultDispenseItemBehavior();
    private final Action action;

    @FunctionalInterface
    public interface Action {
        ItemStack apply(Level level, BlockPos pos, ItemStack stack);
    }

    public VesselItemDispenserBehavior(Action action) {
        this.action = action;
    }

    public ItemStack tryPutStack(BlockSource pointer, ItemStack inputStack, ItemStack outputStack) {
        inputStack.shrink(1);
        if (inputStack.isEmpty()) {
            return outputStack.copy();
        } else {
            if (pointer.blockEntity() instanceof DispenserBlockEntity dispenser) {
                if (dispenser.insertItem(outputStack.copy()).isEmpty()) {
                    return inputStack;
                }
            }
            fallbackBehavior.dispense(pointer, outputStack.copy());
            return inputStack;
        }
    }

    @Override
    protected ItemStack execute(BlockSource pointer, ItemStack stack) {
        this.setSuccess(false);
        Direction direction = pointer.state().getValue(DispenserBlock.FACING);
        BlockPos targetPos = pointer.pos().relative(direction);
        ItemStack result = action.apply(pointer.level(), targetPos, stack);
        if (!result.isEmpty()) {
            this.setSuccess(true);
            return this.tryPutStack(pointer, stack, result);
        } else {
            return super.execute(pointer, stack);
        }
    }
}
