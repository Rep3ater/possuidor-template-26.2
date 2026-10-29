/*
 * Requiem
 * Copyright (C) 2017-2023 Ladysnake
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * License for more details.
 *
 * You should have received a copy of the GNU License
 * along with this program. If not, see <https://www.gnu.org/licenses>.
 *
 * Linking this mod statically or dynamically with other
 * modules is making a combined work based on this mod.
 * Thus, the terms and conditions of the GNU General Public License cover the whole combination.
 *
 * In addition, as a special exception, the copyright holders of
 * this mod give you permission to combine this mod
 * with free software programs or libraries that are released under the GNU LGPL
 * and with code included in the standard release of Minecraft under All Rights Reserved (or
 * modified versions of such code, with unchanged license).
 * You may copy and distribute such a system following the terms of the GNU GPL for this mod
 * and the licenses of the other code concerned.
 *
 * Note that people who make modified versions of this mod are not obligated to grant
 * this special exception for their modified versions; it is their choice whether to do so.
 * The GNU General Public License gives permission to release a modified version without this exception;
 * this exception also makes it possible to release a modified version which carries forward this exception.
 */
package ladysnake.requiem.core.entity.ability;

import ladysnake.requiem.api.v1.possession.Possessable;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;

public class MeleeAbility extends DirectAbilityBase<Mob, Entity> {
    private final boolean ignoreDamageAttribute;

    public MeleeAbility(Mob owner) {
        this(owner, false);
    }

    public MeleeAbility(Mob owner, boolean ignoreDamageAttribute) {
        super(owner, 0, 0, Entity.class);
        this.ignoreDamageAttribute = ignoreDamageAttribute;
    }

    @Override
    public boolean run(Entity target) {
        // We actually need to check if the entity has an attack damage attribute, because mojang doesn't.
        if (!ignoreDamageAttribute && owner.getAttribute(Attributes.ATTACK_DAMAGE) == null) {
            return false;
        }

        if (this.owner.level().isClientSide()) return true;

        boolean success = owner.doHurtTarget(target);

        if (success && target instanceof LivingEntity livingTarget) {
            this.owner.setAggressive(true);
            Player player = ((Possessable) this.owner).getPossessor();

            if (player != null) {
                player.getMainHandItem().hurtEnemy(livingTarget, player);
                // Reset cooldown
                player.resetAttackStrengthTicker();
            }
        }
        return success;
    }
}
