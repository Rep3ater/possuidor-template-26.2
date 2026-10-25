package io.github.ladysnake.locki.impl;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import dev.onyxstudios.cca.api.v3.component.sync.AutoSyncedComponent;
import io.github.ladysnake.locki.DefaultInventoryNodes;
import io.github.ladysnake.locki.InventoryKeeper;
import io.github.ladysnake.locki.InventoryLock;
import io.github.ladysnake.locki.InventoryLockingChangeCallback;
import io.github.ladysnake.locki.InventoryNode;
import io.github.ladysnake.locki.Locki;
import io.github.ladysnake.locki.ModdedInventoryNodes;
import it.unimi.dsi.fastutil.objects.Reference2BooleanMap;
import java.util.BitSet;
import java.util.Map;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;

public class PlayerInventoryKeeper extends InventoryKeeperBase implements AutoSyncedComponent {
    public static final int MAINHAND_SLOT = 0;
    public static final int OFFHAND_SLOT = 40;
    public static final int BACK_SLOT = 41;
    public static final int BELT_SLOT = 42;

    private final Player player;

    public static int fixSelectedSlot(Player player, int selectedSlot) {
        InventoryKeeper inventoryKeeper = InventoryKeeper.get(player);
        if (inventoryKeeper.isLocked(DefaultInventoryNodes.MAIN_HAND)) {
            return 0;
        }
        int hotbarSize = Inventory.getSelectionSize();
        int attempts = 0;
        while (inventoryKeeper.isSlotLocked(selectedSlot) && attempts < hotbarSize) {
            selectedSlot = (selectedSlot + 1) % hotbarSize;
            attempts++;
        }
        return selectedSlot;
    }

    public PlayerInventoryKeeper(Player player) {
        this.player = player;
    }

    @Override
    protected Map<InventoryNode, Reference2BooleanMap<InventoryLock>> getLocks() {
        Preconditions.checkState(!this.player.level().isClientSide(), "Locks can only be managed serverside (check !level.isClientSide)");
        return super.getLocks();
    }

    @Override
    public boolean shouldSyncWith(ServerPlayer player) {
        return player == this.player;
    }

    @Override
    public void writeSyncPacket(RegistryFriendlyByteBuf buf, ServerPlayer recipient) {
        buf.writeVarInt(this.cache.size());
        for (Map.Entry<InventoryNode, BitSet> entry : this.getCache().entrySet()) {
            buf.writeUtf(entry.getKey().getFullName());
            buf.writeByteArray(entry.getValue().toByteArray());
        }
    }

    @Override
    public void applySyncPacket(RegistryFriendlyByteBuf buf) {
        int size = buf.readVarInt();
        this.clearCache();
        for (int i = 0; i < size; i++) {
            String key = buf.readUtf();
            byte[] locked = buf.readByteArray();
            InventoryNode node = Locki.getNode(key);
            if (node == null) {
                Locki.LOGGER.error("Received unknown inventory node path during sync: {}", key);
            } else {
                BitSet value = BitSet.valueOf(locked);
                BitSet old = this.cache.put(node, value);
                if ((old == null || old.isEmpty()) != value.isEmpty()) {
                    InventoryLockingChangeCallback.EVENT.invoker().onLockingStateChanged(this.player, node, !value.isEmpty());
                }
            }
        }
    }

    @Override
    public boolean isLocked(InventoryNode invNode) {
        return !this.player.isCreative() && super.isLocked(invNode);
    }

    @Override
    public boolean isSlotLocked(int index) {
        int mainSize = Inventory.INVENTORY_SIZE;
        if (index <= 0 || index >= mainSize) {
            int armorIndex = index - mainSize;
            ImmutableList<InventoryNode> armorSlots = DefaultInventoryNodes.ARMOR_SLOTS;
            if (armorIndex >= 0 && armorIndex < armorSlots.size()) {
                return this.isLocked(armorSlots.get(armorIndex));
            }
            return switch (index) {
                case MAINHAND_SLOT -> this.isLocked(DefaultInventoryNodes.MAIN_HAND);
                case OFFHAND_SLOT -> this.isLocked(DefaultInventoryNodes.OFF_HAND);
                case BACK_SLOT -> this.isLocked(ModdedInventoryNodes.BACK);
                case BELT_SLOT -> this.isLocked(ModdedInventoryNodes.BELT);
                default -> false;
            };
        }
        return index < Inventory.getSelectionSize()
            ? this.isLocked(DefaultInventoryNodes.HOTBAR)
            : this.isLocked(DefaultInventoryNodes.MAIN_INVENTORY);
    }

    @Override
    protected boolean propagateChange(InventoryNode invNode, InventoryLock lock, boolean locking) {
        if (super.propagateChange(invNode, lock, locking)) {
            LockiComponents.INVENTORY_KEEPER.sync(this.player);
            return true;
        }
        return false;
    }

    @Override
    protected boolean updateCachedLockState(InventoryLock lock, boolean locking, InventoryNode invNode) {
        if (super.updateCachedLockState(lock, locking, invNode)) {
            InventoryLockingChangeCallback.EVENT.invoker().onLockingStateChanged(this.player, invNode, locking);
            return true;
        }
        return false;
    }
}
