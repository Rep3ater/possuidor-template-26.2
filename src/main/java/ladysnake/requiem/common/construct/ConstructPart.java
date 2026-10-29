package ladysnake.requiem.common.construct;

public enum ConstructPart {
    TORSO("torso"),
    HEAD("head"),
    LEFT_ARM("left_arm"),
    RIGHT_ARM("right_arm"),
    LEFT_LEG("left_leg"),
    RIGHT_LEG("right_leg");

    private final String name;

    ConstructPart(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public static ConstructPart fromOrdinal(int ordinal) {
        ConstructPart[] values = values();
        if (ordinal < 0 || ordinal >= values.length) {
            return TORSO;
        }
        return values[ordinal];
    }
}
