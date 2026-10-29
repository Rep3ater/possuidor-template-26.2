package ladysnake.requiem.common.gamerule;

import net.minecraft.world.level.GameRules;

/**
 * Minecraft 1.21.1 supports boolean and integer game rules. Enum choices are stored as
 * integer ordinals and mapped back to the Requiem enums at the use site.
 */
public final class RequiemGamerules {
    private RequiemGamerules() {}

    public static final GameRules.Key<GameRules.BooleanValue> SHOW_POSSESSOR_NAMETAG =
        GameRules.register("requiem_show_possessor_nametag", GameRules.Category.PLAYER,
            GameRules.BooleanValue.create(false));
    public static final GameRules.Key<GameRules.BooleanValue> NO_CURE =
        GameRules.register("requiem_no_cure", GameRules.Category.PLAYER,
            GameRules.BooleanValue.create(false));
    public static final GameRules.Key<GameRules.IntegerValue> STARTING_SOUL_MODE =
        GameRules.register("requiem_starting_remnant_type", GameRules.Category.PLAYER,
            GameRules.IntegerValue.create(StartingRemnantType.CHOOSE.ordinal()));
    public static final GameRules.Key<GameRules.IntegerValue> POSSESSION_KEEP_INVENTORY =
        GameRules.register("requiem_possession_keep_inventory", GameRules.Category.PLAYER,
            GameRules.IntegerValue.create(PossessionKeepInventory.NEVER.ordinal()));
    public static final GameRules.Key<GameRules.IntegerValue> POSSESSION_DETECTION =
        GameRules.register("requiem_possession_detection", GameRules.Category.MOBS,
            GameRules.IntegerValue.create(PossessionDetection.NORMAL.ordinal()));
    public static final GameRules.Key<GameRules.BooleanValue> DROP_VESSEL_ON_DISCONNECT =
        GameRules.register("requiem_drop_vessel_on_disconnect", GameRules.Category.PLAYER,
            GameRules.BooleanValue.create(false));

    public static boolean showPossessorNametag(GameRules rules) {
        return rules.getBoolean(SHOW_POSSESSOR_NAMETAG);
    }

    public static boolean noCure(GameRules rules) {
        return rules.getBoolean(NO_CURE);
    }

    public static boolean dropVesselOnDisconnect(GameRules rules) {
        return rules.getBoolean(DROP_VESSEL_ON_DISCONNECT);
    }

    public static StartingRemnantType startingSoulMode(GameRules rules) {
        return fromOrdinal(StartingRemnantType.values(), rules.getInt(STARTING_SOUL_MODE), StartingRemnantType.CHOOSE);
    }

    public static PossessionKeepInventory possessionKeepInventory(GameRules rules) {
        return fromOrdinal(PossessionKeepInventory.values(), rules.getInt(POSSESSION_KEEP_INVENTORY), PossessionKeepInventory.NEVER);
    }

    public static PossessionDetection possessionDetection(GameRules rules) {
        return fromOrdinal(PossessionDetection.values(), rules.getInt(POSSESSION_DETECTION), PossessionDetection.NORMAL);
    }

    private static <T> T fromOrdinal(T[] values, int ordinal, T fallback) {
        return ordinal >= 0 && ordinal < values.length ? values[ordinal] : fallback;
    }

    public static void init() {
        // Calling init forces class initialization before game-world game rules are constructed.
    }
}
