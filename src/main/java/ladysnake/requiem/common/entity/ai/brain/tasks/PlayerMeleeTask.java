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
package ladysnake.requiem.common.entity.ai.brain.tasks;

import baritone.api.fakeplayer.FakeServerPlayerEntity;
import com.google.common.collect.ImmutableMap;
import ladysnake.requiem.core.util.RayHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.behavior.BehaviorUtils;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ProjectileWeaponItem;

public class PlayerMeleeTask extends Behavior<FakeServerPlayerEntity> {
    public PlayerMeleeTask() {
        super(ImmutableMap.of(
            MemoryModuleType.LOOK_TARGET, MemoryStatus.REGISTERED,
            MemoryModuleType.ATTACK_TARGET, MemoryStatus.VALUE_PRESENT
        ));
    }

    public static double getAttackRange(LivingEntity attacker) {
        return attacker.getAttributeValue(Attributes.ENTITY_INTERACTION_RANGE);
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel world, FakeServerPlayerEntity executor) {
        LivingEntity target = this.getAttackTarget(executor);
        return !this.isHoldingRangedWeapon(executor)
            && BehaviorUtils.canSee(executor, target)
            && this.isLookingAt(executor, target)
            && this.isAttackReady(executor, target);
    }

    private boolean isLookingAt(FakeServerPlayerEntity executor, LivingEntity target) {
        return RayHelper.getTargetedEntity(executor) == target;
    }

    private boolean isAttackReady(Player executor, LivingEntity target) {
        return target.invulnerableTime <= 10 && (executor.getAttackStrengthScale(0.5F) >= 1 || estimateDamage(executor, target) > target.getHealth());
    }

    @Override
    protected void start(ServerLevel serverWorld, FakeServerPlayerEntity mobEntity, long l) {
        LivingEntity livingEntity = this.getAttackTarget(mobEntity);
        BehaviorUtils.lookAtEntity(mobEntity, livingEntity);
        mobEntity.attack(livingEntity);
        mobEntity.swing(InteractionHand.MAIN_HAND);
    }

    private boolean isHoldingRangedWeapon(Player player) {
        return player.isHolding((item) -> item.getItem() instanceof ProjectileWeaponItem);
    }

    private LivingEntity getAttackTarget(LivingEntity executor) {
        return executor.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET).orElseThrow(IllegalStateException::new);
    }

    public static float estimateDamage(Player player, Entity target) {
        float baseDamage = (float) player.getAttributeValue(Attributes.ATTACK_DAMAGE);
        float i = player.getAttackStrengthScale(0.5F);
        baseDamage *= 0.2F + i * i * 0.8F;
        return baseDamage;
    }
}
