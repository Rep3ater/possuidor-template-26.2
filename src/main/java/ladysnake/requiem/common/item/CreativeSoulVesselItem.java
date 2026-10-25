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

import ladysnake.requiem.Requiem;
import ladysnake.requiem.common.entity.RequiemEntityAttributes;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.component.ItemAttributeModifiers;

public class CreativeSoulVesselItem extends EmptySoulVesselItem {
    public CreativeSoulVesselItem(Item.Properties settings) {
        super(settings.attributes(
            ItemAttributeModifiers.builder()
                .add(RequiemEntityAttributes.SOUL_OFFENSE, new AttributeModifier(Requiem.id("creative_vessel_modifier"), 1E12, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.HAND)
                .build()
        ));
    }
}
