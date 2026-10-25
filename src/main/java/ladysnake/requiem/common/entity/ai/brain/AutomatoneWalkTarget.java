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

import baritone.api.pathing.goals.Goal;
import net.minecraft.world.entity.ai.behavior.PositionTracker;
import net.minecraft.world.entity.ai.memory.WalkTarget;

public class AutomatoneWalkTarget extends WalkTarget {
    private final Goal goal;

    public AutomatoneWalkTarget(PositionTracker lookTarget, float speed, Goal goal) {
        super(lookTarget, speed, 0);
        this.goal = goal;
    }

    public Goal getGoal() {
        return goal;
    }
}
