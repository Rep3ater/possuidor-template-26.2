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

import ladysnake.requiem.common.block.obelisk.RunestoneBlock;
import ladysnake.requiem.common.item.EmptySoulVesselItem;
import ladysnake.requiem.common.item.IchorVesselItem;
import ladysnake.requiem.common.item.RequiemItems;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.DispenserBlock;

public final class RequiemDispenserBehaviors {
    public static void registerDispenserBehaviors() {
        DispenserBlock.registerBehavior(RequiemItems.EMPTY_SOUL_VESSEL, new VesselItemDispenserBehavior((level, pos, stack) -> {
            if (level.getBlockState(pos).getBlock() instanceof RunestoneBlock runestone) {
                IchorVesselItem filledVessel = RequiemItems.getVesselByEffect(runestone.getEffect());
                if (filledVessel != null) {
                    InteractionResult res = EmptySoulVesselItem.useOnBlock(level, pos, stack);
                    if (res.consumesAction()) {
                        return new ItemStack(filledVessel);
                    }
                }
            }
            return ItemStack.EMPTY;
        }));

        for (var ichorVessel : RequiemItems.vesselsByEffect.values()) {
            DispenserBlock.registerBehavior(ichorVessel, new VesselItemDispenserBehavior((level, pos, stack) -> {
                InteractionResult res = ichorVessel.useOnBlock(level, pos, stack);
                if (res.consumesAction()) {
                    return new ItemStack(RequiemItems.EMPTY_SOUL_VESSEL);
                }
                return ItemStack.EMPTY;
            }));
        }

        DispenserBlock.registerBehavior(RequiemItems.FILLED_SOUL_VESSEL, new FilledVesselItemDispenserBehavior());
    }
}
