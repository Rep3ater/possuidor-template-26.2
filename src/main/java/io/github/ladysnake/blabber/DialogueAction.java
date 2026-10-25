package io.github.ladysnake.blabber;

import net.minecraft.server.level.ServerPlayer;

@FunctionalInterface
public interface DialogueAction {
    void handle(ServerPlayer player);
}