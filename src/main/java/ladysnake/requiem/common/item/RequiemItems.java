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
package ladysnake.requiem.common.item;

import ladysnake.requiem.Requiem;
import ladysnake.requiem.common.entity.RequiemEntities;
import ladysnake.requiem.common.entity.effect.RequiemStatusEffects;
import ladysnake.requiem.common.item.dispensing.RequiemDispenserBehaviors;
import ladysnake.requiem.common.remnant.RemnantTypes;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class RequiemItems {
    private static ResourceKey<Item> key(String name) {
        return ResourceKey.create(Registries.ITEM, Requiem.id(name));
    }

    public static final DebugItem DEBUG_ITEM = new DebugItem(new Item.Properties().setId(key("debug_item")));
    public static final Item TOTEM_OF_SKELETONIZATION = new Item(new Item.Properties().setId(key("totem_of_skeletonization")).stacksTo(1).rarity(Rarity.UNCOMMON));
    public static final EmptySoulVesselItem EMPTY_SOUL_VESSEL = new EmptySoulVesselItem(new Item.Properties().setId(key("empty_soul_vessel")).stacksTo(1));
    public static final CreativeSoulVesselItem CREATIVE_SOUL_VESSEL = new CreativeSoulVesselItem(new Item.Properties().setId(key("creative_soul_vessel")).stacksTo(1));
    public static final FilledSoulVesselItem FILLED_SOUL_VESSEL = new FilledSoulVesselItem(new Item.Properties().setId(key("filled_soul_vessel")).stacksTo(1), EMPTY_SOUL_VESSEL);
    public static final Item SHATTERED_SOUL_VESSEL = new Item(new Item.Properties().setId(key("shattered_soul_vessel")).stacksTo(1));
    public static final DemonSoulVesselItem PURE_SOUL_VESSEL = new DemonSoulVesselItem(RemnantTypes.MORTAL, ChatFormatting.AQUA, new Item.Properties().setId(key("pure_soul_vessel")).stacksTo(1), "requiem:remnant_vessel.cure");
    public static final DemonSoulVesselItem SEALED_REMNANT_VESSEL = new DemonSoulVesselItem(RemnantTypes.REMNANT, ChatFormatting.RED, new Item.Properties().setId(key("sealed_remnant_vessel")).stacksTo(1), "requiem:remnant_vessel.curse");
    public static final Item MORTICIAN_SPAWN_EGG = new SpawnEggItem(new Item.Properties().setId(key("mortician_spawn_egg")).spawnEgg(RequiemEntities.MORTICIAN));

    public static final Map<MobEffect, IchorVesselItem> vesselsByEffect = new LinkedHashMap<>();

    public static IchorVesselItem getVesselByEffect(Holder<MobEffect> effect) {
        if (effect == null) return null;
        return vesselsByEffect.get(effect.value());
    }
    public static final IchorVesselItem ICHOR_VESSEL_ATTRITION = makeIchorVessel("ichor_vessel_attrition", RequiemStatusEffects.ATTRITION);
    public static final IchorVesselItem ICHOR_VESSEL_EMANCIPATION = makeIchorVessel("ichor_vessel_emancipation", RequiemStatusEffects.EMANCIPATION);
    public static final IchorVesselItem ICHOR_VESSEL_PENANCE = makeIchorVessel("ichor_vessel_penance", RequiemStatusEffects.PENANCE);
    public static final IchorVesselItem ICHOR_VESSEL_RECLAMATION = makeIchorVessel("ichor_vessel_reclamation", RequiemStatusEffects.RECLAMATION);
    public static final int ICHOR_DEFAULT_DURATION = 20 * 60 * 10;

    private static final Map<ResourceKey<CreativeModeTab>, List<ItemLike>> itemsByGroup = new LinkedHashMap<>();

    private static IchorVesselItem makeIchorVessel(String name, Holder<MobEffect> statusEffect) {
        return new IchorVesselItem(new Item.Properties().setId(key(name)).stacksTo(1), new MobEffectInstance(statusEffect, ICHOR_DEFAULT_DURATION, 0, false, false, true));
    }

    public static void register(RegisterEvent.RegisterHelper<Item> helper) {
        registerItem(helper, DEBUG_ITEM, "debug_item", null);
        registerItem(helper, TOTEM_OF_SKELETONIZATION, "totem_of_skeletonization", CreativeModeTabs.COMBAT);
        registerItem(helper, PURE_SOUL_VESSEL, "pure_soul_vessel", CreativeModeTabs.TOOLS_AND_UTILITIES);
        registerItem(helper, EMPTY_SOUL_VESSEL, "empty_soul_vessel", CreativeModeTabs.TOOLS_AND_UTILITIES);
        registerItem(helper, CREATIVE_SOUL_VESSEL, "creative_soul_vessel", CreativeModeTabs.TOOLS_AND_UTILITIES);
        registerItem(helper, FILLED_SOUL_VESSEL, "filled_soul_vessel", CreativeModeTabs.TOOLS_AND_UTILITIES);
        registerItem(helper, SHATTERED_SOUL_VESSEL, "shattered_soul_vessel", CreativeModeTabs.TOOLS_AND_UTILITIES);
        registerItem(helper, SEALED_REMNANT_VESSEL, "sealed_remnant_vessel", CreativeModeTabs.TOOLS_AND_UTILITIES);
        registerItem(helper, ICHOR_VESSEL_ATTRITION, "ichor_vessel_attrition", CreativeModeTabs.TOOLS_AND_UTILITIES);
        registerItem(helper, ICHOR_VESSEL_EMANCIPATION, "ichor_vessel_emancipation", CreativeModeTabs.TOOLS_AND_UTILITIES);
        registerItem(helper, ICHOR_VESSEL_PENANCE, "ichor_vessel_penance", CreativeModeTabs.TOOLS_AND_UTILITIES);
        registerItem(helper, ICHOR_VESSEL_RECLAMATION, "ichor_vessel_reclamation", CreativeModeTabs.TOOLS_AND_UTILITIES);
        registerItem(helper, MORTICIAN_SPAWN_EGG, "mortician_spawn_egg", CreativeModeTabs.SPAWN_EGGS);
    }

    public static void postRegister() {
        vesselsByEffect.put(RequiemStatusEffects.ATTRITION.value(), ICHOR_VESSEL_ATTRITION);
        vesselsByEffect.put(RequiemStatusEffects.EMANCIPATION.value(), ICHOR_VESSEL_EMANCIPATION);
        vesselsByEffect.put(RequiemStatusEffects.PENANCE.value(), ICHOR_VESSEL_PENANCE);
        vesselsByEffect.put(RequiemStatusEffects.RECLAMATION.value(), ICHOR_VESSEL_RECLAMATION);
        RequiemDispenserBehaviors.registerDispenserBehaviors();
    }

    public static void init() {
        // Registered via RegisterEvent
    }

    public static void onBuildCreativeTab(BuildCreativeModeTabContentsEvent event) {
        List<ItemLike> items = itemsByGroup.get(event.getTabKey());
        if (items != null) {
            for (ItemLike item : items) {
                event.accept(item);
            }
        }
    }

    public static void addToGroup(ResourceKey<CreativeModeTab> group, ItemLike item) {
        if (group != null) {
            itemsByGroup.computeIfAbsent(group, g -> new ArrayList<>()).add(item);
        }
    }

    private static <T extends Item> T registerItem(RegisterEvent.RegisterHelper<Item> helper, T item, String name, ResourceKey<CreativeModeTab> group) {
        helper.register(Requiem.id(name), item);
        addToGroup(group, item);
        return item;
    }
}
