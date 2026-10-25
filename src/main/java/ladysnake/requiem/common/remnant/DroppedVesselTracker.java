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

import dev.onyxstudios.cca.api.v3.component.Component;
import dev.onyxstudios.cca.api.v3.component.ComponentKey;
import dev.onyxstudios.cca.api.v3.component.ComponentRegistry;
import ladysnake.requiem.Requiem;
import ladysnake.requiem.api.v1.possession.PossessionComponent;
import ladysnake.requiem.api.v1.record.GlobalRecord;
import ladysnake.requiem.api.v1.record.GlobalRecordKeeper;
import ladysnake.requiem.api.v1.remnant.PlayerSplitResult;
import ladysnake.requiem.api.v1.remnant.RemnantComponent;
import ladysnake.requiem.common.RequiemRecordTypes;
import ladysnake.requiem.common.entity.PlayerShellEntity;
import ladysnake.requiem.core.record.EntityPositionClerk;
import ladysnake.requiem.core.util.NbtCompat;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;

public class DroppedVesselTracker implements Component {
    public static final ComponentKey<DroppedVesselTracker> KEY = ComponentRegistry.getOrCreate(Requiem.id("dropped_vessel_tracker"), DroppedVesselTracker.class);

    @Contract(pure = true)
    public static DroppedVesselTracker get(ServerPlayer player) {
        return KEY.get(player);
    }

    private @Nullable UUID anchorUuid;
    private final Player player;

    public DroppedVesselTracker(Player player) {
        this.player = player;
    }

    public ServerPlayer dropVessel() {
        PossessionComponent possessionComponent = PossessionComponent.get(player);
        Mob host = possessionComponent.getHost();
        if (host != null) {
            possessionComponent.stopPossessing();
            this.anchorUuid = EntityPositionClerk.get(host).getOrCreateRecord().getUuid();
        } else {
            Optional<PlayerSplitResult> result = RemnantComponent.get(player).splitPlayer(true);

            if (result.isPresent()) {
                KEY.get(result.get().soul()).anchorUuid = EntityPositionClerk.get(result.get().shell()).getOrCreateRecord().getUuid();
                return result.get().soul();
            }
        }

        return (ServerPlayer) this.player;
    }

    public void tryMergeWithVessel() {
        this.getAnchor().flatMap(r -> r.get(RequiemRecordTypes.ENTITY_REF)).ifPresent(ptr -> {
            ServerPlayer player = (ServerPlayer) this.player;

            ptr.resolve(player.level().getServer()).ifPresentOrElse(
                body -> mergeWithVessel(player, body),
                () -> {
                    ServerLevel level = player.level().getServer() != null ? player.level().getServer().getLevel(ptr.world()) : null;
                    if (level != null) {
                        player.teleportTo(level, ptr.pos().x, ptr.pos().y, ptr.pos().z, java.util.Set.of(), player.getYRot(), player.getXRot(), true);
                    }
                }
            );
        });

        this.anchorUuid = null;
    }

    private void mergeWithVessel(ServerPlayer player, Entity body) {
        if (body.level() instanceof ServerLevel serverLevel) {
            player.teleportTo(serverLevel, body.getX(), body.getY(), body.getZ(), java.util.Set.of(), body.getYRot(), body.getXRot(), true);
        }

        if (body instanceof PlayerShellEntity shell) {
            RemnantComponent.get(player).merge(shell);
        } else if (body instanceof Mob mob) {
            PossessionComponent.get(player).startPossessing(mob);
        }
    }

    public Optional<GlobalRecord> getAnchor() {
        return Optional.ofNullable(this.anchorUuid).flatMap(GlobalRecordKeeper.get(this.player.level())::getRecord);
    }

    @Override
    public void readFromNbt(CompoundTag tag) {
        if (NbtCompat.containsUuid(tag, "record_id")) {
            this.anchorUuid = NbtCompat.getUuid(tag, "record_id").orElse(null);
        }
    }

    @Override
    public void writeToNbt(CompoundTag tag) {
        if (this.anchorUuid != null) {
            NbtCompat.putUuid(tag, "record_id", this.anchorUuid);
        }
    }
}
