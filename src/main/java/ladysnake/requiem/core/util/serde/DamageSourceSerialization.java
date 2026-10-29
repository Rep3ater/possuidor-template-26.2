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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * License for more details.
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
package ladysnake.requiem.core.util.serde;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public final class DamageSourceSerialization {

    public static CompoundTag toTag(DamageSource damage) {
        CompoundTag tag = new CompoundTag();
        String typeId = damage.typeHolder().unwrapKey().map(k -> k.location().toString()).orElse("minecraft:generic");
        tag.putString("type", typeId);
        if (damage.getDirectEntity() != null) {
            tag.putString("sourceUuid", damage.getDirectEntity().getUUID().toString());
        }
        if (damage.getEntity() != null) {
            tag.putString("attackerUuid", damage.getEntity().getUUID().toString());
        }
        return tag;
    }

    private static Entity entityFromTag(ServerLevel world, CompoundTag tag, String key) {
        if (!tag.contains(key, net.minecraft.nbt.Tag.TAG_STRING)) return null;
        try {
            return world.getEntity(UUID.fromString(tag.getString(key)));
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    public static DamageSource fromTag(CompoundTag tag, @Nullable ServerLevel world) {
        if (world == null) {
            return null;
        }
        String typeId = tag.contains("type", net.minecraft.nbt.Tag.TAG_STRING) ? tag.getString("type") : "minecraft:generic";
        ResourceKey<DamageType> key = ResourceKey.create(Registries.DAMAGE_TYPE, ResourceLocation.parse(typeId));
        Holder<DamageType> holder = world.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).get(key).orElseGet(() -> world.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(DamageTypes.GENERIC));

        Entity source = entityFromTag(world, tag, "sourceUuid");

        Entity attacker = entityFromTag(world, tag, "attackerUuid");

        return new DamageSource(holder, source, attacker);
    }
}
