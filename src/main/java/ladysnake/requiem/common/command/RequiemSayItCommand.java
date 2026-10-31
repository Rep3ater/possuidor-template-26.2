package ladysnake.requiem.common.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import ladysnake.requiem.common.SayItCompatibilityManager;
import ladysnake.requiem.common.RequiemConfig;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import java.util.Collection;
import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

/** Commands for controlling Say It English and Requiem speech modes. */
public final class RequiemSayItCommand {
    private RequiemSayItCommand() { }

    public static LiteralArgumentBuilder<CommandSourceStack> sayItSubcommand() {
        return literal("sayit")
            .then(literal("enable").executes(context -> {
                SayItCompatibilityManager.setGlobalEnabled(true);
                context.getSource().sendSuccess(() -> Component.literal("Say It English is enabled for the server."), true);
                return 1;
            }))
            .then(literal("disable").executes(context -> {
                SayItCompatibilityManager.setGlobalEnabled(false);
                context.getSource().sendSuccess(() -> Component.literal("Say It English challenges are disabled server-wide."), true);
                return 1;
            }))
            .then(literal("translation")
                .then(literal("enable").executes(context -> setTranslationEnabled(context.getSource(), true)))
                .then(literal("disable").executes(context -> setTranslationEnabled(context.getSource(), false)))
                .then(literal("provider")
                    .then(literal("google").executes(context -> setTranslationProvider(context.getSource(), "google")))
                    .then(literal("libretranslate").executes(context -> setTranslationProvider(context.getSource(), "libretranslate")))))
            .then(argument("players", EntityArgument.players())
                .then(literal("enable")
                    .then(literal("sayit").executes(context -> setPlayers(context.getSource(),
                        EntityArgument.getPlayers(context, "players"), SayItCompatibilityManager.Mode.SAYIT, true)))
                    .then(literal("requiem").executes(context -> setPlayers(context.getSource(),
                        EntityArgument.getPlayers(context, "players"), SayItCompatibilityManager.Mode.REQUIEM, true))))
                .then(literal("disable")
                    .then(literal("sayit").executes(context -> setPlayers(context.getSource(),
                        EntityArgument.getPlayers(context, "players"), SayItCompatibilityManager.Mode.SAYIT, false)))
                    .then(literal("requiem").executes(context -> setPlayers(context.getSource(),
                        EntityArgument.getPlayers(context, "players"), SayItCompatibilityManager.Mode.REQUIEM, false))))
            );
    }

    private static int setTranslationEnabled(CommandSourceStack source, boolean enabled) {
        RequiemConfig.speech.translateBubbles = enabled;
        RequiemConfig.save();
        source.sendSuccess(() -> Component.literal("Requiem speech bubble translation "
            + (enabled ? "enabled." : "disabled.")), true);
        return 1;
    }

    private static int setTranslationProvider(CommandSourceStack source, String provider) {
        RequiemConfig.speech.translationProvider = provider;
        RequiemConfig.save();
        source.sendSuccess(() -> Component.literal("Speech translation provider set to " + provider + "."), true);
        return 1;
    }

    private static int setPlayers(CommandSourceStack source, Collection<ServerPlayer> players,
                                  SayItCompatibilityManager.Mode mode, boolean enabled) {
        for (ServerPlayer player : players) {
            SayItCompatibilityManager.setPlayerMode(player, mode, enabled);
        }
        String action = enabled ? "enabled" : "disabled";
        String modeName = mode == SayItCompatibilityManager.Mode.SAYIT ? "Say It English" : "Requiem speech bubbles";
        source.sendSuccess(() -> Component.literal(modeName + " " + action + " for " + players.size() + " player(s)."), true);
        return players.size();
    }
}
