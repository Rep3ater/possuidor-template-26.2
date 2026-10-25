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
 */
package ladysnake.requiem.common.gamerule;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import ladysnake.requiem.Requiem;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.level.gamerules.GameRule;
import net.minecraft.world.level.gamerules.GameRuleCategory;
import net.minecraft.world.level.gamerules.GameRuleType;
import net.neoforged.neoforge.registries.RegisterEvent;
import net.neoforged.neoforge.server.command.EnumArgument;

public class RequiemGamerules {
    public static final GameRule<Boolean> SHOW_POSSESSOR_NAMETAG =
        createBoolean(GameRuleCategory.PLAYER, false);
    public static final GameRule<Boolean> NO_CURE =
        createBoolean(GameRuleCategory.PLAYER, false);
    public static final GameRule<StartingRemnantType> STARTING_SOUL_MODE =
        createEnum(GameRuleCategory.PLAYER, StartingRemnantType.class, StartingRemnantType.CHOOSE);
    public static final GameRule<PossessionKeepInventory> POSSESSION_KEEP_INVENTORY =
        createEnum(GameRuleCategory.PLAYER, PossessionKeepInventory.class, PossessionKeepInventory.NEVER);
    public static final GameRule<PossessionDetection> POSSESSION_DETECTION =
        createEnum(GameRuleCategory.MOBS, PossessionDetection.class, PossessionDetection.NORMAL);
    public static final GameRule<Boolean> DROP_VESSEL_ON_DISCONNECT =
        createBoolean(GameRuleCategory.PLAYER, false);

    public static void register(RegisterEvent.RegisterHelper<GameRule<?>> helper) {
        helper.register(Requiem.id("show_possessor_nametag"), SHOW_POSSESSOR_NAMETAG);
        helper.register(Requiem.id("disable_cure"), NO_CURE);
        helper.register(Requiem.id("starting_remnant_type"), STARTING_SOUL_MODE);
        helper.register(Requiem.id("possession_keep_inventory"), POSSESSION_KEEP_INVENTORY);
        helper.register(Requiem.id("possession_detection"), POSSESSION_DETECTION);
        helper.register(Requiem.id("drop_vessel_on_disconnect"), DROP_VESSEL_ON_DISCONNECT);
    }

    public static void init() {
        // Registered via RegisterEvent
    }

    private static GameRule<Boolean> createBoolean(GameRuleCategory category, boolean defaultValue) {
        return new GameRule<>(
            category,
            GameRuleType.BOOL,
            BoolArgumentType.bool(),
            (visitor, r) -> visitor.visitBoolean(r),
            Codec.BOOL,
            val -> val ? 1 : 0,
            defaultValue,
            FeatureFlagSet.of()
        );
    }

    private static <E extends Enum<E>> GameRule<E> createEnum(GameRuleCategory category, Class<E> enumClass, E defaultValue) {
        Codec<E> codec = Codec.STRING.xmap(s -> Enum.valueOf(enumClass, s.toUpperCase()), Enum::name);
        return new GameRule<>(
            category,
            GameRuleType.BOOL,
            EnumArgument.enumArgument(enumClass),
            (visitor, r) -> {},
            codec,
            Enum::ordinal,
            defaultValue,
            FeatureFlagSet.of()
        );
    }
}
