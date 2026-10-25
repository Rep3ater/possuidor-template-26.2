package io.github.ladysnake.locki;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import dev.onyxstudios.cca.internal.neoforge.CcaNeoForgeEngine;
import io.github.ladysnake.locki.impl.LockiComponents;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.resources.Identifier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Contract;

public final class Locki {
    public static final Logger LOGGER = LogManager.getLogger("Locki");
    public static final Pattern NODE_NAME_PART = Pattern.compile("[a-z0-9_-]+");

    private static final Map<Identifier, InventoryLock> locks = new HashMap<>();
    private static final Map<String, InventoryNode> nodes = new HashMap<>();
    private static int nextId;
    private static final AtomicBoolean INITIALIZED = new AtomicBoolean(false);

    private Locki() {
    }

    public static void init() {
        if (!INITIALIZED.compareAndSet(false, true)) {
            return;
        }
        DefaultInventoryNodes.init();
        ModdedInventoryNodes.init();
        CcaNeoForgeEngine.registerEntityInitializer(new LockiComponents());
    }

    public static synchronized InventoryLock registerLock(Identifier id) {
        return registerLock(id, true);
    }

    public static synchronized InventoryLock registerLock(Identifier id, boolean persistent) {
        Preconditions.checkNotNull(id, "id");
        InventoryLock lock = locks.computeIfAbsent(id, id1 -> new InventoryLock(id1, nextId++, persistent));
        if (lock.shouldSave() != persistent) {
            throw new IllegalStateException("Lock %s registered twice with differing persistence (%s, %s)"
                .formatted(id, lock.shouldSave(), persistent));
        }
        return lock;
    }

    @Contract(value = "null -> null; !null -> _", pure = true)
    @Nullable
    public static synchronized InventoryLock getLock(@Nullable Identifier id) {
        return id == null ? null : locks.get(id);
    }

    public static synchronized InventoryNode registerNode(InventoryNode parent, String name) {
        Preconditions.checkNotNull(parent, "parent");
        Preconditions.checkNotNull(name, "name");
        Preconditions.checkArgument(NODE_NAME_PART.matcher(name).matches(), "Invalid node name: %s", name);
        String fullName = parent == InventoryNode.ROOT ? name : parent.getFullName() + "." + name;
        return nodes.computeIfAbsent(fullName, n -> {
            InventoryNode created = new InventoryNode(parent, n);
            parent.addDescendant(created);
            return created;
        });
    }

    @Contract(value = "null -> null; !null -> _", pure = true)
    @Nullable
    public static synchronized InventoryNode getNode(@Nullable String fullName) {
        return fullName == null ? null : nodes.get(fullName);
    }

    public static synchronized Stream<Identifier> streamLockIds() {
        return locks.keySet().stream();
    }

    public static synchronized Stream<String> streamNodeNames() {
        return nodes.keySet().stream();
    }

    @VisibleForTesting
    static synchronized void reset(Predicate<String> nodeRemovalFilter) {
        locks.clear();
        nodes.keySet().removeIf(nodeRemovalFilter);
        nextId = 0;
    }
}
