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
 * You should have received a copy of the GNU General Public License
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

import ladysnake.requiem.api.v1.entity.ability.AbilityType;
import ladysnake.requiem.api.v1.entity.ability.MobAbilityController;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.phys.AABB;

import java.util.List;

public class AutoAimAbility<E extends LivingEntity> extends IndirectAbilityBase<E> {
    private final double searchRangeX;
    private final double searchRangeY;
    private final AbilityType type;

    public AutoAimAbility(E owner, AbilityType type, double horizontalSearchRange, double verticalSearchRange) {
        super(owner, 0);
        this.searchRangeX = horizontalSearchRange;
        this.searchRangeY = verticalSearchRange;
        this.type = type;
    }

    @Override
    public boolean run() {
        LivingEntity target = null;
        if (this.owner.level() instanceof ServerLevel serverLevel) {
            target = serverLevel.getNearestEntity(
                LivingEntity.class,
                TargetingConditions.forCombat(),
                this.owner,
                this.owner.getX(),
                this.owner.getY() + (double) this.owner.getEyeHeight(net.minecraft.world.entity.Pose.STANDING),
                this.owner.getZ(),
                this.getSearchBox()
            );
        } else {
            List<LivingEntity> list = this.owner.level().getEntitiesOfClass(LivingEntity.class, this.getSearchBox(), e -> e != this.owner && e.isAlive());
            if (!list.isEmpty()) {
                target = list.get(0);
            }
        }
        if (target != null) {
            return MobAbilityController.get(this.owner).useDirect(type, target).consumesAction();
        }
        return false;
    }

    private AABB getSearchBox() {
        double range = this.searchRangeX;
        return this.owner.getBoundingBox().inflate(range, searchRangeY, range);
    }
}
