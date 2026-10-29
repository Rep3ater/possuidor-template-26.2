package net.yourmod.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class ModCommands {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("homunculus")
            .then(Commands.literal("kill")
                .executes(ctx -> {
                    CommandSourceStack source = ctx.getSource();
                    Vec3 pos = source.getPosition();
                    AABB area = new AABB(pos.x - 10, pos.y - 10, pos.z - 10, pos.x + 10, pos.y + 10, pos.z + 10);

                    List<LivingEntity> entities = source.getLevel().getEntitiesOfClass(
                        LivingEntity.class, area, e -> e != source.getEntity()
                    );

                    if (!entities.isEmpty()) {
                        entities.get(0).hurt(source.getLevel().damageSources().generic(), 100.0F);
                        source.sendSuccess(() -> Component.literal("Alvo eliminado com sucesso."), false);
                    } else {
                        source.sendFailure(Component.literal("Nenhum alvo próximo para eliminar."));
                    }
                    return 1;
                })
            )
            .then(Commands.literal("pickup")
                .executes(ctx -> {
                    CommandSourceStack source = ctx.getSource();
                    Vec3 pos = source.getPosition();
                    AABB area = new AABB(pos.x - 5, pos.y - 5, pos.z - 5, pos.x + 5, pos.y + 5, pos.z + 5);

                    List<ItemEntity> items = source.getLevel().getEntitiesOfClass(ItemEntity.class, area);
                    source.sendSuccess(() -> Component.literal("Coletando " + items.size() + " itens próximos."), false);
                    return 1;
                })
            )
            .then(Commands.literal("goto")
                .then(Commands.argument("coords", StringArgumentType.greedyString())
                    .executes(ctx -> {
                        String raw = StringArgumentType.getString(ctx, "coords");
                        CommandSourceStack source = ctx.getSource();
                        String[] parts = raw.trim().split("\s+");

                        if (parts.length < 3) {
                            source.sendFailure(Component.literal("Erro: Coordenadas incompletas. Uso correto: goto <x> <y> <z>"));
                            return 0;
                        }

                        try {
                            double x = Double.parseDouble(parts[0]);
                            double y = Double.parseDouble(parts[1]);
                            double z = Double.parseDouble(parts[2]);
                            source.sendSuccess(() -> Component.literal("Deslocando para: " + x + ", " + y + ", " + z), false);
                        } catch (NumberFormatException e) {
                            source.sendFailure(Component.literal("Erro: As coordenadas devem conter apenas números válidos."));
                            return 0;
                        }
                        return 1;
                    })
                )
            )
        );
    }
}
