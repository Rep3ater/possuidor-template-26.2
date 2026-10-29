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
package ladysnake.requiem.common.loot;

import com.google.common.collect.ImmutableSet;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import ladysnake.requiem.common.entity.MorticianEntity;
import net.minecraft.world.level.storage.loot.parameters.LootContextParam;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

import java.util.Set;

public class RiftMorticianLootCondition implements LootItemCondition {
    @Override
    public net.minecraft.world.level.storage.loot.predicates.LootItemConditionType getType() {
        return RequiemLootTables.RIFT_MORTICIAN_CONDITION;
    }

    public static final MapCodec<RiftMorticianLootCondition> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        LootContext.EntityTarget.CODEC.fieldOf("entity").forGetter(c -> c.entity)
    ).apply(instance, RiftMorticianLootCondition::new));

    private final LootContext.EntityTarget entity;

    public RiftMorticianLootCondition(LootContext.EntityTarget entity) {
        this.entity = entity;
    }

    @Override
    public Set<LootContextParam<?>> getReferencedContextParams() {
        return ImmutableSet.of(this.entity.getParam());
    }

    @Override
    public boolean test(LootContext lootContext) {
        return lootContext.getParamOrNull(this.entity.getParam()) instanceof MorticianEntity mortician && mortician.isObeliskProjection();
    }
}
