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
package ladysnake.requiem.common.entity.ai;

import ladysnake.requiem.common.entity.MorticianEntity;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;

public abstract class MorticianSpellGoal extends Goal {
    protected final MorticianEntity mortician;
    protected int spellCooldown;
    protected int nextCastTime;

    public MorticianSpellGoal(MorticianEntity mortician) {
        this.mortician = mortician;
    }

    @Override
    public void start() {
        this.spellCooldown = this.getWarmupTime();
        this.nextCastTime = this.mortician.tickCount + this.getCooldown();
        this.mortician.playSound(this.getSoundPrepare(), 1.0F, 1.0F);
        this.mortician.setSpellcasting(true);
    }

    @Override
    public boolean canUse() {
        LivingEntity target = this.mortician.getTarget();
        if (target != null && target.isAlive()) {
            if (this.mortician.isSpellcasting()) {
                return false;
            } else {
                return this.mortician.tickCount >= this.nextCastTime;
            }
        } else {
            return false;
        }
    }

    @Override
    public boolean canContinueToUse() {
        LivingEntity target = this.mortician.getTarget();
        return target != null && target.isAlive() && this.spellCooldown > 0;
    }

    @Override
    public void stop() {
        this.mortician.setSpellcasting(false);
    }

    @Override
    public void tick() {
        --this.spellCooldown;
        if (this.spellCooldown == 0) {
            this.castSpell();
        }
    }

    protected abstract void castSpell();

    protected abstract int getWarmupTime();

    protected abstract int getCooldown();

    protected abstract SoundEvent getSoundPrepare();
}
