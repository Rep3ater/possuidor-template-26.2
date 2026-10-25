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

import ladysnake.requiem.Requiem;
import ladysnake.requiem.core.entity.ability.IndirectAbilityBase;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.axolotl.Axolotl;

import java.util.Objects;

public class AxolotlPlayingDeadAbility extends IndirectAbilityBase<Axolotl> {
    public static final int COOLDOWN = 20 * 30;
    public static final int ABILITY_TIME = 200;
    public static final Identifier SPEED_MODIFIER_ID = Requiem.id("playing_dead_slowdown");
    public static final int MAX_HP_HEALED = 4;

    private int ticksLeft;

    public AxolotlPlayingDeadAbility(Axolotl owner) {
        super(owner, COOLDOWN);
    }

    @Override
    protected boolean run() {
        int missingHealth = (int) (this.owner.getMaxHealth() - this.owner.getHealth());
        if (missingHealth > 0) {
            this.ticksLeft = Math.round(ABILITY_TIME / Math.min(1F, (float) missingHealth / MAX_HP_HEALED));
            AttributeInstance speedAttr = Objects.requireNonNull(this.owner.getAttribute(Attributes.MOVEMENT_SPEED));
            speedAttr.removeModifier(SPEED_MODIFIER_ID);
            speedAttr.addTransientModifier(new AttributeModifier(SPEED_MODIFIER_ID, -0.9, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
            this.owner.addEffect(new MobEffectInstance(MobEffects.REGENERATION, ticksLeft));
            return true;
        }
        return false;
    }

    @Override
    public void update() {
        super.update();
        if (this.ticksLeft > 0) {
            this.ticksLeft--;
            this.owner.setPlayingDead(true);

            if (this.ticksLeft == 0) {
                AttributeInstance speedAttr = this.owner.getAttribute(Attributes.MOVEMENT_SPEED);
                if (speedAttr != null) {
                    speedAttr.removeModifier(SPEED_MODIFIER_ID);
                }
            }
        }
    }
}
