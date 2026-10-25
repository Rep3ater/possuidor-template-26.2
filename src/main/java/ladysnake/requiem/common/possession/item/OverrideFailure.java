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
package ladysnake.requiem.common.possession.item;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.Level;

import java.util.Optional;

public record OverrideFailure(boolean strikethrough) implements InstancedItemOverride {
    private static final OverrideFailure defaultFormatting = new OverrideFailure(false);
    private static final OverrideFailure strikeThrough = new OverrideFailure(true);

    public static OverrideFailure get(boolean strikethrough) {
        return strikethrough ? strikeThrough : defaultFormatting;
    }

    @Override
    public boolean shortCircuits() {
        return false;
    }

    @Override
    public InteractionResult use(Player player, Mob possessedEntity, ItemStack heldStack, Level world, InteractionHand hand) {
        return InteractionResult.FAIL;
    }

    @Override
    public InteractionResult finishUsing(Player user, Mob possessedEntity, ItemStack heldStack, Level world, InteractionHand activeHand) {
        return InteractionResult.FAIL;
    }

    @Override
    public Optional<Component> tweakTooltip(Component baseTooltip) {
        if (this.strikethrough()) {
            return Optional.of(baseTooltip.copy().withStyle(ChatFormatting.STRIKETHROUGH));
        } else {
            return InstancedItemOverride.super.tweakTooltip(baseTooltip);
        }
    }
}
