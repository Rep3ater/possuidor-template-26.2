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
package ladysnake.requiem.api.v1.remnant;

import ladysnake.requiem.api.v1.possession.PossessionComponent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.server.level.ServerPlayer;
import org.apiguardian.api.API;
import org.jetbrains.annotations.Contract;

public interface RemnantState {
    String NULL_STATE_ID = "requiem:mortal";

    void setup(RemnantState oldHandler);

    void teardown(RemnantState newHandler);

    /**
     * Return whether this player is currently incorporeal.
     *
     * <p>A player is considered incorporeal if they have neither their natural body or a surrogate one.
     * If this method returns {@code true}, the player is also {@link #isVagrant() vagrant}.
     *
     * @return true if the player is currently incorporeal, {@code false} otherwise
     * @see RemnantComponent#isIncorporeal()
     */
    boolean isIncorporeal();

    /**
     * Return whether this player is currently dissociated from a natural player body.
     *
     * <p>Vagrant players are invulnerable and can only interact with the world through a proxy body.
     * Being vagrant is a prerequisite to being {@linkplain #isIncorporeal() incorporeal} or to
     * {@linkplain PossessionComponent#startPossessing(Mob) start possessing entities}.
     *
     * @see RemnantComponent#isVagrant()
     */
    @Contract(pure = true)
    boolean isVagrant();

    /**
     * Set whether this player is currently dissociated from a congruous body.
     *
     * <p>This operation may fail if this state does not support the given state
     *
     * @param vagrant {@code true} to mark this player as outside a congruous body, {@code false} to mark a merged state
     * @return {@code true} if the operation succeeded, {@code false} otherwise
     * @see #isVagrant()
     * @see #isIncorporeal()
     * @see RemnantComponent#setVagrant(boolean)
     */
    boolean setVagrant(boolean vagrant);

    /**
     * @see RemnantComponent#canDissociateFrom(Mob)
     */
    boolean canDissociateFrom(Mob possessed);

    /**
     * @see RemnantComponent#curePossessed(LivingEntity)
     */
    default void curePossessed(LivingEntity body) {
        // NO-OP
    }

    @API(status = API.Status.EXPERIMENTAL)
    default boolean canCurePossessed(LivingEntity body) {
        return false;
    }

    @API(status = API.Status.EXPERIMENTAL)
    default boolean canRegenerateBody() {
        return true;
    }

    default boolean canSplit(boolean forced) {
        return false;
    }

    /**
     * Called when this remnant state's player is cloned
     *
     * @param original the player's clone
     * @param lossless false if the original player is dead, true otherwise
     * @see RemnantComponent#prepareRespawn(ServerPlayer, boolean)
     */
    void prepareRespawn(ServerPlayer original, boolean lossless);

    void serverTick();
}

