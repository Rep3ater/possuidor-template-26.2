package net.fabricmc.fabric.api.entity.event.v1;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;

public final class EntitySleepEvents {
    public static final Event<AllowSleeping> ALLOW_SLEEPING = EventFactory.createArrayBacked(AllowSleeping.class,
        listeners -> (player, pos) -> {
            for (AllowSleeping listener : listeners) {
                Player.BedSleepingProblem result = listener.allowSleep(player, pos);
                if (result != null) return result;
            }
            return null;
        }
    );

    @FunctionalInterface
    public interface AllowSleeping {
        Player.BedSleepingProblem allowSleep(Player player, BlockPos pos);
    }
}
