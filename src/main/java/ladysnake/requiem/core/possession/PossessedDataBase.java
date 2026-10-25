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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
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
package ladysnake.requiem.core.possession;

import dev.onyxstudios.cca.api.v3.component.sync.AutoSyncedComponent;
import ladysnake.requiem.api.v1.event.requiem.SoulboundStackCheckCallback;
import ladysnake.requiem.api.v1.possession.Possessable;
import ladysnake.requiem.api.v1.possession.PossessedData;
import ladysnake.requiem.api.v1.possession.PossessionComponent;
import ladysnake.requiem.core.util.OrderedInventory;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundSetHeldSlotPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Util;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public abstract class PossessedDataBase implements PossessedData, AutoSyncedComponent {
    public static void onMobConverted(LivingEntity original, LivingEntity converted) {
        Player possessor = ((Possessable) original).getPossessor();
        PossessedData possessedData = KEY.get(converted);
        if (possessor != null) {
            PossessionComponent.get(possessor).stopPossessing(false);
            possessedData.setConvertedUnderPossession();
            // The possession will start when the entity is added to the world
            ((Possessable) converted).setPossessor(possessor);
        }
        // copy possessed data to avoid losing the inventory
        possessedData.copyFrom(KEY.get(original));
    }

    protected final Entity holder;
    private @Nullable CompoundTag hungerData;
    private @Nullable OrderedInventory inventory;
    private boolean convertedUnderPossession;
    private int selectedSlot;

    public PossessedDataBase(Entity holder) {
        this.holder = holder;
    }

    @Override
    public void setConvertedUnderPossession() {
        this.convertedUnderPossession = true;
        // Explicit sync not necessary as the entity will be spawned later
    }

    @Override
    public boolean wasConvertedUnderPossession() {
        return this.convertedUnderPossession;
    }

    @Override
    public void moveItems(Inventory inventory, boolean fromPlayerToThis) {
        if (inventory.player.level().isClientSide()) return;

        if (fromPlayerToThis) {
            this.dropItems();
            this.inventory = new OrderedInventory(inventory.getContainerSize());
            for (int i = 0; i < inventory.getContainerSize(); i++) {
                if (!SoulboundStackCheckCallback.EVENT.invoker().isSoulbound(inventory, inventory.getItem(i), i)) {
                    this.inventory.setItem(i, inventory.removeItemNoUpdate(i));
                }
            }
            this.selectedSlot = inventory.getSelectedSlot();
            this.onPossessed();
        } else {
            if (this.inventory != null) {
                for (int i = 0; i < this.inventory.getContainerSize(); i++) {
                    ItemStack playerStack = inventory.removeItemNoUpdate(i);
                    if (!playerStack.isEmpty() && this.holder.level() instanceof ServerLevel serverLevel) {
                        this.holder.spawnAtLocation(serverLevel, playerStack);
                    }
                    inventory.setItem(i, this.inventory.removeItemNoUpdate(i));
                }
                this.inventory = null;
                inventory.setSelectedSlot(this.selectedSlot);
                if (inventory.player instanceof ServerPlayer serverPlayer) {
                    serverPlayer.connection.send(new ClientboundSetHeldSlotPacket(inventory.getSelectedSlot()));
                }
            }
        }
    }

    protected abstract void onPossessed();

    @Override
    public void dropItems() {
        if (this.inventory != null && this.holder.level() instanceof ServerLevel serverLevel) {
            this.inventory.clearToList().forEach(stack -> this.holder.spawnAtLocation(serverLevel, stack));
        }
    }

    @Override
    public CompoundTag getHungerData() {
        if (this.hungerData == null) {
            this.hungerData = new CompoundTag();
            this.hungerData.putInt("foodLevel", 20);
            this.hungerData.putFloat("foodSaturationLevel", 5.0F);
        }
        return this.hungerData;
    }

    @Override
    public void copyFrom(PossessedData original) {
        this.readFromNbt(Util.make(new CompoundTag(), original::writeToNbt));
    }

    @Override
    public void writeSyncPacket(RegistryFriendlyByteBuf buf, ServerPlayer recipient) {
        buf.writeBoolean(this.convertedUnderPossession);
    }

    @Override
    public void applySyncPacket(RegistryFriendlyByteBuf buf) {
        this.convertedUnderPossession = buf.readBoolean();
    }

    @Override
    public void readFromNbt(CompoundTag tag) {
        if (tag.contains("hunger_data")) {
            this.hungerData = tag.getCompoundOrEmpty("hunger_data");
        }

        if (tag.contains("inventory_size")) {
            ListTag items = tag.getListOrEmpty("inventory");
            this.inventory = new OrderedInventory(tag.getIntOr("inventory_size", 0));
            this.inventory.readNbtList(items);
        }

        this.selectedSlot = tag.getIntOr("selected_slot", 0);

        if (tag.contains("converted_under_possession")) {
            this.convertedUnderPossession = tag.getBooleanOr("converted_under_possession", false);
        }
    }

    @Override
    public void writeToNbt(CompoundTag tag) {
        if (this.hungerData != null) {
            Player possessor = ((Possessable) this.holder).getPossessor();
            if (possessor != null) {
                this.hungerData.putInt("foodLevel", possessor.getFoodData().getFoodLevel());
                this.hungerData.putFloat("foodSaturationLevel", possessor.getFoodData().getSaturationLevel());
            }
            tag.put("hunger_data", this.hungerData.copy());
        }
        if (this.inventory != null) {
            tag.putInt("inventory_size", this.inventory.getContainerSize());
            tag.put("inventory", this.inventory.toNbtList());
        }
        tag.putInt("selected_slot", this.selectedSlot);
        tag.putBoolean("converted_under_possession", this.convertedUnderPossession);
    }
}
