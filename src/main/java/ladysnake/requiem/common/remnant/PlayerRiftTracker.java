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
package ladysnake.requiem.common.remnant;

import com.google.common.base.Preconditions;
import ladysnake.requiem.api.v1.block.ObeliskDescriptor;
import ladysnake.requiem.api.v1.record.GlobalRecord;
import ladysnake.requiem.api.v1.record.GlobalRecordKeeper;
import ladysnake.requiem.api.v1.remnant.RiftTracker;
import ladysnake.requiem.common.RequiemRecordTypes;
import ladysnake.requiem.common.network.RequiemNetworking;
import net.minecraft.core.UUIDUtil;
import net.minecraft.world.entity.player.Player;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public class PlayerRiftTracker implements RiftTracker {
    private final Player player;
    private final Set<UUID> riftRecordUuids;

    public PlayerRiftTracker(Player player) {
        this.player = player;
        this.riftRecordUuids = new HashSet<>();
    }

    @Override
    public void addRift(GlobalRecord riftRecord) {
        Preconditions.checkArgument(riftRecord.has(RequiemRecordTypes.RIFT_OBELISK));
        Preconditions.checkArgument(riftRecord.has(RequiemRecordTypes.OBELISK_REF));

        if (this.riftRecordUuids.add(riftRecord.getUuid()) && this.player instanceof ServerPlayer sp) {
            Optional<Component> obeliskName = riftRecord.get(RequiemRecordTypes.OBELISK_REF).flatMap(ObeliskDescriptor::name);
            RequiemNetworking.sendRiftWitnessedMessage(sp, obeliskName.orElse(Component.empty()));
        }
    }

    @Override
    public Set<ObeliskDescriptor> fetchKnownObelisks() {
        GlobalRecordKeeper recordKeeper = GlobalRecordKeeper.get(this.player.level());
        Set<ObeliskDescriptor> ret = new HashSet<>();

        for (Iterator<UUID> iterator = this.riftRecordUuids.iterator(); iterator.hasNext(); ) {
            UUID uuid = iterator.next();
            Optional<ObeliskDescriptor> descriptor = recordKeeper.getRecord(uuid).filter(r -> r.get(RequiemRecordTypes.RIFT_OBELISK).isPresent()).flatMap(r -> r.get(RequiemRecordTypes.OBELISK_REF));
            if (descriptor.isEmpty()) {
                iterator.remove();
            } else {
                ret.add(descriptor.get());
            }
        }

        return ret;
    }

    @Override
    public void readFromNbt(CompoundTag tag) {
        this.riftRecordUuids.clear();
        net.minecraft.nbt.ListTag records = tag.getList("rift_records", net.minecraft.nbt.Tag.TAG_INT_ARRAY);
        for (int i = 0; i < records.size(); i++) {
            int[] uuid = records.getIntArray(i);
            if (uuid.length == 4) this.riftRecordUuids.add(UUIDUtil.uuidFromIntArray(uuid));
        }
    }

    @Override
    public void writeToNbt(CompoundTag tag) {
        net.minecraft.nbt.ListTag records = new net.minecraft.nbt.ListTag();
        for (java.util.UUID uuid : this.riftRecordUuids) {
            long most = uuid.getMostSignificantBits();
            long least = uuid.getLeastSignificantBits();
            records.add(new net.minecraft.nbt.IntArrayTag(new int[] {
                (int) (most >> 32), (int) most, (int) (least >> 32), (int) least
            }));
        }
        tag.put("rift_records", records);
    }
}
