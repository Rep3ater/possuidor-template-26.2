package ladysnake.requiem.core.util;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/** An inventory that preserves physical slot positions when serialized. */
public class OrderedInventory extends SimpleContainer {
    public OrderedInventory(int size) {
        super(size);
    }

    public void readNbtList(ListTag tags) {
        this.clearContent();
        for (int i = 0; i < tags.size(); i++) {
            Tag raw = tags.get(i);
            if (!(raw instanceof CompoundTag slotTag)) continue;
            int slot = slotTag.getInt("Slot");
            if (slot < 0 || slot >= this.getContainerSize()) continue;
            CompoundTag stackTag = slotTag.copy();
            stackTag.remove("Slot");
            ItemStack.CODEC.parse(NbtOps.INSTANCE, stackTag).result()
                .filter(stack -> !stack.isEmpty())
                .ifPresent(stack -> this.setItem(slot, stack));
        }
    }

    public ListTag toNbtList() {
        ListTag result = new ListTag();
        for (int slot = 0; slot < this.getContainerSize(); slot++) {
            ItemStack stack = this.getItem(slot);
            if (stack.isEmpty()) continue;
            final int slotIndex = slot;
            final int slotIndex = slot;
            ItemStack.CODEC.encodeStart(NbtOps.INSTANCE, stack).result().ifPresent(encoded -> {
                if (encoded instanceof CompoundTag itemTag) {
                    itemTag.putInt("Slot", slotIndex);
                    result.add(itemTag);
                }
            });
        }
        return result;
    }

    public List<ItemStack> clearToList() {
        return this.removeAllItems();
    }
}
