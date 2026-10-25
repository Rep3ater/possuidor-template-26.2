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

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import ladysnake.requiem.api.v1.record.EntityPointer;
import net.minecraft.advancements.predicates.DistancePredicate;
import net.minecraft.advancements.predicates.entity.EntityPredicate;
import net.minecraft.advancements.predicates.LocationPredicate;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public record EntityRefPredicate(Optional<DistancePredicate> distance, Optional<LocationPredicate> location, Optional<EntityPredicate> entity) {
    public static final EntityRefPredicate ANY = new EntityRefPredicate(
        Optional.empty(),
        Optional.empty(),
        Optional.empty()
    );

    public static final Codec<EntityRefPredicate> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        DistancePredicate.CODEC.optionalFieldOf("distance").forGetter(EntityRefPredicate::distance),
        LocationPredicate.CODEC.optionalFieldOf("location").forGetter(EntityRefPredicate::location),
        EntityPredicate.CODEC.optionalFieldOf("entity").forGetter(EntityRefPredicate::entity)
    ).apply(instance, EntityRefPredicate::new));

    public boolean test(ServerLevel world, @Nullable Vec3 origin, @Nullable EntityPointer entityPointer) {
        if (entityPointer == null) {
            return false;
        }

        Vec3 loc = entityPointer.pos();

        if (this.distance.isPresent()) {
            if (origin == null || !this.distance.get().matches(origin.x, origin.y, origin.z, loc.x, loc.y, loc.z)) {
                return false;
            }
        }

        if (this.location.isPresent() && !this.location.get().matches(world, loc.x, loc.y, loc.z)) {
            return false;
        }

        if (this.entity.isPresent()) {
            @Nullable Entity entityObj = entityPointer.resolve(world.getServer()).orElse(null);
            return this.entity.get().matches(world, origin, entityObj);
        }

        return true;
    }
}
