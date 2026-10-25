package io.github.ladysnake.locki;

import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;

public final class InventoryLock {
    private final Identifier id;
    private final int rawId;
    private final boolean persistent;

    InventoryLock(Identifier id, int rawId, boolean persistent) {
        this.id = id;
        this.rawId = rawId;
        this.persistent = persistent;
    }

    public Identifier getId() {
        return this.id;
    }

    public int getRawId() {
        return this.rawId;
    }

    public boolean shouldSave() {
        return this.persistent;
    }

    public void lock(Player player, InventoryNode invNode) {
        InventoryKeeper.get(player).addLock(this, invNode);
    }

    public void lock(Player player, EquipmentSlot equipmentSlot) {
        this.lock(player, DefaultInventoryNodes.get(equipmentSlot));
    }

    public void lockInventory(Player player) {
        this.lock(player, DefaultInventoryNodes.INVENTORY);
    }

    public void unlock(Player player, InventoryNode invNode) {
        InventoryKeeper.get(player).removeLock(this, invNode);
    }

    public void unlock(Player player, EquipmentSlot equipmentSlot) {
        this.unlock(player, DefaultInventoryNodes.get(equipmentSlot));
    }

    public void unlockInventory(Player player) {
        this.unlock(player, DefaultInventoryNodes.INVENTORY);
    }

    public boolean isPlacedOn(Player player, InventoryNode invNode) {
        return InventoryKeeper.get(player).getAllPlacedLocks(invNode).contains(this);
    }

    public boolean isLocking(Player player, InventoryNode invNode) {
        return InventoryKeeper.get(player).isLockedBy(this, invNode);
    }

    public void toggle(Player player, InventoryNode invNode) {
        InventoryKeeper keeper = InventoryKeeper.get(player);
        if (keeper.isLockedBy(this, invNode)) {
            keeper.removeLock(this, invNode);
        } else {
            keeper.addLock(this, invNode);
        }
    }

    @Override
    public String toString() {
        return "\ud83d\udd12[" + this.id + "]";
    }
}
