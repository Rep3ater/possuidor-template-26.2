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
package ladysnake.requiem.common.entity;

import dev.onyxstudios.cca.api.v3.component.Component;
import dev.onyxstudios.cca.api.v3.component.ComponentKey;
import dev.onyxstudios.cca.api.v3.component.ComponentRegistry;
import ladysnake.requiem.Requiem;
import ladysnake.requiem.api.v1.possession.Possessable;
import ladysnake.requiem.common.advancement.criterion.RequiemCriteria;
import ladysnake.requiem.common.tag.RequiemEntityTypeTags;
import ladysnake.requiem.core.tag.RequiemCoreTags;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.entity.monster.WitherSkeleton;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class SkeletonBoneComponent implements Component {
    public static final ComponentKey<SkeletonBoneComponent> KEY = ComponentRegistry.getOrCreate(Requiem.id("skeleton_bones"), SkeletonBoneComponent.class);

    private final Mob owner;
    private int replacedBones;

    public SkeletonBoneComponent(Mob owner) {
        this.owner = owner;
    }

    public boolean replaceBone() {
        if (this.owner.isAlive() && this.owner.getHealth() < this.owner.getMaxHealth()) {
            this.replacedBones++;
            this.owner.heal(4.0f);
            this.owner.playAmbientSound();

            if (this.shouldBeReplaced() && !this.owner.level().isClientSide()) {
                this.replaceSkeleton();
            }

            return true;
        }
        return false;
    }

    private void replaceSkeleton() {
        Player possessor = ((Possessable) this.owner).getPossessor();
        Skeleton replacement = this.owner.convertTo(EntityType.SKELETON, true);
        if (replacement != null) {
            replacement.setHealth(this.owner.getHealth());
            if (possessor instanceof ServerPlayer) {
                RequiemCriteria.TRANSFORMED_POSSESSED_ENTITY.handle(((ServerPlayer) possessor), this.owner, replacement, false);
            }
            if (this.owner instanceof WitherSkeleton
                && replacement.getItemBySlot(EquipmentSlot.HEAD).isEmpty()
                && !replacement.getType().builtInRegistryHolder().is(RequiemCoreTags.Entity.ARMOR_BANNED)) {
                if (this.owner.getRandom().nextInt(5) == 0) {
                    replacement.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.WITHER_SKELETON_SKULL));
                }
            }
        }
    }

    private boolean shouldBeReplaced() {
        if (this.owner.getType().builtInRegistryHolder().is(RequiemEntityTypeTags.REPLACEABLE_SKELETONS)) {
            return switch (this.owner.level().getDifficulty()) {
                case PEACEFUL -> false; // what is this skeleton doing in peaceful anyway....?
                case EASY -> this.replacedBones > 8;
                case NORMAL -> this.replacedBones > 4;
                default -> this.replacedBones > 2;
            };
        }
        return false;
    }

    @Override
    public void readFromNbt(CompoundTag tag) {
        if (tag.contains("replaced_bones")) {
            this.replacedBones = tag.getIntOr("replaced_bones", 0);
        }
    }

    @Override
    public void writeToNbt(CompoundTag tag) {
        if (this.replacedBones > 0) {
            tag.putInt("replaced_bones", this.replacedBones);
        }
    }
}
