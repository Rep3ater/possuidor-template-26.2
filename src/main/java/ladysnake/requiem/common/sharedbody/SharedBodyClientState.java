package ladysnake.requiem.common.sharedbody;

import java.util.Collections;
import java.util.Set;

/**
 * Client-side mirror of the mobs that are currently driven by the local player through the Shared Body effect.
 *
 * <p>Deliberately free of any client-only class so it can be referenced from common networking code.
 */
public final class SharedBodyClientState {
    private static volatile Set<Integer> controlled = Collections.emptySet();
    private static boolean lastAttack;
    private static boolean lastUse;

    private SharedBodyClientState() {}

    public static void setControlled(Set<Integer> ids) {
        controlled = ids;
    }

    public static boolean isControlled(int entityId) {
        return controlled.contains(entityId);
    }

    public static boolean hasControlled() {
        return !controlled.isEmpty();
    }

    /**
     * Sends the held state of attack/use keys to the server, but only when it changed since the last call.
     */
    public static void updateHeldKeys(boolean attack, boolean use, Runnable sender) {
        if (attack != lastAttack || use != lastUse) {
            lastAttack = attack;
            lastUse = use;
            sender.run();
        }
    }

    public static void reset() {
        controlled = Collections.emptySet();
        lastAttack = false;
        lastUse = false;
    }
}
