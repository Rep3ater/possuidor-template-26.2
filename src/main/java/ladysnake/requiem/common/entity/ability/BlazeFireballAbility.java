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
import ladysnake.requiem.mixin.common.possession.gameplay.ability.BlazeEntityAccessor;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Blaze;
import net.minecraft.world.entity.projectile.hurtingprojectile.SmallFireball;
import net.minecraft.world.phys.Vec3;

public class BlazeFireballAbility extends IndirectAbilityBase<LivingEntity> {
    public static final double RANDOM_SKEW_FACTOR = Mth.sqrt(Mth.sqrt(25.0f)) * 0.25F;
    public static final int CONSECUTIVE_FIREBALLS = 3;
    public static final int FIRE_TICKS = 200;
    public static final int BLAZE_SHOOT_EVENT = 1018;

    private int fireballs = CONSECUTIVE_FIREBALLS;

    public BlazeFireballAbility(LivingEntity owner) {
        super(owner, FIRE_TICKS);
    }

    @Override
    public void onCooldownEnd() {
        if (!this.owner.level().isClientSide()) {
            if (this.owner instanceof Blaze && ((BlazeEntityAccessor) this.owner).requiem$invokeIsFireActive()) {
                ((BlazeEntityAccessor) this.owner).requiem$invokeSetFireActive(false);
            }
            this.fireballs = CONSECUTIVE_FIREBALLS;
        }
    }

    @Override
    public boolean run() {
        if (!this.owner.level().isClientSide() && this.fireballs > 0) {
            this.playFireballEffects();
            this.spawnFireball();
            this.consumeFireball();
        }
        return true;
    }

    private void spawnFireball() {
        Vec3 rot = this.owner.getViewVector(1.0f).scale(10);
        Vec3 dir = new Vec3(
            rot.x + this.owner.getRandom().nextGaussian() * RANDOM_SKEW_FACTOR,
            rot.y,
            rot.z + this.owner.getRandom().nextGaussian() * RANDOM_SKEW_FACTOR
        );
        SmallFireball fireball = new SmallFireball(
            this.owner.level(),
            this.owner,
            dir
        );
        fireball.setPos(this.owner.getX(), this.owner.getY() + (double)(this.owner.getBbHeight() / 2.0F) + 0.5D, this.owner.getZ());
        this.owner.level().addFreshEntity(fireball);
    }

    private void playFireballEffects() {
        this.owner.level().levelEvent(BLAZE_SHOOT_EVENT, this.owner.blockPosition(), 0);
        if (this.owner instanceof Blaze) {
            ((BlazeEntityAccessor) this.owner).requiem$invokeSetFireActive(true);
        }
    }

    private void consumeFireball() {
        this.fireballs--;

        if (this.fireballs == 0) {
            this.beginCooldown();
        }
    }
}
