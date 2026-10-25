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
package ladysnake.requiem.common.entity.ability;

import ladysnake.requiem.core.entity.ability.TickingGoalAbility;
import ladysnake.requiem.core.util.reflection.UncheckedReflectionException;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.Guardian;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

public class GuardianBeamAbility extends TickingGoalAbility<Guardian, LivingEntity> {
    public GuardianBeamAbility(Guardian owner) {
        super(owner, makeGoal(owner), 20 * 4, 15, LivingEntity.class);
    }

    private static final Constructor<? extends Goal> BEAM_GOAL_FACTORY;

    private static Goal makeGoal(Guardian owner) {
        try {
            return BEAM_GOAL_FACTORY.newInstance(owner);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new UncheckedReflectionException("Failed to instantiate GuardianAttackGoal", e);
        }
    }

    static {
        try {
            Class<?> clazz = Class.forName("net.minecraft.world.entity.monster.Guardian$GuardianAttackGoal");
            Constructor<? extends Goal> ctor = (Constructor<? extends Goal>) clazz.getDeclaredConstructor(Guardian.class);
            ctor.setAccessible(true);
            BEAM_GOAL_FACTORY = ctor;
        } catch (ClassNotFoundException e) {
            throw new UncheckedReflectionException("Could not find the GuardianAttackGoal class", e);
        } catch (NoSuchMethodException e) {
            throw new UncheckedReflectionException("Could not find the GuardianAttackGoal constructor", e);
        }
    }
}
