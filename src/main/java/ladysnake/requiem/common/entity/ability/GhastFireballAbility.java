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

import ladysnake.requiem.core.entity.ability.IndirectAbilityBase;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Ghast;
import net.minecraft.world.entity.projectile.LargeFireball;
import net.minecraft.world.phys.Vec3;

public class GhastFireballAbility extends IndirectAbilityBase<LivingEntity> {
    public GhastFireballAbility(LivingEntity owner) {
        super(owner, 60);
    }

    @Override
    public void update() {
        super.update();
        if (this.cooldown == 20) {
            this.setShooting(false);
        }
    }

    private void setShooting(boolean shooting) {
        if (this.owner instanceof Ghast ghast) {
            ghast.setCharging(shooting);
        }
    }

    @Override
    public boolean run() {
        if (!this.owner.level().isClientSide()) {
            this.owner.level().levelEvent(null, 1016, this.owner.blockPosition(), 0);
            this.setShooting(true);
            this.spawnFireball();
            this.beginCooldown();
        }
        return true;
    }

    private void spawnFireball() {
        Vec3 scaledRot = this.owner.getViewVector(1.0F);
        Vec3 rot = this.owner.getViewVector(1.0f).scale(10);
        int explosionPower = this.owner instanceof Ghast ghast ? ghast.getExplosionPower() : 1;
        LargeFireball fireball = new LargeFireball(this.owner.level(), this.owner, rot.normalize(), explosionPower);
        fireball.setPos(
            this.owner.getX() + scaledRot.x * 4.0D,
            this.owner.getY() + (double) (this.owner.getBbHeight() / 2.0F) + 0.5D,
            this.owner.getZ() + scaledRot.z * 4.0D
        );
        this.owner.level().addFreshEntity(fireball);
    }
}
