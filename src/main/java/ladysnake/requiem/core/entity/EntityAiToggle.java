/*
 * Requiem
 * Copyright (C) 2017-2023 Ladysnake
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * License for more details.
 *
 * You should have received a copy of the GNU License
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
package ladysnake.requiem.core.entity;

import dev.onyxstudios.cca.api.v3.component.ComponentKey;
import dev.onyxstudios.cca.api.v3.component.ComponentRegistry;
import dev.onyxstudios.cca.api.v3.component.sync.AutoSyncedComponent;
import it.unimi.dsi.fastutil.objects.Object2BooleanMap;
import it.unimi.dsi.fastutil.objects.Object2BooleanOpenHashMap;
import ladysnake.requiem.core.RequiemCore;
import ladysnake.requiem.core.entity.ai.DisableableAiController;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class EntityAiToggle implements AutoSyncedComponent {
    public static final ComponentKey<EntityAiToggle> KEY = ComponentRegistry.getOrCreate(RequiemCore.id("ai_toggle"), EntityAiToggle.class);

    public static EntityAiToggle get(LivingEntity owner) {
        return KEY.get(owner);
    }

    private final LivingEntity owner;
    private final Object2BooleanMap<Identifier> aiInhibitors = new Object2BooleanOpenHashMap<>();
    private boolean disabled;

    public EntityAiToggle(LivingEntity owner) {
        this.owner = owner;
    }

    /**
     * Toggles an entity's AI.
     *
     * <p>If this method is called multiple times with the same inhibitor,
     * the new state will override the previous one.
     *
     * <p>The entity's AI will remain disabled as long as there is at least
     * one inhibitor affecting it.
     *
     * @param inhibitorId the unique identifier for the mechanic inhibiting this entity's AI
     * @param inhibit if {@code true}, the entity's AI will be disabled, otherwise the inhibitor will stop affecting the entity
     */
    public void toggleAi(Identifier inhibitorId, boolean inhibit) {
        this.toggleAi(inhibitorId, inhibit, false);
    }

    /**
     * Toggles an entity's AI.
     *
     * <p>If this method is called multiple times with the same inhibitor,
     * the new state will override the previous one.
     *
     * <p>The entity's AI will remain disabled as long as there is at least
     * one inhibitor affecting it.
     *
     * @param inhibitorId the unique identifier for the mechanic inhibiting this entity's AI
     * @param inhibit if {@code true}, the entity's AI will be disabled, otherwise the inhibitor will stop affecting the entity
     * @param persistent if {@code true}, the inhibition will be saved with the entity
     */
    public void toggleAi(Identifier inhibitorId, boolean inhibit, boolean persistent) {
        boolean wasDisabled = this.isAiDisabled();
        if (inhibit) {
            this.aiInhibitors.put(inhibitorId, persistent);
        } else {
            this.aiInhibitors.removeBoolean(inhibitorId);
        }
        boolean nowDisabled = !this.aiInhibitors.isEmpty();

        if (wasDisabled != nowDisabled) {
            this.refresh(nowDisabled);
        }
    }

    private void refresh(boolean nowDisabled) {
        this.disabled = nowDisabled;
        if (this.owner.getBrain() instanceof DisableableAiController brain) {
            brain.requiem$setDisabled(nowDisabled);
        }
        if (this.owner instanceof Mob mob) {
            if (mob.goalSelector instanceof DisableableAiController goalSel) {
                goalSel.requiem$setDisabled(nowDisabled);
            }
            if (mob.targetSelector instanceof DisableableAiController targetSel) {
                targetSel.requiem$setDisabled(nowDisabled);
            }
            if (mob.getNavigation() instanceof DisableableAiController nav) {
                nav.requiem$setDisabled(nowDisabled);
            }
        }
        KEY.sync(this.owner);
    }

    @Override
    public void writeSyncPacket(RegistryFriendlyByteBuf buf, ServerPlayer recipient) {
        buf.writeBoolean(this.disabled);
    }

    @Override
    public void applySyncPacket(RegistryFriendlyByteBuf buf) {
        this.disabled = buf.readBoolean();
    }

    public boolean isAiDisabled() {
        return this.disabled;
    }

    @Override
    public void readFromNbt(CompoundTag tag) {
        tag.getList("inhibitors")
            .ifPresent(list -> list.stream()
                .map(elem -> elem.asString().orElse(null))
                .filter(Objects::nonNull)
                .map(Identifier::tryParse)
                .filter(Objects::nonNull)
                .forEach(id -> this.aiInhibitors.put(id, true)));
        this.refresh(!this.aiInhibitors.isEmpty());
    }

    @Override
    public void writeToNbt(CompoundTag tag) {
        ListTag list = this.aiInhibitors.object2BooleanEntrySet().stream()
            .filter(Object2BooleanMap.Entry::getBooleanValue)
            .map(Map.Entry::getKey)
            .map(Identifier::toString)
            .map(StringTag::valueOf)
            .collect(Collectors.toCollection(ListTag::new));
        tag.put("inhibitors", list);
    }
}
