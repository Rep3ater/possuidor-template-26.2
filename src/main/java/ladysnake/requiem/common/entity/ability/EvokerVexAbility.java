/*
 * Requiem
 * Copyright (C) 2019-2023 Ladysnake
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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package ladysnake.requiem.common.entity.ability;

import ladysnake.requiem.common.entity.internal.SpellcastingIllagerAccess;
import ladysnake.requiem.core.entity.ability.IndirectAbilityBase;
import ladysnake.requiem.core.util.reflection.UncheckedReflectionException;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.illager.Evoker;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

public class EvokerVexAbility extends IndirectAbilityBase<Evoker> {
    private static final Constructor<? extends Goal> VEX_GOAL_FACTORY;

    private final Goal summonVexGoal;
    private boolean started;

    public EvokerVexAbility(Evoker owner) {
        super(owner, 0);
        try {
            this.summonVexGoal = VEX_GOAL_FACTORY.newInstance(owner);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new UncheckedReflectionException("Failed to instantiate SummonVexGoal", e);
        }
    }

    @Override
    public boolean run() {
        if (this.owner.level().isClientSide()) return true;

        boolean hasTarget = owner.getTarget() != null;
        if (!hasTarget) owner.setTarget(owner); // Need to have some kind of target to cast the spell

        try {
            if (this.summonVexGoal.canUse()) {
                this.summonVexGoal.start();
                this.started = true;
                this.beginCooldown();
                return true;
            }
        } finally {
            if (!hasTarget) owner.setTarget(null);
        }

        return false;
    }

    @Override
    public void update() {
        super.update();
        if (started) {
            boolean hasTarget = owner.getTarget() != null;
            if (!hasTarget) owner.setTarget(owner); // Need to have some kind of target to cast the spell

            try {
                if (summonVexGoal.canContinueToUse()) {
                    summonVexGoal.tick();
                } else {
                    started = false;
                    this.summonVexGoal.stop();
                    owner.setIsCastingSpell(SpellcastingIllagerAccess.SPELL_NONE);
                }
            } finally {
                if (!hasTarget) owner.setTarget(null);
            }
        }
    }

    static {
        try {
            Class<?> clazz = Class.forName("net.minecraft.world.entity.monster.illager.Evoker$EvokerSummonSpellGoal");
            Constructor<? extends Goal> ctor = (Constructor<? extends Goal>) clazz.getDeclaredConstructor(Evoker.class);
            ctor.setAccessible(true);
            VEX_GOAL_FACTORY = ctor;
        } catch (ClassNotFoundException e) {
            throw new UncheckedReflectionException("Could not find the EvokerSummonSpellGoal class", e);
        } catch (NoSuchMethodException e) {
            throw new UncheckedReflectionException("Could not get the EvokerSummonSpellGoal constructor", e);
        }
    }
}
