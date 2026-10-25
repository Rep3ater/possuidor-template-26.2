package baritone.api.event.listener;

import baritone.api.event.events.BlockInteractEvent;
import baritone.api.event.events.PathEvent;

public interface IGameEventListener {
    default void onTickServer() {}
    default void onBlockInteract(BlockInteractEvent event) {}
    default void onPathEvent(PathEvent event) {}
}