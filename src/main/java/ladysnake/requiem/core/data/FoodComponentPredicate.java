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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * License for more details.
 *
 * You should have received a copy of the GNU General Public License
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
package ladysnake.requiem.core.data;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import net.minecraft.advancements.critereon.MinMaxBounds;
import net.minecraft.core.component.DataComponents;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public record FoodComponentPredicate(
    MinMaxBounds.Ints hunger,
    MinMaxBounds.Doubles saturationModifier,
    @Nullable Boolean alwaysEdible
) {
    public static final FoodComponentPredicate ANY = new FoodComponentPredicate(MinMaxBounds.Ints.ANY, MinMaxBounds.Doubles.ANY, null);

    public static FoodComponentPredicate fromJson(@Nullable JsonElement json) {
        if (json != null && !json.isJsonNull()) {
            JsonObject jsonObject = GsonHelper.convertToJsonObject(json, "requiem:food");
            MinMaxBounds.Ints hunger = jsonObject.has("hunger") ? MinMaxBounds.Ints.CODEC.parse(JsonOps.INSTANCE, jsonObject.get("hunger")).result().orElse(MinMaxBounds.Ints.ANY) : MinMaxBounds.Ints.ANY;
            MinMaxBounds.Doubles saturation = jsonObject.has("saturation") ? MinMaxBounds.Doubles.CODEC.parse(JsonOps.INSTANCE, jsonObject.get("saturation")).result().orElse(MinMaxBounds.Doubles.ANY) : MinMaxBounds.Doubles.ANY;
            Boolean alwaysEdible = jsonObject.has("always_edible") ? GsonHelper.getAsBoolean(jsonObject, "always_edible") : null;
            return new FoodComponentPredicate(hunger, saturation, alwaysEdible);
        }
        return ANY;
    }

    public boolean test(ItemStack stack) {
        if (this == ANY) return true;

        FoodProperties food = stack.get(DataComponents.FOOD);
        if (food == null) {
            return false;
        } else if (!this.hunger.matches(food.nutrition())) {
            return false;
        } else if (!this.saturationModifier.matches(food.saturation())) {
            return false;
        } else if (this.alwaysEdible != null && this.alwaysEdible != food.canAlwaysEat()) {
            return false;
        }
        return true;
    }
}
