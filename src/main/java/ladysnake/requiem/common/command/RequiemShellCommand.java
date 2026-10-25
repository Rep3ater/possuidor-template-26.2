/*
 * Requiem
 * Copyright (C) 2017-2023 Ladysnake
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses>.
 *
 * Linking this mod statically or dynamically with other
 * modules is making a combined work based on this mod.
 * Thus, the terms and conditions of the GNU General Public License cover the whole combination.
 *
 * In addition, as a special exception, the copyright holders of
 * this mod give you permission to combine this mod
 * with free software programs or libraries that are released under the GNU LGPL
 * and with code included in the standard release of Minecraft under All Rights Reserved (or
 * modified versions of such code, with unchanged license).
 * You may copy and distribute such a system following the terms of the GNU GPL for this mod
 * and the licenses of the other code concerned.
 *
 * Note that people who make modified versions of this mod are not obligated to grant
 * this special exception for their modified versions; it is their choice whether to do so.
 * The GNU General Public License gives permission to release a modified version without this exception;
 * this exception also makes it possible to release a modified version which carries forward this exception.
 */
package ladysnake.requiem.common.command;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import ladysnake.requiem.api.v1.remnant.RemnantComponent;
import ladysnake.requiem.common.entity.PlayerShellEntity;
import ladysnake.requiem.common.remnant.PlayerSplitter;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.server.players.NameAndId;

import net.minecraft.world.entity.Entity;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;

import java.util.Collection;

import static net.minecraft.commands.arguments.EntityArgument.*;
import static net.minecraft.commands.arguments.GameProfileArgument.gameProfile;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

public final class RequiemShellCommand {
    public static final String SHELL_SUBCOMMAND = "shell";
    private static final SimpleCommandExceptionType TOO_MANY_PLAYERS_EXCEPTION =
        new SimpleCommandExceptionType(Component.translatable("argument.player.toomany"));

    public static LiteralArgumentBuilder<CommandSourceStack> shellSubcommand() {
        return literal(SHELL_SUBCOMMAND)
            .then(literal("create")
                .executes(context -> RequiemCommand.runOne(context.getSource(), context.getSource().getPlayer(), player -> createShell(context.getSource().getPosition(), player)))
                // pandemonium shell create
                .then(argument("player", player())
                    // pandemonium shell create <player>
                    .executes(context -> RequiemCommand.runOne(context.getSource(), getPlayer(context, "player"), player -> createShell(context.getSource().getPosition(), player)))
                )
            )
            .then(literal("split")
                // pandemonium shell split
                .executes(context -> RequiemCommand.runOne(context.getSource(), context.getSource().getPlayer(), RequiemShellCommand::split))
                .then(argument("players", players())
                    // pandemonium shell split <players>
                    .executes(context -> RequiemCommand.runMany(context.getSource(), getPlayers(context, "players"), RequiemShellCommand::split))
                )
            )
            .then(literal("merge").then(argument("shell", entity())
                .executes(context -> {
                    Entity shell = getEntity(context, "shell");
                    return RequiemCommand.runOne(context.getSource(), context.getSource().getPlayer(), player -> merge(player, shell));
                }).then(argument("player", player()).executes(context -> {
                    Entity shell = getEntity(context, "shell");
                    return RequiemCommand.runOne(context.getSource(), getPlayer(context, "player"), player -> merge(player, shell));
                })))
            )
            .then(literal("identity").then(literal("set")
                    .then(argument("shells", entities()).then(argument("profile", gameProfile())
                        .executes(context -> {
                            Collection<NameAndId> profiles = GameProfileArgument.getGameProfiles(context, "profile");
                            if (profiles.size() > 1) throw TOO_MANY_PLAYERS_EXCEPTION.create();
                            NameAndId p = profiles.iterator().next();
                            GameProfile profile = new GameProfile(p.id(), p.name());
                            return RequiemCommand.runMany(context.getSource(), getEntities(context, "shells"), s -> setIdentity(s, profile));
                        })
                    ))
                )
            );
    }

    private static void merge(ServerPlayer player, Entity entity) {
        if (!(entity instanceof PlayerShellEntity shell)) {
            throw new CommandRuntimeException(Component.translatable("requiem:commands.shell.fail.not_shell"));
        }
        RemnantComponent.get(player).merge(shell);
        player.sendSystemMessage(Component.literal("§aReclaimed physical shell."));
    }

    private static void setIdentity(Entity shell, GameProfile profile) {
        if (!(shell instanceof PlayerShellEntity)) {
            throw new CommandRuntimeException(Component.translatable("requiem:commands.shell.fail.not_shell"));
        }
        ((PlayerShellEntity) shell).setDisplayProfile(profile);
    }

    private static void createShell(Vec3 position, ServerPlayer player) {
        PlayerShellEntity shell = PlayerSplitter.createShell(player);
        shell.setPos(position.x, position.y, position.z);
        player.level().addFreshEntity(shell);
        player.sendSystemMessage(Component.literal("§aCreated player shell at §e" + (int)position.x + ", " + (int)position.y + ", " + (int)position.z));
    }

    private static void split(ServerPlayer player) {
        RemnantComponent remnantComponent = RemnantComponent.get(player);
        if (remnantComponent.isVagrant()) {
            throw new CommandRuntimeException(Component.translatable("requiem:commands.shell.split.fail.vagrant", player.getDisplayName()));
        }
        if (!remnantComponent.getRemnantType().isDemon() && !player.isCreative() && (player.level().getServer() == null || !player.level().getServer().getPlayerList().isOp(new NameAndId(player.getGameProfile())))) {
            throw new CommandRuntimeException(Component.translatable("requiem:commands.shell.split.fail.mortal", player.getDisplayName()));
        }
        if (!remnantComponent.getRemnantType().isDemon()) {
            remnantComponent.become(ladysnake.requiem.common.remnant.RemnantTypes.REMNANT, true);
        }
        remnantComponent.splitPlayer(true);
        player.sendSystemMessage(Component.literal("§aSeparated soul from mortal shell."));
    }
}
