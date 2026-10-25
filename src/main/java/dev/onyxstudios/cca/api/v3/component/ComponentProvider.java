package dev.onyxstudios.cca.api.v3.component;

import dev.onyxstudios.cca.internal.neoforge.CcaNeoForgeEngine;
import java.util.Collections;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.scores.Scoreboard;

public interface ComponentProvider {
    ComponentContainer getComponentContainer();

    default ComponentProvider asComponentProvider() {
        return this;
    }

    default Iterable<ServerPlayer> getRecipientsForComponentSync() {
        return Collections.emptyList();
    }

    static ComponentProvider fromEntity(Entity entity) {
        if (entity instanceof ComponentProvider cp) {
            return cp;
        }
        return CcaNeoForgeEngine.getEntityProvider(entity);
    }

    static ComponentProvider fromScoreboard(Scoreboard scoreboard) {
        if (scoreboard instanceof ComponentProvider cp) {
            return cp;
        }
        return CcaNeoForgeEngine.getScoreboardProvider(scoreboard);
    }

    static ComponentProvider fromLevel(Level level) {
        return fromScoreboard(level.getScoreboard());
    }

    static ComponentProvider fromServer(MinecraftServer server) {
        return fromScoreboard(server.getScoreboard());
    }
}
