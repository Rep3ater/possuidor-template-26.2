package io.github.ladysnake.locki;

import com.google.common.collect.ImmutableList;
import net.minecraft.world.entity.EquipmentSlot;

public final class DefaultInventoryNodes {
    public static final InventoryNode INVENTORY = Locki.registerNode(InventoryNode.ROOT, "inventory");
    public static final InventoryNode MAIN_INVENTORY = Locki.registerNode(INVENTORY, "main");
    public static final InventoryNode HOTBAR = Locki.registerNode(MAIN_INVENTORY, "hotbar");
    public static final InventoryNode HANDS = Locki.registerNode(INVENTORY, "hands");
    public static final InventoryNode MAIN_HAND = Locki.registerNode(HANDS, "main_hand");
    public static final InventoryNode OFF_HAND = Locki.registerNode(HANDS, "off_hand");
    public static final InventoryNode ARMOR = Locki.registerNode(INVENTORY, "armor");
    public static final InventoryNode FEET = Locki.registerNode(ARMOR, "feet");
    public static final InventoryNode LEGS = Locki.registerNode(ARMOR, "legs");
    public static final InventoryNode CHEST = Locki.registerNode(ARMOR, "chest");
    public static final InventoryNode HEAD = Locki.registerNode(ARMOR, "head");
    public static final InventoryNode BODY = Locki.registerNode(ARMOR, "body");
    public static final InventoryNode SADDLE = Locki.registerNode(ARMOR, "saddle");
    public static final ImmutableList<InventoryNode> ARMOR_SLOTS = ImmutableList.of(FEET, LEGS, CHEST, HEAD);
    public static final InventoryNode CRAFTING = Locki.registerNode(INVENTORY, "crafting");
    public static final InventoryNode CRAFTING_GRID = Locki.registerNode(CRAFTING, "grid");
    public static final InventoryNode CRAFTING_RESULT = Locki.registerNode(CRAFTING, "result");
    public static final InventoryNode CRAFTING_BOOK = Locki.registerNode(CRAFTING, "book");

    private DefaultInventoryNodes() {
    }

    public static InventoryNode get(EquipmentSlot equipmentSlot) {
        return switch (equipmentSlot) {
            case MAINHAND -> MAIN_HAND;
            case OFFHAND -> OFF_HAND;
            case FEET -> FEET;
            case LEGS -> LEGS;
            case CHEST -> CHEST;
            case HEAD -> HEAD;
            case BODY -> BODY;
            case SADDLE -> SADDLE;
        };
    }

    public static void init() {
    }
}
