package baritone.api.fakeplayer;

import baritone.api.IBaritone;
import baritone.api.Settings;
import baritone.api.behavior.IPathingBehavior;
import baritone.api.event.listener.IEventBus;
import baritone.api.event.listener.IGameEventListener;
import baritone.api.pathing.calc.IPathingControlManager;
import baritone.api.process.IBaritoneProcess;
import baritone.api.utils.IEntityContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

public class DummyBaritoneImpl implements IBaritone {
    private final Settings settings = new Settings();
    private final List<IBaritoneProcess> processes = new ArrayList<>();
    private final List<IGameEventListener> listeners = new ArrayList<>();
    private final Player player;

    public DummyBaritoneImpl(Player player) {
        this.player = player;
    }

    @Override
    public IPathingControlManager getPathingControlManager() {
        return processes::add;
    }

    @Override
    public IEventBus getGameEventHandler() {
        return listeners::add;
    }

    @Override
    public baritone.api.behavior.ILookBehavior getLookBehavior() {
        return new baritone.api.behavior.ILookBehavior() {};
    }

    @Override
    public Settings settings() {
        return settings;
    }

    @Override
    public IEntityContext getPlayerContext() {
        return new IEntityContext() {
            @Override
            public LivingEntity entity() { return player; }
            @Override
            public Level world() { return player.level(); }
            @Override
            public BlockPos feetPos() { return player.blockPosition(); }
        };
    }

    @Override
    public IPathingBehavior getPathingBehavior() {
        return () -> player.blockPosition();
    }

    @Override
    public void readFromNbt(CompoundTag tag, HolderLookup.Provider provider) {}

    @Override
    public void writeToNbt(CompoundTag tag, HolderLookup.Provider provider) {}
}
