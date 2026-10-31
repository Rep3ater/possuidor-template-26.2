package ladysnake.requiem.common.command;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;

import java.util.Collection;
import java.util.function.Consumer;
import java.util.function.Predicate;

import static net.minecraft.commands.Commands.literal;

public final class RequiemCommand {

    public static final String REQUIEM_ROOT_COMMAND = "requiem";

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(literal(REQUIEM_ROOT_COMMAND)
            .requires(RequiemCommand::checkPermissions)
            .then(RequiemEtherealCommand.etherealSubcommand())
            .then(RequiemPossessionCommand.possessionSubcommand())
            .then(RequiemRemnantCommand.remnantSubcommand())
            .then(RequiemShellCommand.shellSubcommand())
            .then(RequiemSoulCommand.soulSubcommand())
            .then(ladysnake.requiem.common.command.RequiemSayItCommand.sayItSubcommand())
        );
    }

    public static Predicate<CommandSourceStack> permission(String name) {
        return source -> source.hasPermission(2);
    }

    private static boolean checkPermissions(CommandSourceStack source) {
        return source.hasPermission(2);
    }

    public static <T> int runOne(T element, Consumer<T> action) {
        try {
            action.accept(element);
            return 1;
        } catch (CommandRuntimeException e) {
            throw e;
        }
    }

    public static <T> int runOne(CommandSourceStack source, T element, Consumer<T> action) {
        try {
            action.accept(element);
            return 1;
        } catch (CommandRuntimeException e) {
            source.sendFailure(e.getComponent());
            return 0;
        }
    }

    public static <T> int runMany(Collection<T> collection, Consumer<T> action) {
        int count = 0;
        for (T element : collection) {
            action.accept(element);
            count++;
        }
        return count;
    }

    public static <T> int runMany(CommandSourceStack source, Collection<T> collection, Consumer<T> action) {
        int count = 0;
        try {
            for (T element : collection) {
                action.accept(element);
                count++;
            }
        } catch (CommandRuntimeException e) {
            source.sendFailure(e.getComponent());
        }
        return count;
    }
}
