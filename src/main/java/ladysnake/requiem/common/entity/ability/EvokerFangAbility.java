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

import ladysnake.requiem.api.v1.possession.Possessable;
import ladysnake.requiem.common.entity.internal.SpellcastingIllagerAccess;
import ladysnake.requiem.core.entity.ability.DirectAbilityBase;
import ladysnake.requiem.core.util.reflection.UncheckedReflectionException;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.illager.Evoker;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

public class EvokerFangAbility extends DirectAbilityBase<Evoker, LivingEntity> {
    private static final Constructor<? extends Goal> FANGS_GOAL_FACTORY;
    public static final int FANG_COOLDOWN = 40;
    public static final int HOSTILE_TIME = 200;

    private final Goal conjureFangsGoal;
    private int hostileTime;

    public EvokerFangAbility(Evoker owner) {
        super(owner, FANG_COOLDOWN, 12, LivingEntity.class);
        this.conjureFangsGoal = makeGoal(owner);
    }

    @Override
    public boolean canTarget(LivingEntity target) {
        return super.canTarget(target) && target.isAlive();
    }

    @Override
    public boolean run(LivingEntity entity) {
        if (this.owner.level().isClientSide()) return true;

        // We are not resetting the target afterwards, as the vexes need it
        this.owner.setTarget(entity);

        if (this.conjureFangsGoal.canUse()) {
            this.castSpell();
            this.owner.setIsCastingSpell(SpellcastingIllagerAccess.SPELL_FANGS);
            this.beginCooldown();
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void update() {
        super.update();

        if (this.hostileTime > 0) {
            this.hostileTime--;

            if (this.hostileTime == 0 || !((Possessable) this.owner).isBeingPossessed()) {
                this.owner.setTarget(null);
            }
        }
    }

    private void castSpell() {
        try {
            SpellcastingIllagerAccess.CAST_SPELL_GOAL$CAST_SPELL.invoke(conjureFangsGoal);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new UncheckedReflectionException("Failed to trigger evoker fang ability", e);
        }
    }

    @Override
    public void onCooldownEnd() {
        if (!this.owner.level().isClientSide()) {
            this.owner.setIsCastingSpell(SpellcastingIllagerAccess.SPELL_NONE);
        }
    }

    private static Goal makeGoal(Evoker owner) {
        try {
            return FANGS_GOAL_FACTORY.newInstance(owner);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new UncheckedReflectionException(e);
        }
    }

    static {
        try {
            Class<?> clazz = Class.forName("net.minecraft.world.entity.monster.illager.Evoker$EvokerAttackSpellGoal");
            Constructor<? extends Goal> ctor = (Constructor<? extends Goal>) clazz.getDeclaredConstructor(Evoker.class);
            ctor.setAccessible(true);
            FANGS_GOAL_FACTORY = ctor;
        } catch (ClassNotFoundException e) {
            throw new UncheckedReflectionException("Could not find the EvokerAttackSpellGoal class", e);
        } catch (NoSuchMethodException e) {
            throw new UncheckedReflectionException("Could not find the EvokerAttackSpellGoal constructor", e);
        }
    }
}
