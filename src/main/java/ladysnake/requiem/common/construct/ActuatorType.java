package ladysnake.requiem.common.construct;

public enum ActuatorType {
    NONE("none"),
    PISTON_THRUST("piston_thrust"),
    HYDRAULIC_JUMP("hydraulic_jump"),
    DISPENSER_CANNON("dispenser_cannon"),
    REDSTONE_PULSE("redstone_pulse"),
    KEY_R_TRIGGER("key_r_trigger"),
    KEY_V_TRIGGER("key_v_trigger");

    private final String name;

    ActuatorType(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public static ActuatorType fromOrdinal(int ordinal) {
        ActuatorType[] values = values();
        if (ordinal < 0 || ordinal >= values.length) {
            return NONE;
        }
        return values[ordinal];
    }
}
