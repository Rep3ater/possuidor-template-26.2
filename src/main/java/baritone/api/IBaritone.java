package baritone.api;

import baritone.api.behavior.IPathingBehavior;
import baritone.api.event.listener.IEventBus;
import baritone.api.pathing.calc.IPathingControlManager;
import baritone.api.utils.IEntityContext;
import dev.onyxstudios.cca.api.v3.component.Component;
import dev.onyxstudios.cca.api.v3.component.ComponentKey;
import dev.onyxstudios.cca.api.v3.component.ComponentRegistry;
import net.minecraft.resources.ResourceLocation;

public interface IBaritone extends Component {
    ComponentKey<IBaritone> KEY = ComponentRegistry.getOrCreate(ResourceLocation.fromNamespaceAndPath("automatone", "baritone"), IBaritone.class);

    IPathingControlManager getPathingControlManager();
    IEventBus getGameEventHandler();
    Settings settings();
    baritone.api.behavior.ILookBehavior getLookBehavior();
    IEntityContext getPlayerContext();
    IPathingBehavior getPathingBehavior();
}
