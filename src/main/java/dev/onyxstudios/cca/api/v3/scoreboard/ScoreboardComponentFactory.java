package dev.onyxstudios.cca.api.v3.scoreboard;

import dev.onyxstudios.cca.api.v3.component.Component;
import javax.annotation.Nullable;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.scores.Scoreboard;

@FunctionalInterface
public interface ScoreboardComponentFactory<C extends Component> {
    C createForScoreboard(Scoreboard scoreboard, @Nullable MinecraftServer server);
}
