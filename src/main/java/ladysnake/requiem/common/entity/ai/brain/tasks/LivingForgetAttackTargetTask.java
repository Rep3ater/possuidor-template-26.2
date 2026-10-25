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
import java.util.function.Predicate;

public class LivingForgetAttackTargetTask<E extends LivingEntity> extends Behavior<E> {
    private final Predicate<LivingEntity> alternativeCondition;

    public LivingForgetAttackTargetTask(Predicate<LivingEntity> alternativeCondition) {
        super(ImmutableMap.of(
            MemoryModuleType.ATTACK_TARGET, MemoryStatus.VALUE_PRESENT,
            MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE, MemoryStatus.REGISTERED
        ));
        this.alternativeCondition = alternativeCondition;
    }

    public LivingForgetAttackTargetTask() {
        this((livingEntity) -> false);
    }

    @Override
    protected void start(ServerLevel serverWorld, E mobEntity, long l) {
        LivingEntity livingEntity = this.getAttackTarget(mobEntity);
        if (!livingEntity.isAttackable()) {
            this.forgetAttackTarget(mobEntity);
        } else if (cannotReachTarget(mobEntity)) {
            this.forgetAttackTarget(mobEntity);
        } else if (this.isAttackTargetDead(mobEntity)) {
            this.forgetAttackTarget(mobEntity);
        } else if (this.isAttackTargetInAnotherWorld(mobEntity)) {
            this.forgetAttackTarget(mobEntity);
        } else if (this.alternativeCondition.test(this.getAttackTarget(mobEntity))) {
            this.forgetAttackTarget(mobEntity);
        }
    }

    private boolean isAttackTargetInAnotherWorld(E entity) {
        return this.getAttackTarget(entity).level() != entity.level();
    }

    private LivingEntity getAttackTarget(E entity) {
        return entity.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET).orElseThrow(IllegalStateException::new);
    }

    private static <E extends LivingEntity> boolean cannotReachTarget(E entity) {
        Optional<Long> optional = entity.getBrain().getMemory(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE);
        return optional.isPresent() && entity.level().getGameTime() - optional.get() > 200L;
    }

    private boolean isAttackTargetDead(E entity) {
        Optional<LivingEntity> optional = entity.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET);
        return optional.isPresent() && !optional.get().isAlive();
    }

    private void forgetAttackTarget(E entity) {
        entity.getBrain().eraseMemory(MemoryModuleType.ATTACK_TARGET);
    }
}
