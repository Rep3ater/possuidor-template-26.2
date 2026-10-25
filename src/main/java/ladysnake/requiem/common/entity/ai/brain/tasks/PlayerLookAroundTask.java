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
import baritone.api.utils.RotationUtils;
import com.google.common.collect.ImmutableMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;

public class PlayerLookAroundTask extends Behavior<FakeServerPlayerEntity> {
    public PlayerLookAroundTask(int minRunTime, int maxRunTime) {
        super(ImmutableMap.of(MemoryModuleType.LOOK_TARGET, MemoryStatus.VALUE_PRESENT), minRunTime, maxRunTime);
    }

    @Override
    protected boolean canStillUse(ServerLevel serverWorld, FakeServerPlayerEntity player, long l) {
        return player.getBrain().getMemory(MemoryModuleType.LOOK_TARGET)
            .filter(lookTarget -> lookTarget.isVisibleBy(player))
            .isPresent();
    }

    @Override
    protected void stop(ServerLevel serverWorld, FakeServerPlayerEntity player, long l) {
        player.getBrain().eraseMemory(MemoryModuleType.LOOK_TARGET);
    }

    @Override
    protected void tick(ServerLevel serverWorld, FakeServerPlayerEntity player, long l) {
        player.getBrain().getMemory(MemoryModuleType.LOOK_TARGET).ifPresent((lookTarget) ->
            player.getBaritone().getLookBehavior().updateSecondaryTarget(
                RotationUtils.calcRotationFromVec3d(player.getEyePosition(1.0F), lookTarget.currentPosition())));
    }
}
