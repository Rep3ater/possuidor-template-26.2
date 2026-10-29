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
import ladysnake.requiem.api.v1.record.EntityPointer;
import ladysnake.requiem.common.RequiemRecordTypes;
import ladysnake.requiem.common.remnant.PlayerBodyTracker;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.loot.parameters.LootContextParam;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

public class BoundShellLootCondition implements LootItemCondition {
    @Override
    public net.minecraft.world.level.storage.loot.predicates.LootItemConditionType getType() {
        return RequiemLootTables.BOUND_SHELL_CONDITION;
    }

    public static final MapCodec<BoundShellLootCondition> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        LootContext.EntityTarget.CODEC.fieldOf("entity").forGetter(c -> c.entity),
        EntityRefPredicate.CODEC.optionalFieldOf("predicate", EntityRefPredicate.ANY).forGetter(c -> c.predicate)
    ).apply(instance, BoundShellLootCondition::new));

    private final LootContext.EntityTarget entity;
    private final EntityRefPredicate predicate;

    public BoundShellLootCondition(LootContext.EntityTarget entity, EntityRefPredicate predicate) {
        this.entity = entity;
        this.predicate = predicate;
    }

    @Override
    public Set<LootContextParam<?>> getReferencedContextParams() {
        return ImmutableSet.of(LootContextParams.ORIGIN, this.entity.getParam());
    }

    @Override
    public boolean test(LootContext lootContext) {
        if (!(lootContext.getParamOrNull(this.entity.getParam()) instanceof ServerPlayer player)) return false;

        @Nullable EntityPointer shellPointer = PlayerBodyTracker.get(player)
            .getAnchor()
            .flatMap(record -> record.get(RequiemRecordTypes.ENTITY_REF))
            .orElse(null);

        return this.predicate.test(lootContext.getLevel(), lootContext.getParamOrNull(LootContextParams.ORIGIN), shellPointer);
    }
}
