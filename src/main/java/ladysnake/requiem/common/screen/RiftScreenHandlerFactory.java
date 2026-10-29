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
import ladysnake.requiem.api.v1.remnant.RiftTracker;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.MenuProvider;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Predicate;

public final class RiftScreenHandlerFactory implements MenuProvider, IMenuProviderExtension {
    public static final StreamCodec<RegistryFriendlyByteBuf, ObeliskDescriptor> DESCRIPTOR_STREAM_CODEC = ByteBufCodecs.fromCodecWithRegistries(ObeliskDescriptor.CODEC);
    public static final StreamCodec<RegistryFriendlyByteBuf, Set<ObeliskDescriptor>> SET_STREAM_CODEC = ByteBufCodecs.collection(LinkedHashSet::new, DESCRIPTOR_STREAM_CODEC);

    private final ObeliskDescriptor source;
    private final Predicate<Player> useCheck;

    public RiftScreenHandlerFactory(ObeliskDescriptor source, Predicate<Player> useCheck) {
        this.source = source;
        this.useCheck = useCheck;
    }

    @Override
    public void writeClientSideData(AbstractContainerMenu menu, RegistryFriendlyByteBuf buf) {
        DESCRIPTOR_STREAM_CODEC.encode(buf, this.source);
        SET_STREAM_CODEC.encode(buf, new LinkedHashSet<>(((RiftScreenHandler) menu).getObeliskPositions()));
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("requiem:container.obelisk_rift");
    }

    @Override
    public AbstractContainerMenu createMenu(int syncId, Inventory inv, Player player) {
        return new RiftScreenHandler(RequiemScreenHandlers.RIFT_SCREEN_HANDLER, syncId, source, this.useCheck, RiftTracker.KEY.get(player).fetchKnownObelisks());
    }
}
