package ladysnake.requiem.core.util;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.AbstractList;
import java.util.List;

/** Access the 36 main inventory slots on Minecraft 1.21.1. */
public final class PlayerInventoryCompat {
    private PlayerInventoryCompat() { }

    public static List<ItemStack> mainInventory(Inventory inventory) {
        return new AbstractList<>() {
            @Override
            public ItemStack get(int index) {
                checkIndex(index);
                return inventory.getItem(index);
            }

            @Override
            public ItemStack set(int index, ItemStack stack) {
                checkIndex(index);
                ItemStack previous = inventory.getItem(index);
                inventory.setItem(index, stack);
                return previous;
            }

            @Override
            public int size() {
                return 36;
            }

            private void checkIndex(int index) {
                if (index < 0 || index >= size()) throw new IndexOutOfBoundsException(index);
            }
        };
    }
}
