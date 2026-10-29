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

import ladysnake.requiem.common.entity.WololoComponent;
import ladysnake.requiem.common.entity.internal.SpellcastingIllagerAccess;
import ladysnake.requiem.core.entity.ability.DirectAbilityBase;
import ladysnake.requiem.mixin.common.possession.gameplay.ability.EvokerEntityAccessor;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.entity.monster.Evoker;
import org.jetbrains.annotations.Nullable;

public class EvokerWololoAbility extends DirectAbilityBase<Evoker, Entity> {
    private final CustomWololoGoal wololoGoal;
    private boolean started;

    public EvokerWololoAbility(Evoker owner) {
        super(owner, 0, 16, Entity.class);
        this.wololoGoal = new CustomWololoGoal(owner);
    }

    @Override
    public boolean canTarget(Entity target) {
        if (!super.canTarget(target)) return false;
        if (target instanceof Sheep sheep) {
            if (this.owner.level() instanceof ServerLevel serverLevel) {
                return this.wololoGoal.requiem_getConvertibleSheepPredicate().test(serverLevel, this.owner, sheep);
            }
            return false;
        }
        return WololoComponent.canBeConverted(target);
    }

    @Override
    public boolean run(Entity target) {
        if (this.owner.level().isClientSide()) return true;

        this.wololoGoal.target = target;

        if (this.wololoGoal.canUse()) {
            this.wololoGoal.start();
            this.beginCooldown();
            this.started = true;
            return true;
        } else {
            this.wololoGoal.target = null;
            return false;
        }
    }

    @Override
    public void update() {
        super.update();
        if (this.started) {
            if (this.wololoGoal.canContinueToUse()) {
                this.wololoGoal.tick();
            } else {
                this.started = false;
                this.wololoGoal.stop();
                this.owner.setIsCastingSpell(SpellcastingIllagerAccess.SPELL_NONE);
            }
        }
    }

    final class CustomWololoGoal extends Evoker.EvokerWololoSpellGoal implements ExtendedWololoGoal {
        private @Nullable Entity target;

        private CustomWololoGoal(Evoker evoker) {
            evoker.super();
        }

        @Override
        public boolean canContinueToUse() {
            return this.requiem_hasValidTarget() && this.attackWarmupDelay > 0;
        }

        @Override
        protected void performSpellCasting() {
            assert this.target != null;
            if (this.target instanceof Sheep sheep) {
                ((EvokerEntityAccessor) EvokerWololoAbility.this.owner).requiem$invokeSetWololoTarget(sheep);
                super.performSpellCasting();
            } else {
                WololoComponent.KEY.maybeGet(this.target).ifPresent(WololoComponent::wololo);
            }
        }

        @Override
        public void stop() {
            super.stop();
            this.target = null;
        }

        @Override
        public boolean requiem_hasValidTarget() {
            return this.target != null && EvokerWololoAbility.this.canTarget(this.target);
        }
    }
}
