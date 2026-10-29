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
package ladysnake.requiem.common.entity.cure;

import ladysnake.requiem.Requiem;
import ladysnake.requiem.common.entity.RequiemEntities;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ConversionParams;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.zombie.ZombifiedPiglin;
import org.jetbrains.annotations.Nullable;

public class CurableZombifiedPiglinComponent extends SimpleCurableEntityComponent {
    private @Nullable EntityType<?> originalPiglinType;

    public CurableZombifiedPiglinComponent(ZombifiedPiglin zombie) {
        super(zombie);
    }

    @Override
    public boolean canBeAssimilated() {
        return false;
    }

    @Override
    public boolean canBeCured() {
        return true;
    }

    @Override
    protected @Nullable Mob createCuredEntity() {
        @SuppressWarnings("unchecked") EntityType<? extends Mob> originalPiglinType = (EntityType<? extends Mob>) this.originalPiglinType;
        if (originalPiglinType != null) {
            try {
                return this.entity.convertTo(getCuredVariant(originalPiglinType), ConversionParams.single(this.entity, true, false), cured -> {});
            } catch (ClassCastException e) {
                Requiem.LOGGER.error("[Requiem] Invalid original piglin type", e);
            }
        }
        return this.entity.convertTo(getCuredVariant(EntityTypes.PIGLIN), ConversionParams.single(this.entity, true, false), cured -> {});
    }

    private EntityType<? extends Mob> getCuredVariant(EntityType<? extends Mob> originalPiglinType) {
        // TODO if the current dimension is piglin friendly, revert to originalPiglinType
        return RequiemEntities.CURED_PIGLIN_VARIANTS.getOrDefault(originalPiglinType, originalPiglinType);
    }

    public void setOriginalPiglinType(EntityType<?> originalPiglinType) {
        this.originalPiglinType = originalPiglinType;
    }

    @Override
    public void readFromNbt(CompoundTag tag) {
        super.readFromNbt(tag);
        if (tag.contains("original_piglin_type")) {
            this.originalPiglinType = tag.getString("original_piglin_type").map(ResourceLocation::tryParse).flatMap(BuiltInRegistries.ENTITY_TYPE::getOptional).orElse(null);
        }
    }

    @Override
    public void writeToNbt(CompoundTag tag) {
        super.writeToNbt(tag);
        if (this.originalPiglinType != null) {
            tag.putString("original_piglin_type", BuiltInRegistries.ENTITY_TYPE.getKey(this.originalPiglinType).toString());
        }
    }
}
