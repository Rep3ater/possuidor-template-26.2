package baritone.api;

public class Settings {
    public static class Setting<T> {
        public T value;
        public Setting(T val) { this.value = val; }
        public void set(T val) { this.value = val; }
        public T defaultValue() { return value; }
    }
    public final Setting<Boolean> allowBreak = new Setting<>(true);
    public final Setting<Boolean> allowPlace = new Setting<>(true);
    public final Setting<Boolean> allowSprint = new Setting<>(true);
    public final Setting<Boolean> allowParkour = new Setting<>(true);
    public final Setting<Double> blockBreakAdditionalPenalty = new Setting<>(1.0);
    public final Setting<Double> blockPlacementPenalty = new Setting<>(1.0);
}
