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

import ladysnake.requiem.core.entity.ability.DirectAbilityBase;
import ladysnake.requiem.mixin.common.access.ShulkerEntityAccessor;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Shulker;
import net.minecraft.world.entity.projectile.ShulkerBullet;

public class ShulkerShootAbility extends DirectAbilityBase<Shulker, LivingEntity> {
    public static final int BULLET_COOLDOWN = 20;

    public ShulkerShootAbility(Shulker owner) {
        super(owner, BULLET_COOLDOWN, 16, LivingEntity.class);
    }

    @Override
    public boolean canTarget(LivingEntity target) {
        return super.canTarget(target) && ((ShulkerEntityAccessor) this.owner).requiem$getPeekAmount() > 50;
    }

    @Override
    public boolean run(LivingEntity target) {
        if (!this.owner.level().isClientSide()) {
            this.owner.level().addFreshEntity(new ShulkerBullet(this.owner.level(), this.owner, target, this.owner.getAttachFace().getAxis()));
            this.owner.playSound(SoundEvents.SHULKER_SHOOT, 2.0F, (this.owner.getRandom().nextFloat() - this.owner.getRandom().nextFloat()) * 0.2F + 1.0F);
            this.beginCooldown();
        }

        return true;
    }
}
