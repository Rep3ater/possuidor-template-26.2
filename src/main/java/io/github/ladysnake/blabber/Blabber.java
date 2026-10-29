package io.github.ladysnake.blabber;

import com.mojang.serialization.Codec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * Port of the Blabber dialogue library public API.
 * The dialogue engine itself lives in {@code io.github.ladysnake.blabber.impl}.
 */
public final class Blabber {
    public static final String MOD_ID = "blabber";
    public static final Logger LOGGER = LogManager.getLogger("blabber");
    /** Registered dialogue action codecs, by id. */
    public static final Map<ResourceLocation, Codec<? extends DialogueAction>> ACTIONS = new HashMap<>();

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }

    public static void startDialogue(ServerPlayer player, ResourceLocation id) {
        io.github.ladysnake.blabber.impl.common.PlayerDialogueTracker.get(player).startDialogue(id);
    }

    public static void endDialogue(ServerPlayer player, @Nullable ResourceLocation expectedDialogue) {
        io.github.ladysnake.blabber.impl.common.PlayerDialogueTracker.get(player).endDialogue(expectedDialogue);
    }

    public static void registerAction(ResourceLocation actionId, DialogueAction action) {
        registerAction(actionId, com.mojang.serialization.MapCodec.unitCodec(action));
    }

    public static void registerAction(ResourceLocation actionId, Codec<? extends DialogueAction> codec) {
        ACTIONS.put(actionId, codec);
    }
}