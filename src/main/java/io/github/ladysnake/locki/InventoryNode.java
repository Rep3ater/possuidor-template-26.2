package io.github.ladysnake.locki;

import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import java.util.Collections;
import java.util.Set;
import javax.annotation.Nullable;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;

public final class InventoryNode implements Comparable<InventoryNode> {
    public static final InventoryNode ROOT = new InventoryNode(null, "");

    @Nullable
    private final InventoryNode parent;
    private final Set<InventoryNode> descendants = new ReferenceOpenHashSet<>();
    private final Set<InventoryNode> unmodifiableDescendants = Collections.unmodifiableSet(this.descendants);
    private final String name;

    @Internal
    InventoryNode(@Nullable InventoryNode parent, String name) {
        this.parent = parent;
        this.name = name;
    }

    @Internal
    void addDescendant(InventoryNode node) {
        this.descendants.add(node);
        if (this.parent != null) {
            this.parent.addDescendant(node);
        }
    }

    @Nullable
    public InventoryNode getParent() {
        return this.parent;
    }

    public Set<InventoryNode> getDescendants() {
        return this.unmodifiableDescendants;
    }

    public String getFullName() {
        return this.name;
    }

    @Override
    public int compareTo(@NotNull InventoryNode o) {
        return this.name.compareTo(o.name);
    }

    @Override
    public String toString() {
        return this.name;
    }
}
