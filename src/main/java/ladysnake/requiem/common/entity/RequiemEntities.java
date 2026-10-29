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
 */
package ladysnake.requiem.common.entity;

import baritone.api.fakeplayer.FakePlayers;
import com.google.common.collect.ImmutableMap;
import ladysnake.requiem.Requiem;
import net.neoforged.neoforge.registries.RegisterEvent;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.entity.monster.piglin.PiglinBrute;
import net.minecraft.world.entity.player.Player;

public final class RequiemEntities {

    public static final EntityType<ObeliskSoulEntity> OBELISK_SOUL = EntityType.Builder.<ObeliskSoulEntity>of(ObeliskSoulEntity::new, MobCategory.MISC)
        .sized(0.25f, 0.25f)
        .clientTrackingRange(4)
        .updateInterval(10)
        .build(ResourceKey.create(Registries.ENTITY_TYPE, Requiem.id("obelisk_soul")));

    public static final EntityType<ReleasedSoulEntity> RELEASED_SOUL = EntityType.Builder.<ReleasedSoulEntity>of(ReleasedSoulEntity::new, MobCategory.MISC)
        .sized(0.25f, 0.25f)
        .clientTrackingRange(4)
        .updateInterval(10)
        .build(ResourceKey.create(Registries.ENTITY_TYPE, Requiem.id("released_soul")));

    public static final EntityType<CuredVillagerEntity> CURED_VILLAGER = EntityType.Builder.<CuredVillagerEntity>of(CuredVillagerEntity::new, MobCategory.MISC)
        .sized(0.6f, 1.95f)
        .clientTrackingRange(10)
        .updateInterval(3)
        .build(ResourceKey.create(Registries.ENTITY_TYPE, Requiem.id("cured_villager")));

    public static final EntityType<Piglin> CURED_PIGLIN = EntityType.Builder.<Piglin>of((entityType, level) -> {
            Piglin ret = new Piglin(entityType, level);
            ret.setImmuneToZombification(true);
            return ret;
        }, MobCategory.MONSTER)
        .sized(0.6f, 1.95f)
        .clientTrackingRange(8)
        .updateInterval(3)
        .build(ResourceKey.create(Registries.ENTITY_TYPE, Requiem.id("cured_piglin")));

    public static final EntityType<PiglinBrute> CURED_PIGLIN_BRUTE = EntityType.Builder.<PiglinBrute>of((entityType, level) -> {
            PiglinBrute ret = new PiglinBrute(entityType, level);
            ret.setImmuneToZombification(true);
            return ret;
        }, MobCategory.MONSTER)
        .sized(0.6f, 1.95f)
        .clientTrackingRange(8)
        .updateInterval(3)
        .build(ResourceKey.create(Registries.ENTITY_TYPE, Requiem.id("cured_piglin_brute")));

    public static final ImmutableMap<EntityType<? extends Mob>, EntityType<? extends Mob>> CURED_PIGLIN_VARIANTS =
        ImmutableMap.of(EntityTypes.PIGLIN, CURED_PIGLIN, EntityTypes.PIGLIN_BRUTE, CURED_PIGLIN_BRUTE);

    public static final EntityType<PlayerShellEntity> PLAYER_SHELL = EntityType.Builder.<PlayerShellEntity>of(FakePlayers.entityFactory(PlayerShellEntity::new), MobCategory.MISC)
        .sized(0.6f, 1.8f)
        .clientTrackingRange(8)
        .updateInterval(1)
        .noSummon()
        .build(ResourceKey.create(Registries.ENTITY_TYPE, Requiem.id("player_shell")));

    public static final EntityType<MorticianEntity> MORTICIAN = EntityType.Builder.<MorticianEntity>of(MorticianEntity::new, MobCategory.CREATURE)
        .sized(0.6f, 1.95f)
        .build(ResourceKey.create(Registries.ENTITY_TYPE, Requiem.id("mortician")));

    public static final EntityType<ladysnake.requiem.common.construct.entity.RoboticConstructEntity> ROBOTIC_CONSTRUCT = EntityType.Builder.<ladysnake.requiem.common.construct.entity.RoboticConstructEntity>of(ladysnake.requiem.common.construct.entity.RoboticConstructEntity::new, MobCategory.MISC)
        .sized(1.2f, 2.2f)
        .clientTrackingRange(10)
        .updateInterval(2)
        .build(ResourceKey.create(Registries.ENTITY_TYPE, Requiem.id("robotic_construct")));

    public static final EntityType<HomunculusEntity> HOMUNCULUS = EntityType.Builder.<HomunculusEntity>of(HomunculusEntity::new, MobCategory.CREATURE)
        .sized(0.6f, 1.8f).clientTrackingRange(8).updateInterval(3)
        .build(ResourceKey.create(Registries.ENTITY_TYPE, Requiem.id("homunculus")));

    public static final EntityType<ImmortalHomunculusEntity> IMMORTAL_HOMUNCULUS = EntityType.Builder.<ImmortalHomunculusEntity>of(ImmortalHomunculusEntity::new, MobCategory.MONSTER)
        .sized(0.7f, 2.05f).clientTrackingRange(8).updateInterval(2)
        .build(ResourceKey.create(Registries.ENTITY_TYPE, Requiem.id("immortal_homunculus")));

    public static void register(RegisterEvent.RegisterHelper<EntityType<?>> helper) {
        helper.register(Requiem.id("player_shell"), PLAYER_SHELL);
        helper.register(Requiem.id("obelisk_soul"), OBELISK_SOUL);
        helper.register(Requiem.id("released_soul"), RELEASED_SOUL);
        helper.register(Requiem.id("cured_villager"), CURED_VILLAGER);
        helper.register(Requiem.id("cured_piglin"), CURED_PIGLIN);
        helper.register(Requiem.id("cured_piglin_brute"), CURED_PIGLIN_BRUTE);
        helper.register(Requiem.id("mortician"), MORTICIAN);
        helper.register(Requiem.id("robotic_construct"), ROBOTIC_CONSTRUCT);
        helper.register(Requiem.id("homunculus"), HOMUNCULUS);
        helper.register(Requiem.id("immortal_homunculus"), IMMORTAL_HOMUNCULUS);
    }

    public static void postRegister() {
        MorticianSpawner.init();
    }

    public static void init() {
        // Registered via RegisterEvent
    }
}
