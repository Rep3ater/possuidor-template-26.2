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

import baritone.api.Settings;
import baritone.api.pathing.goals.Goal;
import baritone.api.pathing.goals.GoalNear;
import com.google.common.collect.ImmutableMap;
import ladysnake.requiem.common.entity.PlayerShellEntity;
import ladysnake.requiem.common.entity.ai.brain.AutomatoneWalkTarget;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.memory.WalkTarget;

public class PlayerWanderAroundTask extends Behavior<PlayerShellEntity> {
    public PlayerWanderAroundTask() {
        super(ImmutableMap.of(
            MemoryModuleType.WALK_TARGET, MemoryStatus.VALUE_PRESENT
        ), Integer.MAX_VALUE);
    }

    @Override
    protected void start(ServerLevel world, PlayerShellEntity executor, long time) {
        WalkTarget target = executor.getBrain().getMemory(MemoryModuleType.WALK_TARGET).orElseThrow(IllegalStateException::new);
        float speed = target.getSpeedModifier();
        Settings settings = executor.getBaritone().settings();
        if (speed <= 0) {
            throw new IllegalStateException("Some task asked to walk towards something with a negative speed ?!");
        }
        settings.allowSprint.set(speed > 0.5);
        settings.allowParkour.set(speed >= 1.);
        double terrainModificationFactor = 1. / Mth.clamp(speed * speed, 0.1, 1.);
        settings.blockBreakAdditionalPenalty.set(settings.blockBreakAdditionalPenalty.defaultValue() * terrainModificationFactor);
        settings.blockPlacementPenalty.set(settings.blockPlacementPenalty.defaultValue() * terrainModificationFactor);
        executor.getPathfindingProcess().setGoal(this.getGoal(target));
    }

    private Goal getGoal(WalkTarget target) {
        if (target instanceof AutomatoneWalkTarget automatoneWalkTarget) {
            return automatoneWalkTarget.getGoal();
        }
        return new GoalNear(target.getTarget().currentBlockPosition(), target.getCloseEnoughDist());
    }

    @Override
    protected boolean canStillUse(ServerLevel world, PlayerShellEntity entity, long time) {
        return entity.getPathfindingProcess().isActive();
    }
}
