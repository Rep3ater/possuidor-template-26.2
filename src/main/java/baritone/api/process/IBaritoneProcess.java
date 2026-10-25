package baritone.api.process;

public interface IBaritoneProcess {
    boolean isActive();
    PathingCommand onTick(boolean calcFailed, boolean isSafeToCancel);
    boolean isTemporary();
    void onLostControl();
    default double priority() { return 0; }
    default String displayName() { return "Process"; }
}
