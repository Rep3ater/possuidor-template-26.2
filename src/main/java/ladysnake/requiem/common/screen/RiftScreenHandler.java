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
package ladysnake.requiem.common.screen;

import ladysnake.requiem.api.v1.block.ObeliskDescriptor;
import ladysnake.requiem.api.v1.remnant.RemnantComponent;
import ladysnake.requiem.common.block.RequiemBlocks;
import ladysnake.requiem.common.block.obelisk.RiftRunestoneBlock;
import ladysnake.requiem.common.entity.effect.AttritionStatusEffect;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Set;
import java.util.function.Predicate;

public class RiftScreenHandler extends AbstractContainerMenu {
    private final ObeliskDescriptor source;
    private final Predicate<Player> canBeUsedBy;
    private final Set<ObeliskDescriptor> obelisks;

    public RiftScreenHandler(int syncId, ObeliskDescriptor source, Set<ObeliskDescriptor> obeliskPositions) {
        this(RequiemScreenHandlers.RIFT_SCREEN_HANDLER, syncId, source, p -> true, obeliskPositions);
    }

    public RiftScreenHandler(@Nullable MenuType<?> type, int syncId, ObeliskDescriptor source, Predicate<Player> canBeUsedBy, Set<ObeliskDescriptor> obeliskPositions) {
        super(type, syncId);
        this.source = source;
        this.canBeUsedBy = canBeUsedBy;
        this.obelisks = obeliskPositions;
    }

    public Collection<ObeliskDescriptor> getObelisks() {
        return obelisks;
    }

    public Set<ObeliskDescriptor> getObeliskPositions() {
        return obelisks;
    }

    public ObeliskDescriptor getSource() {
        return source;
    }

    @Override
    public boolean stillValid(Player player) {
        return RemnantComponent.isIncorporeal(player) && canBeUsedBy.test(player) && RequiemBlocks.RIFT_RUNE.canBeUsedByVagrant(player);
    }

    public void useRift(ServerPlayer player, ObeliskDescriptor target) {
        if (this.obelisks.contains(target) && !this.source.equals(target)) {
            RiftRunestoneBlock.findRespawnPosition(player.getType(), player.level(), target.pos()).ifPresent(respawnPosition -> {
                Vec3 towardsObelisk = target.center().subtract(respawnPosition).normalize();
                float yaw = (float) Mth.wrapDegrees(Mth.atan2(towardsObelisk.z, towardsObelisk.x) * 180.0F / (float)Math.PI - 90.0);
                player.teleportTo(respawnPosition.x, respawnPosition.y, respawnPosition.z);
                player.setYRot(yaw);
                AttritionStatusEffect.apply(player, 1, getAttritionLength(target.pos()));
                player.closeContainer();
            });
        }
    }

    private int getAttritionLength(BlockPos target) {
        double distanceTraveled = Math.sqrt(this.source.pos().distSqr(target));
        // Base penalty is 15mn of attrition
        int minLength = 20*60*15;
        double logBase = Math.log(Math.pow(10, 0.003));
        // 1000 blocks <=> 5 minutes of attrition
        int distancePenalty = (int) (Math.log(distanceTraveled) / logBase * 6);
        return minLength + distancePenalty;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }
}
