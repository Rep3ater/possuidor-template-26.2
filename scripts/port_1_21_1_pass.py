#!/usr/bin/env python3
"""Mechanical package/name pass for the Requiem Minecraft 26.2 -> 1.21.1 port.

This deliberately handles only high-confidence renames; semantic API rewrites remain
separate and must be driven by the compiler.
"""
from pathlib import Path
import re

ROOT = Path("src/main/java")
REPLACEMENTS = [
    # More specific packages first.
    ("net.minecraft.advancements.predicates.entity.EntityPredicate", "net.minecraft.advancements.critereon.EntityPredicate"),
    ("net.minecraft.advancements.predicates.ContextAwarePredicate", "net.minecraft.advancements.critereon.ContextAwarePredicate"),
    ("net.minecraft.advancements.predicates.DamagePredicate", "net.minecraft.advancements.critereon.DamagePredicate"),
    ("net.minecraft.advancements.predicates.ItemPredicate", "net.minecraft.advancements.critereon.ItemPredicate"),
    ("net.minecraft.advancements.predicates.", "net.minecraft.advancements.critereon."),
    ("net.minecraft.advancements.triggers.SimpleCriterionTrigger", "net.minecraft.advancements.critereon.SimpleCriterionTrigger"),
    ("net.minecraft.advancements.triggers.CriterionTrigger", "net.minecraft.advancements.CriterionTrigger"),
    ("net.minecraft.advancements.triggers.CriteriaTriggers", "net.minecraft.advancements.CriteriaTriggers"),
    ("net.minecraft.world.level.gamerules.GameRules", "net.minecraft.world.level.GameRules"),
    ("net.minecraft.util.context.ContextKeySet", "net.minecraft.world.level.storage.loot.parameters.LootContextParamSet"),
    ("net.minecraft.util.context.ContextKey", "net.minecraft.world.level.storage.loot.parameters.LootContextParam"),
    ("net.minecraft.util.context.", "net.minecraft.world.level.storage.loot.parameters."),
    ("net.minecraft.world.entity.monster.skeleton.", "net.minecraft.world.entity.monster."),
    ("net.minecraft.world.entity.monster.zombie.", "net.minecraft.world.entity.monster."),
    ("net.minecraft.world.entity.monster.illager.", "net.minecraft.world.entity.monster."),
    ("net.minecraft.world.entity.monster.spider.", "net.minecraft.world.entity.monster."),
    ("net.minecraft.world.entity.animal.fish.", "net.minecraft.world.entity.animal."),
    ("net.minecraft.world.entity.animal.sheep.", "net.minecraft.world.entity.animal."),
    ("net.minecraft.world.entity.animal.chicken.", "net.minecraft.world.entity.animal."),
    ("net.minecraft.world.entity.animal.equine.", "net.minecraft.world.entity.animal.horse."),
    ("net.minecraft.world.entity.npc.villager.", "net.minecraft.world.entity.npc."),
    ("net.minecraft.world.entity.projectile.throwableitemprojectile.", "net.minecraft.world.entity.projectile."),
    ("net.minecraft.world.entity.projectile.hurtingprojectile.", "net.minecraft.world.entity.projectile."),
    ("net.minecraft.world.entity.projectile.arrow.", "net.minecraft.world.entity.projectile."),
    ("net.minecraft.world.entity.EntitySpawnReason", "net.minecraft.world.entity.MobSpawnType"),
    ("net.minecraft.world.entity.EntityTypes", "net.minecraft.world.entity.EntityType"),
    ("net.minecraft.resources.Identifier", "net.minecraft.resources.ResourceLocation"),
    ("net.minecraft.world.item.ItemUseAnimation", "net.minecraft.world.item.UseAnim"),
    ("net.minecraft.world.entity.player.PlayerSkin", "net.minecraft.client.resources.PlayerSkin"),
    ("net.minecraft.util.TriState", "net.neoforged.neoforge.common.util.TriState"),
    ("net.neoforged.neoforge.event.level.block.BreakBlockEvent", "net.neoforged.neoforge.event.level.BlockEvent.BreakEvent"),
    ("net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent", "net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent"),
    ("net.neoforged.neoforge.client.network.RegisterClientPayloadHandlersEvent", "net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent"),
    ("net.neoforged.neoforge.common.extensions.IMenuProviderExtension", "net.minecraft.world.MenuProvider"),
    ("net.minecraft.world.level.redstone.Orientation", "net.minecraft.core.Direction"),
    ("net.minecraft.world.entity.npc.villager.", "net.minecraft.world.entity.npc."),
    ("net.minecraft.world.entity.monster.cubemob.", "net.minecraft.world.entity.monster."),
]
WORD_REPLACEMENTS = [
    (r"\bIdentifier\b", "ResourceLocation"),
    (r"\bEntityTypes\b", "EntityType"),
    (r"\bEntitySpawnReason\b", "MobSpawnType"),
    (r"\bItemUseAnimation\b", "UseAnim"),
    (r"\bContextKeySet\b", "LootContextParamSet"),
    (r"\bContextKey\b", "LootContextParam"),
    (r"\bBreakBlockEvent\b", "BlockEvent.BreakEvent"),
]
METHOD_REPLACEMENTS = [
    (r"\.writeIdentifier\(", ".writeResourceLocation("),
    (r"\.readIdentifier\(", ".readResourceLocation("),
    (r"\.identifier\(\)", ".location()"),
]

changed = []
for path in ROOT.rglob("*.java"):
    original = path.read_text(encoding="utf-8")
    text = original
    for old, new in REPLACEMENTS:
        text = text.replace(old, new)
    for pattern, replacement in WORD_REPLACEMENTS:
        text = re.sub(pattern, replacement, text)
    for pattern, replacement in METHOD_REPLACEMENTS:
        text = re.sub(pattern, replacement, text)
    # 1.21.1 reload listeners are registered directly; this shim no longer has the keyed overload.
    text = re.sub(r"event\.addListener\(keyOf\(listener\),\s*listener\);", "event.addListener(listener);", text)
    if text != original:
        path.write_text(text, encoding="utf-8", newline="")
        changed.append(str(path))
print(f"Mechanical 1.21.1 substitutions changed {len(changed)} Java files.")
for path in changed:
    print(path)
