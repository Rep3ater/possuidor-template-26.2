package io.github.ladysnake.locki;

public final class ModdedInventoryNodes {
    public static final InventoryNode TOOL_SPACE = Locki.registerNode(DefaultInventoryNodes.INVENTORY, "tool_space");
    public static final InventoryNode BACK = Locki.registerNode(TOOL_SPACE, "back");
    public static final InventoryNode BELT = Locki.registerNode(TOOL_SPACE, "belt");

    public static final InventoryNode BACK_SLOT = BACK;
    public static final InventoryNode BELT_SLOT = BELT;

    private ModdedInventoryNodes() {
    }

    public static void init() {
    }
}
