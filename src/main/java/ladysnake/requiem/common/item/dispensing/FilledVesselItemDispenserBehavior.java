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

import ladysnake.requiem.common.entity.ReleasedSoulEntity;
import ladysnake.requiem.common.entity.RequiemEntities;
import ladysnake.requiem.common.item.FilledSoulVesselItem;
import ladysnake.requiem.core.util.NbtCompat;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.core.dispenser.DefaultDispenseItemBehavior;
import net.minecraft.core.dispenser.DispenseItemBehavior;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.entity.DispenserBlockEntity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class FilledVesselItemDispenserBehavior implements DispenseItemBehavior {
    private final DefaultDispenseItemBehavior fallbackBehavior = new DefaultDispenseItemBehavior();

    public FilledVesselItemDispenserBehavior() {
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
    public ItemStack dispense(BlockSource pointer, ItemStack stack) {
        Direction direction = pointer.state().getValue(DispenserBlock.FACING);
        ServerLevel level = pointer.level();
        Vec3 targetPos = Vec3.atCenterOf(pointer.pos().relative(direction));

        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        @Nullable UUID ownerRecord = tag.getCompound(FilledSoulVesselItem.SOUL_FRAGMENT_NBT)
            .flatMap(frag -> NbtCompat.getUuid(frag, "uuid"))
            .orElse(null);

        ReleasedSoulEntity releasedSoul = new ReleasedSoulEntity(RequiemEntities.RELEASED_SOUL, level, ownerRecord);
        releasedSoul.setPos(targetPos.x, targetPos.y, targetPos.z);
        releasedSoul.setDeltaMovement(new Vec3(direction.getStepX(), direction.getStepY(), direction.getStepZ()).scale(0.15f));
        releasedSoul.setYRot(level.getRandom().nextFloat() * 360f);
        releasedSoul.setXRot(level.getRandom().nextFloat() * 360f);
        level.addFreshEntity(releasedSoul);

        ItemStack result = ((FilledSoulVesselItem) stack.getItem()).getEmptiedStack();
        return this.tryPutStack(pointer, stack, result);
    }
}
