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

import ladysnake.requiem.api.v1.entity.ability.DirectAbility;
import ladysnake.requiem.api.v1.entity.ability.IndirectAbility;
import ladysnake.requiem.core.entity.ability.AbilityBase;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.projectile.WitherSkull;
import net.minecraft.world.phys.Vec3;

import java.util.Random;

public class WitherSkullAbility extends AbilityBase<WitherBoss> {
    protected static final Random RANDOM = new Random();

    public WitherSkullAbility(WitherBoss owner, int cooldownTime) {
        super(owner, cooldownTime);
    }

    protected double getHeadX(int headIndex) {
        if (headIndex <= 0) {
            return owner.getX();
        } else {
            float f = (owner.yBodyRot + (float) (180 * (headIndex - 1))) * 0.017453292F;
            float g = Mth.cos(f);
            return owner.getX() + (double) g * 1.3D;
        }
    }

    protected double getHeadY(int headIndex) {
        return headIndex <= 0 ? owner.getY() + 3.0D : owner.getY() + 2.2D;
    }

    protected double getHeadZ(int headIndex) {
        if (headIndex <= 0) {
            return owner.getZ();
        } else {
            float f = (owner.yBodyRot + (float) (180 * (headIndex - 1))) * 0.017453292F;
            float g = Mth.sin(f);
            return owner.getZ() + (double) g * 1.3D;
        }
    }

    protected WitherSkull summonSkullWithTarget(double dirX, double dirY, double dirZ) {
        int headIndex = RANDOM.nextInt(3);
        double x = getHeadX(headIndex);
        double y = getHeadY(headIndex);
        double z = getHeadZ(headIndex);
        return summonSkullWithTarget(x, y, z, dirX, dirY, dirZ);
    }

    protected WitherSkull summonSkullWithTarget(double x, double y, double z, double dirX, double dirY, double dirZ) {
        if (!owner.isSilent()) {
            owner.level().levelEvent(null, 1024, owner.blockPosition(), 0);
        }
        WitherSkull witherSkull = new WitherSkull(
            this.owner.level(),
            this.owner,
            new Vec3(dirX, dirY, dirZ)
        );
        witherSkull.setOwner(owner);
        witherSkull.setPos(x, y, z);
        owner.level().addFreshEntity(witherSkull);
        return witherSkull;
    }

    public static class BlueWitherSkullAbility extends WitherSkullAbility implements IndirectAbility<WitherBoss> {
        public BlueWitherSkullAbility(WitherBoss owner, int cooldown) {
            super(owner, cooldown);
        }

        public BlueWitherSkullAbility(WitherBoss owner) {
            this(owner, 40);
        }

        @Override
        public boolean trigger() {
            Vec3 rot = this.owner.getViewVector(1.0f).scale(10);
            WitherSkull skull = this.summonSkullWithTarget(rot.x + this.owner.getRandom().nextGaussian(), rot.y, rot.z + this.owner.getRandom().nextGaussian());
            skull.setDangerous(true);
            return true;
        }
    }

    public static class BlackWitherSkullAbility extends WitherSkullAbility implements DirectAbility<WitherBoss, LivingEntity> {
        public BlackWitherSkullAbility(WitherBoss owner, int cooldown) {
            super(owner, cooldown);
        }

        public BlackWitherSkullAbility(WitherBoss owner) {
            this(owner, 40);
        }

        @Override
        public double getRange() {
            return 20;
        }

        @Override
        public Class<LivingEntity> getTargetType() {
            return LivingEntity.class;
        }

        @Override
        public boolean canTarget(LivingEntity target) {
            return true;
        }

        @Override
        public InteractionResult trigger(LivingEntity target) {
            int headIndex = RANDOM.nextInt(3);
            double g = getHeadX(headIndex);
            double h = getHeadY(headIndex);
            double i = getHeadZ(headIndex);
            double dirX = target.getX() - g;
            double dirY = target.getY() + (double) target.getEyeHeight() * 0.5D - h;
            double dirZ = target.getZ() - i;
            this.summonSkullWithTarget(g, h, i, dirX, dirY, dirZ);
            return InteractionResult.SUCCESS;
        }
    }
}
