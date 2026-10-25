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
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.frog.Frog;

import java.util.Objects;
import java.util.Optional;

public class FrogCatchAbility extends DirectAbilityBase<Frog, Entity> {
    private int eatAnimationTicks = -1;

    public FrogCatchAbility(Frog owner) {
        super(owner, 20, 1.75, Entity.class);
    }

    @Override
    protected boolean run(Entity target) {
        Frog frog = this.owner;

        if (frog.level().isClientSide()) return true;
        // Taken from CatchAndEatTask#update
        frog.setTongueTarget(target);
        frog.level().playSound(null, frog.getX(), frog.getY(), frog.getZ(), SoundEvents.FROG_TONGUE, SoundSource.NEUTRAL, 2.0F, 1.0F);
        frog.setPose(Pose.USING_TONGUE);
        target.setDeltaMovement(frog.position().subtract(target.position()).normalize().scale(0.75));
        this.eatAnimationTicks = 0;
        return true;
    }

    @Override
    public void update() {
        super.update();

        if (!this.owner.level().isClientSide()) {
            Optional<Entity> target = this.owner.getTongueTarget();

            if (target.isPresent()) {
                if (this.eatAnimationTicks >= 10) {
                    this.owner.eraseTongueTarget();
                    this.owner.setPose(Pose.STANDING);
                    this.eatAnimationTicks = -1;
                } else if (this.eatAnimationTicks == 6) {
                    this.eatTargetEntity(this.owner, target.get());
                }
                this.eatAnimationTicks++;
            }
        }
    }

    private void eatTargetEntity(Frog frog, Entity entity) {
        frog.level().playSound(null, frog.getX(), frog.getY(), frog.getZ(), SoundEvents.FROG_EAT, SoundSource.NEUTRAL, 2.0F, 1.0F);

        if (entity.isAlive() && frog.level() instanceof ServerLevel serverLevel) {
            AttributeInstance attackDamage = Objects.requireNonNull(frog.getAttribute(Attributes.ATTACK_DAMAGE));
            double baseAttackValue = attackDamage.getBaseValue();

            if (!(entity instanceof LivingEntity living && Frog.canEat(living))) {
                attackDamage.setBaseValue(1);
            }

            frog.doHurtTarget(serverLevel, entity);

            attackDamage.setBaseValue(baseAttackValue);

            if (!entity.isAlive()) {
                entity.remove(Entity.RemovalReason.KILLED);
            }
        }
    }
}
