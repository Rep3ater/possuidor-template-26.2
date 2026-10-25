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
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package ladysnake.requiem.common.entity;

import ladysnake.requiem.Requiem;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.RegisterEvent;

public final class RequiemEntityAttributes {
    public static final Attribute SOUL_OFFENSE_RAW = new RangedAttribute("attribute.name.remnant.requiem.soul_offense", 15, 0, 1024).setSyncable(true);
    public static final Attribute SOUL_DEFENSE_RAW = new RangedAttribute("attribute.name.remnant.requiem.soul_defense", 0, -1024, 1024).setSyncable(true);

    public static final DeferredHolder<Attribute, Attribute> SOUL_OFFENSE = DeferredHolder.create(Registries.ATTRIBUTE, Requiem.id("soul_offense"));
    public static final DeferredHolder<Attribute, Attribute> SOUL_DEFENSE = DeferredHolder.create(Registries.ATTRIBUTE, Requiem.id("soul_defense"));

    public static void register(RegisterEvent.RegisterHelper<Attribute> helper) {
        helper.register(Requiem.id("soul_offense"), SOUL_OFFENSE_RAW);
        helper.register(Requiem.id("soul_defense"), SOUL_DEFENSE_RAW);
    }

    public static void init() {
        // Registered via RegisterEvent
    }
}
