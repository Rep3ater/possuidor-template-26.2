package io.github.ladysnake.locki;

import io.github.ladysnake.locki.impl.LockiComponents;
import java.util.Set;
import net.minecraft.world.entity.player.Player;

public interface InventoryKeeper {
    static InventoryKeeper get(Player player) {
        return LockiComponents.INVENTORY_KEEPER.get(player);
    }

    boolean isLocked(InventoryNode invNode);

    boolean isEntirelyLocked(InventoryNode invNode);

    boolean isSlotLocked(int slot);

    boolean isLockedBy(InventoryLock lock, InventoryNode invNode);

    void addLock(InventoryLock lock, InventoryNode invNode);

    void removeLock(InventoryLock lock, InventoryNode invNode);

    default void toggleLock(InventoryLock lock, InventoryNode invNode) {
        if (this.isLockedBy(lock, invNode)) {
            this.removeLock(lock, invNode);
        } else {
            this.addLock(lock, invNode);
        }
    }

    void forceRefresh();

    Set<InventoryLock> getAllPlacedLocks(InventoryNode invNode);
}
