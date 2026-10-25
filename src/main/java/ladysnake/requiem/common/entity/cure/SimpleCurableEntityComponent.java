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
package ladysnake.requiem.common.entity.cure;

import ladysnake.requiem.api.v1.entity.CurableEntityComponent;
import ladysnake.requiem.core.tag.RequiemCoreTags;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.EntityTypeTags;
import org.jetbrains.annotations.Nullable;

public class SimpleCurableEntityComponent implements CurableEntityComponent {

    protected final Mob entity;
    protected boolean cured;

    public SimpleCurableEntityComponent(Mob entity) {
        this.entity = entity;
    }

    @Override
    public boolean hasBeenCured() {
        return this.cured;
    }

    @Override
    public void setCured() {
        this.cured = true;
    }

    @Override
    public void readFromNbt(CompoundTag tag) {
        if (tag.contains("cured")) this.cured = tag.getBooleanOr("cured", false);
    }

    @Override
    public void writeToNbt(CompoundTag tag) {
        if (this.cured) {
            tag.putBoolean("cured", true);
        }
    }

    @Override
    public boolean canBeAssimilated() {
        return this.entity.getType().builtInRegistryHolder().is(EntityTypeTags.UNDEAD) && this.entity.getType().builtInRegistryHolder().is(RequiemCoreTags.Entity.ITEM_USERS);
    }

    @Override
    public boolean canBeCured() {
        return false;
    }

    // Taken from ZombieVillagerEntity#finishConversion
    @Override
    public Mob cure() {
        Mob cured = this.createCuredEntity();
        if (cured != null) {
            for (EquipmentSlot equipmentSlot : EquipmentSlot.values()) {
                ItemStack itemStack = this.entity.getItemBySlot(equipmentSlot);
                if (!itemStack.isEmpty()) {
                    if (EnchantmentHelper.has(itemStack, net.minecraft.world.item.enchantment.EnchantmentEffectComponents.PREVENT_ARMOR_CHANGE)) {
                        cured.setItemSlot(equipmentSlot, itemStack);
                    } else {
                        this.entity.spawnAtLocation((ServerLevel) this.entity.level(), itemStack);
                    }
                }
            }
            ServerLevel serverLevel = (ServerLevel) this.entity.level();
            cured.finalizeSpawn(serverLevel, serverLevel.getCurrentDifficultyAt(cured.blockPosition()), EntitySpawnReason.CONVERSION, null);
            cured.addEffect(new MobEffectInstance(MobEffects.NAUSEA, 200, 0));
            cured.setBaby(this.entity.isBaby());

            if (!this.entity.isSilent()) {
                this.entity.level().levelEvent(1027, this.entity.blockPosition(), 0);
            }
        }
        return cured;
    }

    protected @Nullable Mob createCuredEntity() {
        return null;
    }
}
