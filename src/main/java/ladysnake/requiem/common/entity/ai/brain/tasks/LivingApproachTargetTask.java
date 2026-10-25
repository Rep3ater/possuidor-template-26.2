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

import baritone.api.fakeplayer.FakeServerPlayerEntity;
import baritone.api.pathing.goals.GoalNear;
import baritone.api.pathing.goals.GoalRunAway;
import com.google.common.collect.ImmutableMap;
import ladysnake.requiem.common.entity.ai.brain.AutomatoneWalkTarget;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.behavior.EntityTracker;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.memory.WalkTarget;

public class LivingApproachTargetTask extends Behavior<FakeServerPlayerEntity> {
    private final float speed;

    public LivingApproachTargetTask(float speed) {
        super(ImmutableMap.of(
            MemoryModuleType.WALK_TARGET, MemoryStatus.REGISTERED,
            MemoryModuleType.LOOK_TARGET, MemoryStatus.REGISTERED,
            MemoryModuleType.ATTACK_TARGET, MemoryStatus.VALUE_PRESENT,
            MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES, MemoryStatus.REGISTERED
        ));
        this.speed = speed;
    }

    @Override
    protected void start(ServerLevel serverWorld, FakeServerPlayerEntity executor, long l) {
        LivingEntity livingEntity = executor.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET).orElseThrow(IllegalStateException::new);
        this.rememberWalkTarget(executor, livingEntity);
    }

    private void rememberWalkTarget(FakeServerPlayerEntity executor, LivingEntity target) {
        Brain<?> brain = executor.getBrain();
        brain.setMemory(MemoryModuleType.LOOK_TARGET, new EntityTracker(target, true));
        WalkTarget walkTarget = new AutomatoneWalkTarget(
            new EntityTracker(target, false),
            this.speed,
            new GoalKeepDistanceWithTarget(
                target.blockPosition(),
                (int) PlayerMeleeTask.getAttackRange(executor),
                2.5,
                executor.blockPosition(),
                executor.tickCount - executor.getLastHurtMobTimestamp() == 1 && executor.getRandom().nextBoolean() ? 1 : 0
            )
        );
        brain.setMemory(MemoryModuleType.WALK_TARGET, walkTarget);
    }

    public static class GoalKeepDistanceWithTarget extends GoalNear {
        private final GoalRunAway avoidTarget;
        private final GoalRunAway avoidSrc;

        public GoalKeepDistanceWithTarget(BlockPos pos, int range, double avoidRange, BlockPos src, double avoidSrcRange) {
            super(pos, range);
            this.avoidTarget = new GoalRunAway(avoidRange, pos);
            this.avoidSrc = new GoalRunAway(avoidSrcRange, src);
        }

        @Override
        public boolean isInGoal(int x, int y, int z) {
            return super.isInGoal(x, y, z) && this.avoidTarget.isInGoal(x, y, z) && this.avoidSrc.isInGoal(x, y, z);
        }

        @Override
        public double heuristic(int x, int y, int z) {
            return Math.max(
                Math.max(super.heuristic(x, y, z), this.avoidTarget.heuristic(x, y, z)),
                this.avoidSrc.heuristic(x, y, z)
            );
        }
    }
}
