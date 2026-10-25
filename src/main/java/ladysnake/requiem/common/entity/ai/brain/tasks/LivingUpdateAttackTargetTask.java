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
package ladysnake.requiem.common.entity.ai.brain.tasks;

import com.google.common.collect.ImmutableMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;

import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

public class LivingUpdateAttackTargetTask<E extends LivingEntity> extends Behavior<E> {
    private final Predicate<E> startCondition;
    private final Function<E, Optional<? extends LivingEntity>> targetGetter;

    public LivingUpdateAttackTargetTask(Predicate<E> startCondition, Function<E, Optional<? extends LivingEntity>> targetGetter) {
        super(ImmutableMap.of(
            MemoryModuleType.ATTACK_TARGET, MemoryStatus.VALUE_ABSENT,
            MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE, MemoryStatus.REGISTERED
        ));
        this.startCondition = startCondition;
        this.targetGetter = targetGetter;
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel serverWorld, E executor) {
        if (!this.startCondition.test(executor)) {
            return false;
        } else {
            Optional<? extends LivingEntity> optional = this.targetGetter.apply(executor);
            return optional.isPresent() && optional.get().isAlive();
        }
    }

    @Override
    protected void start(ServerLevel serverWorld, E executor, long l) {
        this.targetGetter.apply(executor).ifPresent((livingEntity) -> this.updateAttackTarget(executor, livingEntity));
    }

    private void updateAttackTarget(E executor, LivingEntity target) {
        executor.getBrain().setMemory(MemoryModuleType.ATTACK_TARGET, target);
        executor.getBrain().eraseMemory(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE);
    }
}
