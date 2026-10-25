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
import ladysnake.requiem.api.v1.possession.Possessable;
import ladysnake.requiem.api.v1.possession.PossessionComponent;
import net.minecraft.advancements.predicates.entity.EntityPredicate;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.Set;

public class HostLootCondition implements LootItemCondition {
    public static final MapCodec<HostLootCondition> HOST_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        LootContext.EntityTarget.CODEC.fieldOf("entity").forGetter(c -> c.entity),
        EntityPredicate.CODEC.optionalFieldOf("predicate").forGetter(c -> Optional.ofNullable(c.predicate))
    ).apply(instance, (target, pred) -> new HostLootCondition(CheckedEntity.HOST, target, pred.orElse(null))));

    public static final MapCodec<HostLootCondition> POSSESSOR_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        LootContext.EntityTarget.CODEC.fieldOf("entity").forGetter(c -> c.entity),
        EntityPredicate.CODEC.optionalFieldOf("predicate").forGetter(c -> Optional.ofNullable(c.predicate))
    ).apply(instance, (target, pred) -> new HostLootCondition(CheckedEntity.POSSESSOR, target, pred.orElse(null))));

    private final CheckedEntity checkedEntity;
    private final LootContext.EntityTarget entity;
    @Nullable
    private final EntityPredicate predicate;

    public HostLootCondition(CheckedEntity checkedEntity, LootContext.EntityTarget entity, @Nullable EntityPredicate predicate) {
        this.checkedEntity = checkedEntity;
        this.entity = entity;
        this.predicate = predicate;
    }

    @Override
    public MapCodec<? extends LootItemCondition> codec() {
        return checkedEntity == CheckedEntity.HOST ? HOST_CODEC : POSSESSOR_CODEC;
    }

    @Override
    public Set<ContextKey<?>> getReferencedContextParams() {
        return ImmutableSet.of(LootContextParams.ORIGIN, this.entity.contextParam());
    }

    @Override
    public boolean test(LootContext lootContext) {
        if (!(lootContext.getOptionalParameter(this.entity.contextParam()) instanceof LivingEntity e)) return false;
        if (predicate == null) return true;

        LivingEntity target = switch (checkedEntity) {
            case HOST -> PossessionComponent.getHost(e);
            case POSSESSOR -> e instanceof Possessable p ? p.getPossessor() : null;
        };

        return predicate.matches(lootContext.getLevel(), lootContext.getOptionalParameter(LootContextParams.ORIGIN), target);
    }

    public enum CheckedEntity {
        HOST,
        POSSESSOR
    }
}
