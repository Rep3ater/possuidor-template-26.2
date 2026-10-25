package io.github.ladysnake.blabber.impl.common;

import ladysnake.requiem.common.network.RequiemNetworking;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

public final class PlayerDialogueTracker {
    private final Player player;

    private PlayerDialogueTracker(Player player) {
        this.player = player;
    }

    public static PlayerDialogueTracker get(Player player) {
        return new PlayerDialogueTracker(player);
    }

    public void startDialogue(Identifier id) {
        if (this.player instanceof ServerPlayer sp) {
            RequiemNetworking.sendOpenDialogueMessage(sp, id.toString());
        }
    }

    public void endDialogue(@Nullable Identifier expected) {
    }
}
