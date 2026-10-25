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

import dev.onyxstudios.cca.api.v3.component.ComponentKey;
import dev.onyxstudios.cca.api.v3.component.ComponentRegistry;
import dev.onyxstudios.cca.api.v3.component.tick.ServerTickingComponent;
import ladysnake.requiem.Requiem;
import ladysnake.requiem.api.v1.record.GlobalRecord;
import ladysnake.requiem.api.v1.record.GlobalRecordKeeper;
import ladysnake.requiem.common.RequiemRecordTypes;
import ladysnake.requiem.common.network.RequiemNetworking;
import ladysnake.requiem.core.util.NbtCompat;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Optional;
import java.util.UUID;

public final class PlayerBodyTracker implements ServerTickingComponent {
    public static final ComponentKey<PlayerBodyTracker> KEY = ComponentRegistry.getOrCreate(Requiem.id("body_tracker"), PlayerBodyTracker.class);

    public static PlayerBodyTracker get(Player player) {
        return KEY.get(player);
    }

    private final Player player;
    @Nullable
    private UUID anchorUuid;
    private float previousAnchorHealth = -1;

    public PlayerBodyTracker(Player player) {
        this.player = player;
    }

    @Override
    public void serverTick() {
        assert this.player instanceof ServerPlayer;

        this.getAnchor().ifPresentOrElse(
            this::updateBodyHealth,
            this::onBodyDisappeared
        );
    }

    public void setAnchor(GlobalRecord anchor) {
        this.anchorUuid = anchor.getUuid();
    }

    public Optional<GlobalRecord> getAnchor() {
        return Optional.ofNullable(this.anchorUuid).flatMap(GlobalRecordKeeper.get(this.player.level())::getRecord);
    }

    @Override
    public void writeToNbt(@Nonnull CompoundTag tag) {
        if (this.anchorUuid != null) {
            NbtCompat.putUuid(tag, "AnchorUuid", this.anchorUuid);
        }
    }

    @Override
    public void readFromNbt(@Nonnull CompoundTag tag) {
        if (NbtCompat.containsUuid(tag, "AnchorUuid")) {
            this.anchorUuid = NbtCompat.getUuid(tag, "AnchorUuid").orElse(null);
        }
    }

    private void updateBodyHealth(GlobalRecord anchor) {
        ServerPlayer player = (ServerPlayer) this.player;

        if (anchor.get(RequiemRecordTypes.ENTITY_REF).flatMap(ptr -> ptr.resolve(player.level().getServer())).orElse(null) instanceof LivingEntity anchorEntity) {
            float health = anchorEntity.getHealth();

            if (health < this.previousAnchorHealth) {
                RequiemNetworking.sendAnchorDamageMessage(player, false);
            }

            this.previousAnchorHealth = health;
        }
    }

    private void onBodyDisappeared() {
        if (this.previousAnchorHealth > 0) {
            RequiemNetworking.sendAnchorDamageMessage((ServerPlayer) this.player, true);
            this.previousAnchorHealth = -1;
        }
    }
}
