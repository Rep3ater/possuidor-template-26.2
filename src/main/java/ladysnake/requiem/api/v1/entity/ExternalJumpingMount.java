/*
 * Requiem
 * Copyright (C) 2017-2023 Ladysnake
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; If not, see <https://www.gnu.org/licenses>.
 */
package ladysnake.requiem.api.v1.entity;

import dev.onyxstudios.cca.api.v3.component.Component;
import dev.onyxstudios.cca.api.v3.component.ComponentFactory;
import dev.onyxstudios.cca.api.v3.component.ComponentKey;
import dev.onyxstudios.cca.api.v3.component.ComponentRegistry;
import ladysnake.requiem.api.v1.internal.ApiInternals;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PlayerRideableJumping;
import net.minecraft.world.entity.player.Player;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import org.apiguardian.api.API;

/**
 * A fake {@link PlayerRideableJumping} that is not implemented by a rideable entity.
 *
 * <p>Can be attached to various entities as a component
 */
@API(status = API.Status.EXPERIMENTAL, since = "2.0.0")
public interface ExternalJumpingMount extends PlayerRideableJumping, Component {
    ComponentKey<ExternalJumpingMount> KEY = ComponentRegistry.getOrCreate(Identifier.fromNamespaceAndPath("requiem", "charged_jump"), ExternalJumpingMount.class);

    static <E extends LivingEntity> ComponentFactory<E, ExternalJumpingMount> simple(float baseJumpStrength, SoundEvent stepSound) {
        return ApiInternals.getExternalJumpingMountFactory().simple(baseJumpStrength, stepSound);
    }

    /**
     * @return {@code true} if the player should be able to initiate long jumps with this mount
     */
    default boolean canJump(Player playerEntity) {
        return canJump();
    }

    @Override
    boolean canJump();

    /**
     * Called when a player releases the jump key clientside
     */
    @Override
    void onPlayerJump(int strength);

    default void setJumpStrength(int strength) {
        onPlayerJump(strength);
    }

    /**
     * Called when the long jump starts serverside
     */
    @Override
    void handleStartJump(int height);

    default void startJumping(int height) {
        handleStartJump(height);
    }

    /**
     * Called when the long jump stops serverside
     */
    @Override
    void handleStopJump();

    default void stopJumping() {
        handleStopJump();
    }

    /**
     * Called every tick in {@link Mob#travel(Vec3)}.
     * Should start a jump if {@link #onPlayerJump(int)} has previously been called.
     */
    void attemptJump();
}
