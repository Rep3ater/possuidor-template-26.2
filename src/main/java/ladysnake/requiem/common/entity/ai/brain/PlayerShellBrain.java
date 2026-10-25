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
package ladysnake.requiem.common.entity.ai.brain;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.mojang.datafixers.util.Pair;
import ladysnake.requiem.common.entity.PlayerShellEntity;
import ladysnake.requiem.common.entity.ai.brain.tasks.LivingApproachTargetTask;
import ladysnake.requiem.common.entity.ai.brain.tasks.LivingForgetAttackTargetTask;
import ladysnake.requiem.common.entity.ai.brain.tasks.LivingUpdateAttackTargetTask;
import ladysnake.requiem.common.entity.ai.brain.tasks.PlayerGoHomeTask;
import ladysnake.requiem.common.entity.ai.brain.tasks.PlayerLookAroundTask;
import ladysnake.requiem.common.entity.ai.brain.tasks.PlayerMeleeTask;
import ladysnake.requiem.common.entity.ai.brain.tasks.PlayerWanderAroundTask;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.behavior.SetEntityLookTargetSometimes;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.schedule.Activity;

import java.util.Optional;
import java.util.Set;

public final class PlayerShellBrain {

    public static Brain<PlayerShellEntity> create(Brain<PlayerShellEntity> brain) {
        addCoreTasks(brain);
        addIdleTasks(brain);
        addFightTasks(brain);
        brain.setCoreActivities(ImmutableSet.of(Activity.CORE));
        brain.setDefaultActivity(Activity.IDLE);
        brain.useDefaultActivity();
        return brain;
    }

    private static void addCoreTasks(Brain<PlayerShellEntity> brain) {
        brain.addActivity(Activity.CORE, ImmutableList.of(
            Pair.of(0, new PlayerLookAroundTask(45, 90)),
            Pair.of(1, new PlayerWanderAroundTask())
        ), Set.of(), Set.of());
    }

    private static void addIdleTasks(Brain<PlayerShellEntity> brain) {
        brain.addActivity(Activity.IDLE, ImmutableList.of(
            Pair.of(0, new PlayerGoHomeTask(MemoryModuleType.HOME, 1f, 10, 5000)),
            Pair.of(1, new LivingUpdateAttackTargetTask<>(PlayerShellBrain::canFight, PlayerShellBrain::getPriorityAttackTarget)),
            Pair.of(2, SetEntityLookTargetSometimes.create(8.0F, UniformInt.of(30, 60)))
        ), Set.of(), Set.of());
    }

    private static Optional<? extends LivingEntity> getPriorityAttackTarget(PlayerShellEntity shell) {
        return shell.getBrain().getMemory(MemoryModuleType.NEAREST_HOSTILE);
    }

    private static boolean canFight(PlayerShellEntity shell) {
        return shell.getHealth() > 4F;
    }

    private static void addFightTasks(Brain<PlayerShellEntity> brain) {
        brain.addActivity(Activity.FIGHT, ImmutableList.of(
            Pair.of(0, new LivingApproachTargetTask(1.0F)),
            Pair.of(1, new PlayerMeleeTask()),
            Pair.of(2, new LivingForgetAttackTargetTask<>(e -> e instanceof Monster monster && monster.getTarget() == null))
        ), Set.of(Pair.of(MemoryModuleType.ATTACK_TARGET, MemoryStatus.VALUE_PRESENT)), Set.of());
    }

    public static void refreshActivities(PlayerShellEntity shell) {
        Brain<PlayerShellEntity> brain = shell.getBrain();
        Activity before = brain.getActiveNonCoreActivity().orElse(null);
        brain.setActiveActivityToFirstValid(ImmutableList.of(Activity.FIGHT, Activity.AVOID, Activity.IDLE));
        Activity after = brain.getActiveNonCoreActivity().orElse(null);
        if (before != after) {
            getActivityTransitionSound(shell).ifPresent(s -> shell.playSound(s, 1.0F, 1.0F));
        }
    }

    public static Optional<SoundEvent> getActivityTransitionSound(PlayerShellEntity shell) {
        return shell.getBrain().getActiveNonCoreActivity().map(PlayerShellBrain::getActivityStartSound);
    }

    private static SoundEvent getActivityStartSound(Activity activity) {
        if (activity == Activity.FIGHT) {
            return SoundEvents.PIGLIN_ANGRY;
        } else {
            return SoundEvents.PIGLIN_AMBIENT;
        }
    }
}
