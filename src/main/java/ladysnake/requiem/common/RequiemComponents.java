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
package ladysnake.requiem.common;

import dev.onyxstudios.cca.api.v3.entity.EntityComponentFactoryRegistry;
import dev.onyxstudios.cca.api.v3.entity.EntityComponentInitializer;
import dev.onyxstudios.cca.api.v3.entity.RespawnCopyStrategy;
import dev.onyxstudios.cca.api.v3.scoreboard.ScoreboardComponentFactoryRegistry;
import dev.onyxstudios.cca.api.v3.scoreboard.ScoreboardComponentInitializer;
import ladysnake.requiem.api.v1.entity.CurableEntityComponent;
import ladysnake.requiem.api.v1.entity.ExternalJumpingMount;
import ladysnake.requiem.api.v1.entity.MovementAlterer;
import ladysnake.requiem.api.v1.entity.ability.MobAbilityController;
import ladysnake.requiem.api.v1.internal.StatusEffectReapplicator;
import ladysnake.requiem.api.v1.possession.PossessedData;
import ladysnake.requiem.api.v1.possession.PossessionComponent;
import ladysnake.requiem.api.v1.record.GlobalRecordKeeper;
import ladysnake.requiem.api.v1.remnant.AttritionFocus;
import ladysnake.requiem.api.v1.remnant.DeathSuspender;
import ladysnake.requiem.api.v1.remnant.RemnantComponent;
import ladysnake.requiem.api.v1.remnant.RiftTracker;
import ladysnake.requiem.common.entity.CoolPlayerMovementAlterer;
import ladysnake.requiem.common.entity.SkeletonBoneComponent;
import ladysnake.requiem.common.entity.WololoComponent;
import ladysnake.requiem.common.entity.cure.CurableZombifiedPiglinComponent;
import ladysnake.requiem.common.entity.cure.DelegatingCurableEntityComponent;
import ladysnake.requiem.common.entity.cure.SimpleCurableEntityComponent;
import ladysnake.requiem.common.entity.effect.PenanceComponent;
import ladysnake.requiem.common.entity.effect.StatusEffectReapplicatorImpl;
import ladysnake.requiem.common.gamerule.RequiemSyncedGamerules;
import ladysnake.requiem.common.possession.LootingPossessedData;
import ladysnake.requiem.common.possession.jump.DummyHorseJumpingMount;
import ladysnake.requiem.common.remnant.DroppedVesselTracker;
import ladysnake.requiem.common.remnant.GlobalAttritionFocus;
import ladysnake.requiem.common.remnant.PlayerBodyTracker;
import ladysnake.requiem.common.remnant.PlayerRiftTracker;
import ladysnake.requiem.common.remnant.RemnantComponentImpl;
import ladysnake.requiem.common.remnant.SimpleAttritionFocus;
import ladysnake.requiem.core.ability.ImmutableMobAbilityController;
import ladysnake.requiem.core.ability.PlayerAbilityController;
import ladysnake.requiem.core.entity.EntityAiToggle;
import ladysnake.requiem.core.entity.SoulHolderComponent;
import ladysnake.requiem.core.possession.PossessionComponentImpl;
import ladysnake.requiem.core.record.CommonRecordKeeper;
import ladysnake.requiem.core.record.EntityPositionClerk;
import ladysnake.requiem.core.record.ServerRecordKeeper;
import ladysnake.requiem.core.remnant.RevivingDeathSuspender;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.ZombieVillager;
import net.minecraft.world.entity.monster.ZombifiedPiglin;
import net.minecraft.world.entity.animal.frog.Frog;
import net.minecraft.world.entity.animal.goat.Goat;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.sounds.SoundEvents;

public final class RequiemComponents implements EntityComponentInitializer, ScoreboardComponentInitializer {

    @Override
    public void registerEntityComponentFactories(EntityComponentFactoryRegistry registry) {
        // the order here is important, possession must be synced/deserialized after remnant
        registry.registerForPlayers(RemnantComponent.KEY, RemnantComponentImpl::new, RespawnCopyStrategy.ALWAYS_COPY);
        registry.registerForPlayers(PossessionComponent.KEY, PossessionComponentImpl::new, RespawnCopyStrategy.INVENTORY);
        // order does not matter for the other components
        registry.registerForPlayers(MovementAlterer.KEY, CoolPlayerMovementAlterer::new, RespawnCopyStrategy.LOSSLESS_ONLY);
        registry.registerForPlayers(DeathSuspender.KEY, RevivingDeathSuspender::new, RespawnCopyStrategy.LOSSLESS_ONLY);
        registry.registerFor(EnderMan.class, WololoComponent.KEY, WololoComponent::create);
        registry.registerFor(Mob.class, PossessedData.KEY, LootingPossessedData::new);
        registry.registerFor(Mob.class, MobAbilityController.KEY, ImmutableMobAbilityController::new);
        registry.registerForPlayers(MobAbilityController.KEY, player -> new PlayerAbilityController(player, VanillaRequiemPlugin.SOUL_ABILITY_CONFIG), RespawnCopyStrategy.LOSSLESS_ONLY);
        registry.registerFor(Mob.class, SkeletonBoneComponent.KEY, SkeletonBoneComponent::new);
        registry.registerFor(Mob.class, AttritionFocus.KEY, p -> new SimpleAttritionFocus());
        registry.registerForPlayers(StatusEffectReapplicator.KEY, StatusEffectReapplicatorImpl::new, RespawnCopyStrategy.LOSSLESS_ONLY);
        registry.registerFor(Mob.class, CurableEntityComponent.KEY, SimpleCurableEntityComponent::new);
        registry.registerFor(ZombieVillager.class, CurableEntityComponent.KEY, DelegatingCurableEntityComponent::new);
        registry.registerFor(ZombifiedPiglin.class, CurableEntityComponent.KEY, CurableZombifiedPiglinComponent::new);
        registry.registerFor(LivingEntity.class, EntityAiToggle.KEY, EntityAiToggle::new);
        registry.beginRegistration(LivingEntity.class, EntityPositionClerk.KEY).respawnStrategy(RespawnCopyStrategy.LOSSLESS_ONLY).end(entity -> new EntityPositionClerk(RequiemRecordTypes.ENTITY_REF, entity));
        registry.registerForPlayers(PlayerBodyTracker.KEY, PlayerBodyTracker::new, RespawnCopyStrategy.ALWAYS_COPY);
        registry.registerForPlayers(PenanceComponent.KEY, PenanceComponent::new, RespawnCopyStrategy.LOSSLESS_ONLY);
        registry.registerFor(LivingEntity.class, SoulHolderComponent.KEY, SoulHolderComponent::new);
        registry.registerFor(Frog.class, ExternalJumpingMount.KEY, ExternalJumpingMount.simple(0.6F, SoundEvents.FROG_STEP));
        registry.registerFor(Goat.class, ExternalJumpingMount.KEY, ExternalJumpingMount.simple(1F, SoundEvents.GOAT_STEP));
        registry.registerFor(AbstractHorse.class, ExternalJumpingMount.KEY, e -> new DummyHorseJumpingMount(e, SoundEvents.HORSE_STEP));
        registry.registerForPlayers(RiftTracker.KEY, PlayerRiftTracker::new, RespawnCopyStrategy.ALWAYS_COPY);
        registry.registerForPlayers(DroppedVesselTracker.KEY, DroppedVesselTracker::new, RespawnCopyStrategy.LOSSLESS_ONLY);
    }

    @Override
    public void registerScoreboardComponentFactories(ScoreboardComponentFactoryRegistry registry) {
        registry.registerScoreboardComponent(AttritionFocus.KEY, GlobalAttritionFocus.class, (sc, server) -> new GlobalAttritionFocus(server));
        registry.registerScoreboardComponent(RequiemSyncedGamerules.KEY, (scoreboard, server) -> new RequiemSyncedGamerules(server));
        registry.registerScoreboardComponent(GlobalRecordKeeper.KEY, (scoreboard, server) -> server == null
            ? new CommonRecordKeeper()
            : new ServerRecordKeeper(server)
        );
    }
}
