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
import ladysnake.requiem.common.entity.ai.brain.PandemoniumMemoryModules;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.memory.WalkTarget;

import java.util.Optional;

public class PlayerGoHomeTask extends Behavior<LivingEntity> {
    private final MemoryModuleType<GlobalPos> destination;
    private final float speed;
    private final int completionRange;
    private final int maxAttempts;

    public PlayerGoHomeTask(MemoryModuleType<GlobalPos> destination, float speed, int completionRange, int maxAttempts) {
        super(ImmutableMap.of(
            PandemoniumMemoryModules.GO_HOME_ATTEMPTS, MemoryStatus.REGISTERED,
            MemoryModuleType.WALK_TARGET, MemoryStatus.VALUE_ABSENT,
            destination, MemoryStatus.VALUE_PRESENT
        ));
        this.destination = destination;
        this.speed = speed;
        this.completionRange = completionRange;
        this.maxAttempts = maxAttempts;
    }

    private void giveUp(LivingEntity executor, long time) {
        Brain<?> brain = executor.getBrain();
        brain.setMemory(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE, time);
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel world, LivingEntity executor) {
        return !this.shouldGiveUp(executor);
    }

    @Override
    protected void start(ServerLevel serverWorld, LivingEntity executor, long l) {
        Brain<?> brain = executor.getBrain();
        brain.getMemory(this.destination).ifPresent((globalPos) -> {
            if (!this.isInOtherDimension(serverWorld, globalPos) && !this.shouldGiveUp(executor)) {
                if (this.reachedDestination(serverWorld, executor, globalPos)) {
                    brain.eraseMemory(PandemoniumMemoryModules.GO_HOME_ATTEMPTS);
                } else {
                    brain.setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(globalPos.pos(), this.speed, this.completionRange));
                    brain.setMemory(PandemoniumMemoryModules.GO_HOME_ATTEMPTS, brain.getMemory(PandemoniumMemoryModules.GO_HOME_ATTEMPTS).orElse(0) + 1);
                }
            } else {
                this.giveUp(executor, l);
            }
        });
    }

    private boolean shouldGiveUp(LivingEntity executor) {
        Optional<Integer> optional = executor.getBrain().getMemory(PandemoniumMemoryModules.GO_HOME_ATTEMPTS);
        return optional.filter(attempts -> attempts > this.maxAttempts).isPresent();
    }

    private boolean isInOtherDimension(ServerLevel currentWorld, GlobalPos target) {
        return target.dimension() != currentWorld.dimension();
    }

    private boolean reachedDestination(ServerLevel world, LivingEntity executor, GlobalPos pos) {
        return pos.dimension() == world.dimension() && pos.pos().distManhattan(executor.blockPosition()) <= this.completionRange;
    }
}
