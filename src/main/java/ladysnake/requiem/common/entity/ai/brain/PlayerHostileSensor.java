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
package ladysnake.requiem.common.entity.ai.brain;

import com.google.common.collect.ImmutableSet;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.behavior.BehaviorUtils;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.NearestVisibleLivingEntities;
import net.minecraft.world.entity.ai.sensing.Sensor;
import net.minecraft.world.entity.player.Player;

import java.util.Optional;
import java.util.Set;

public class PlayerHostileSensor extends Sensor<Player> {
    public PlayerHostileSensor() {
        super(10);
    }

    @Override
    public Set<MemoryModuleType<?>> requires() {
        return ImmutableSet.of(MemoryModuleType.NEAREST_HOSTILE, MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES);
    }

    @Override
    protected void doTick(ServerLevel world, Player entity) {
        entity.getBrain().setMemory(MemoryModuleType.NEAREST_HOSTILE, this.getNearestHostile(entity));
    }

    private Optional<LivingEntity> getNearestHostile(Player subject) {
        return this.getVisibleMobs(subject).flatMap(mobs -> mobs.findClosest(entity ->
            this.isHostile(subject, entity) && this.isCloseEnoughForDanger(subject, entity)
        ));
    }

    private boolean isCloseEnoughForDanger(Player subject, LivingEntity entity) {
        if (entity instanceof Mob mobEntity) {
            return BehaviorUtils.isWithinAttackRange(mobEntity, subject, 0);
        }
        return entity instanceof Player;
    }

    private boolean isHostile(Player subject, LivingEntity livingEntity) {
        return livingEntity instanceof Mob mob && mob.getTarget() == subject;
    }

    private Optional<NearestVisibleLivingEntities> getVisibleMobs(LivingEntity entity) {
        return entity.getBrain().getMemory(MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES);
    }
}
