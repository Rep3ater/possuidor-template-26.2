package io.github.ladysnake.locki.impl;

import dev.onyxstudios.cca.api.v3.component.Component;
import io.github.ladysnake.locki.InventoryKeeper;
import io.github.ladysnake.locki.InventoryLock;
import io.github.ladysnake.locki.InventoryNode;
import io.github.ladysnake.locki.Locki;
import it.unimi.dsi.fastutil.objects.Reference2BooleanMap;
import it.unimi.dsi.fastutil.objects.Reference2BooleanOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import java.util.BitSet;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;

public class InventoryKeeperBase implements Component, InventoryKeeper {
    private final Map<InventoryNode, Reference2BooleanMap<InventoryLock>> locks = new TreeMap<>();
    protected final Map<InventoryNode, BitSet> cache = new Reference2ObjectOpenHashMap<>();

    @Override
    public boolean isLocked(InventoryNode invNode) {
        return !this.lookup(invNode).isEmpty();
    }

    @Override
    public boolean isEntirelyLocked(InventoryNode invNode) {
        if (!this.isLocked(invNode)) {
            return false;
        }
        for (InventoryNode descendant : invNode.getDescendants()) {
            if (!this.isLocked(descendant)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean isLockedBy(InventoryLock lock, InventoryNode invNode) {
        return this.lookup(invNode).get(lock.getRawId());
    }

    @Override
    public Set<InventoryLock> getAllPlacedLocks(InventoryNode invNode) {
        Reference2BooleanMap<InventoryLock> map = this.getLocks().get(invNode);
        return map == null ? Collections.emptySet() : map.keySet();
    }

    @Override
    public void addLock(InventoryLock lock, InventoryNode invNode) {
        this.updateLock(lock, invNode, true);
    }

    @Override
    public void removeLock(InventoryLock lock, InventoryNode invNode) {
        this.updateLock(lock, invNode, false);
    }

    @Override
    public boolean isSlotLocked(int slot) {
        throw new UnsupportedOperationException();
    }

    protected void updateLock(InventoryLock lock, InventoryNode invNode, boolean locking) {
        this.doUpdateLock(lock, invNode, locking);
        for (InventoryNode subNode : invNode.getDescendants()) {
            Reference2BooleanMap<InventoryLock> subLocks = this.getLocks().get(subNode);
            if (subLocks != null) {
                subLocks.removeBoolean(lock);
            }
        }
        this.propagateChange(invNode, lock, locking);
    }

    private void doUpdateLock(InventoryLock lock, InventoryNode invNode, boolean locking) {
        this.getLocks().computeIfAbsent(invNode, n -> new Reference2BooleanOpenHashMap<>()).put(lock, locking);
    }

    protected boolean propagateChange(InventoryNode inventoryNode, InventoryLock lock, boolean locking) {
        boolean changed = this.updateCachedLockState(lock, locking, inventoryNode);
        for (InventoryNode child : inventoryNode.getDescendants()) {
            changed |= this.updateCachedLockState(lock, locking, child);
        }
        return changed;
    }

    protected boolean updateCachedLockState(InventoryLock lock, boolean locking, InventoryNode inventoryNode) {
        BitSet bitset = this.lookup(inventoryNode);
        if (bitset.get(lock.getRawId()) != locking) {
            bitset.set(lock.getRawId(), locking);
            return true;
        }
        return false;
    }

    @Override
    public void forceRefresh() {
        this.getCache().clear();
        for (Map.Entry<InventoryNode, Reference2BooleanMap<InventoryLock>> entry : this.getLocks().entrySet()) {
            for (Reference2BooleanMap.Entry<InventoryLock> e : entry.getValue().reference2BooleanEntrySet()) {
                this.propagateChange(entry.getKey(), e.getKey(), e.getBooleanValue());
            }
        }
    }

    protected Map<InventoryNode, Reference2BooleanMap<InventoryLock>> getLocks() {
        return this.locks;
    }

    protected Map<InventoryNode, BitSet> getCache() {
        return this.cache;
    }

    protected void clearCache() {
        this.cache.clear();
    }

    protected BitSet lookup(InventoryNode inventoryNode) {
        return this.getCache().computeIfAbsent(inventoryNode, n -> new BitSet());
    }

    @Override
    public void readFromNbt(CompoundTag tag, HolderLookup.Provider registryLookup) {
        this.readFromNbt(tag);
    }

    @Override
    public void readFromNbt(CompoundTag tag) {
        if (tag.contains("locks")) {
            this.clearCache();
            this.getLocks().clear();
            CompoundTag dict = tag.getCompound("locks");
            for (String key : new TreeSet<>(dict.getAllKeys())) {
                InventoryNode node = Locki.getNode(key);
                if (node != null) {
                    ListTag list = dict.getList(key, net.minecraft.nbt.Tag.TAG_COMPOUND);
                    for (Tag elem : list) {
                        if (elem instanceof CompoundTag lockInfo) {
                            ResourceLocation id = ResourceLocation.tryParse(lockInfo.getString("id"));
                            InventoryLock lock = Locki.getLock(id);
                            if (lock != null) {
                                this.updateLock(lock, node, lockInfo.getBoolean("locking"));
                            } else {
                                Locki.LOGGER.error("Dropping unregistered inventory lock {}", lockInfo);
                            }
                        }
                    }
                } else {
                    Locki.LOGGER.error("Dropping lock data for unregistered inventory node {}", key);
                }
            }
        }
    }

    @Override
    public void writeToNbt(CompoundTag tag, HolderLookup.Provider registryLookup) {
        this.writeToNbt(tag);
    }

    @Override
    public void writeToNbt(CompoundTag tag) {
        CompoundTag dict = new CompoundTag();
        for (Map.Entry<InventoryNode, Reference2BooleanMap<InventoryLock>> nodeEntry : this.getLocks().entrySet()) {
            ListTag list = new ListTag();
            for (Reference2BooleanMap.Entry<InventoryLock> lockEntry : nodeEntry.getValue().reference2BooleanEntrySet()) {
                if (lockEntry.getKey().shouldSave()) {
                    CompoundTag l = new CompoundTag();
                    l.putString("id", lockEntry.getKey().getId().toString());
                    l.putBoolean("locking", lockEntry.getBooleanValue());
                    list.add(l);
                }
            }
            if (!list.isEmpty()) {
                dict.put(nodeEntry.getKey().getFullName(), list);
            }
        }
        if (!dict.isEmpty()) {
            tag.put("locks", dict);
        }
    }
}
