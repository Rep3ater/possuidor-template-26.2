package ladysnake.requiem.common.command;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import ladysnake.requiem.Requiem;
import ladysnake.requiem.api.v1.remnant.RemnantType;
import ladysnake.requiem.common.RequiemRegistries;
import ladysnake.requiem.common.remnant.RemnantTypes;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;

public class RemnantArgumentType implements ArgumentType<RemnantType> {
    private static final Collection<String> EXAMPLES = Arrays.asList("requiem:mortal", "remnant");
    private static final SimpleCommandExceptionType BAD_IDENTIFIER = new SimpleCommandExceptionType(Component.translatable("argument.id.invalid"));
    public static final DynamicCommandExceptionType NOT_FOUND_EXCEPTION = new DynamicCommandExceptionType(o -> Component.translatable("requiem:remnant_type.not_found", o));

    public static RemnantArgumentType remnantType() {
        return new RemnantArgumentType();
    }

    public static RemnantType getRemnantType(CommandContext<CommandSourceStack> context, String name) throws CommandSyntaxException {
        return context.getArgument(name, RemnantType.class);
    }

    @Override
    public RemnantType parse(StringReader stringReader) throws CommandSyntaxException {
        try {
            return stringReader.readBoolean() ? RemnantTypes.REMNANT : RemnantTypes.MORTAL;
        } catch (CommandSyntaxException e) {
            ResourceLocation identifier = parseIdentifier(stringReader);
            return RequiemRegistries.REMNANT_STATES.getOptional(identifier).orElseThrow(() -> NOT_FOUND_EXCEPTION.create(identifier));
        }
    }

    private static ResourceLocation parseIdentifier(StringReader reader) throws CommandSyntaxException {
        int i = reader.getCursor();

        while(reader.canRead() && ResourceLocation.isAllowedInResourceLocation(reader.peek())) {
            reader.skip();
        }

        String string = reader.getString().substring(i, reader.getCursor());

        try {
            if (string.contains(":")) {
                return ResourceLocation.parse(string);
            } else {
                return ResourceLocation.fromNamespaceAndPath(Requiem.MOD_ID, string);
            }
        } catch (Exception var4) {
            reader.setCursor(i);
            throw BAD_IDENTIFIER.createWithContext(reader);
        }
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        if ("true".startsWith(builder.getRemaining().toLowerCase())) {
            builder.suggest("true");
        }
        if ("false".startsWith(builder.getRemaining().toLowerCase())) {
            builder.suggest("false");
        }
        return SharedSuggestionProvider.suggestResource(RequiemRegistries.REMNANT_STATES.keySet(), builder);
    }

    @Override
    public Collection<String> getExamples() {
        return EXAMPLES;
    }
}
