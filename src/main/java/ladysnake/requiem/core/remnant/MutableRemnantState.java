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
package ladysnake.requiem.core.remnant;

import net.minecraft.world.level.Level;


import io.github.ladysnake.pal.AbilitySource;
import io.github.ladysnake.pal.Pal;
import io.github.ladysnake.pal.VanillaAbilities;
import ladysnake.requiem.api.v1.entity.CurableEntityComponent;
import ladysnake.requiem.api.v1.entity.MovementAlterer;
import ladysnake.requiem.api.v1.event.requiem.CanCurePossessedCallback;
import ladysnake.requiem.api.v1.possession.PossessionComponent;
import ladysnake.requiem.api.v1.remnant.RemnantComponent;
import ladysnake.requiem.api.v1.remnant.RemnantState;
import ladysnake.requiem.api.v1.remnant.StickyStatusEffect;
import ladysnake.requiem.core.RequiemCore;
import ladysnake.requiem.core.movement.SerializableMovementConfig;
import ladysnake.requiem.core.tag.RequiemCoreTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.Nullable;

public abstract class MutableRemnantState implements RemnantState {

    public static final AbilitySource SOUL_STATE;

    static {
        AbilitySource src = null;
        try {
            src = Pal.getAbilitySource(RequiemCore.id("soul_state"), AbilitySource.FREE);
        } catch (Throwable t) {
            RequiemCore.LOGGER.error("CRITICAL ERROR initializing MutableRemnantState.SOUL_STATE", t);
            throw new ExceptionInInitializerError(t);
        }
        SOUL_STATE = src;
    }
    protected final Player player;
    private boolean ethereal;

    public MutableRemnantState(Player player) {
        this.player = player;
    }

    @Override
    public void setup(RemnantState oldHandler) {
        this.setVagrant(oldHandler.isVagrant());
    }

    @Override
    public void teardown(RemnantState newHandler) {
        this.updatePlayerState(false);
    }

    @Override
    public boolean isIncorporeal() {
        return this.isVagrant() && !PossessionComponent.get(this.player).isPossessionOngoing();
    }

    @Override
    public boolean isVagrant() {
        return this.ethereal;
    }

    @Override
    public boolean setVagrant(boolean vagrant) {
        this.ethereal = vagrant;
        this.updatePlayerState(vagrant);
        return true;
    }

    private void updatePlayerState(boolean vagrant) {
        SerializableMovementConfig config;
        boolean serverside = !this.player.level().isClientSide();
        boolean incorporeal = vagrant && !PossessionComponent.get(this.player).isPossessionOngoing();
        if (incorporeal) {
            config = SerializableMovementConfig.SOUL;
            if (serverside) {
                Pal.grantAbility(this.player, VanillaAbilities.INVULNERABLE, SOUL_STATE);
            }
        } else {
            config = null;
            if (serverside) {
                Pal.revokeAbility(this.player, VanillaAbilities.INVULNERABLE, SOUL_STATE);
            }
        }
        MovementAlterer.get(player).setConfig(config);
        RemnantComponent.KEY.sync(player);
    }

    @Override
    public boolean canDissociateFrom(Mob possessed) {
        return true;
    }

    @Override
    public void curePossessed(LivingEntity body) {
        if (!this.canCurePossessed(body)) {
            return;
        }
        if (this.canRegenerateBodyFrom(body)) {
            this.regenerateBody(body);
        } else {
            this.cureMob(body);
        }
    }

    protected abstract void regenerateBody(LivingEntity body);

    protected @Nullable Mob cureMob(LivingEntity body) {
        Mob cured = CurableEntityComponent.KEY.get(body).cure();

        if (cured != null) {
            PossessionComponent.get(this.player).startPossessing(cured);
        }

        return cured;
    }

    @Override
    public boolean canCurePossessed(LivingEntity body) {
        return CanCurePossessedCallback.EVENT.invoker().canCurePossessed(body).get();
    }

    @Override
    public boolean canRegenerateBody() {
        return true;
    }

    protected boolean canRegenerateBodyFrom(LivingEntity body) {
        return this.canRegenerateBody() && CurableEntityComponent.KEY.get(body).canBeAssimilated();
    }

    @Override
    public void prepareRespawn(ServerPlayer original, boolean lossless) {
        if (!lossless && !this.isVagrant()) {
            RemnantComponent.get(this.player).setVagrant(true);
            this.copyGlobalPos(original);

            if (original.isDeadOrDying()) {
                this.onRespawnAfterDeath();
            }
        }
        for (MobEffectInstance effect : original.getActiveEffects()) {
            if (StickyStatusEffect.shouldStick(effect, this.player)) {
                this.player.addEffect(new MobEffectInstance(effect));
            }
        }
    }

    protected abstract void onRespawnAfterDeath();

    protected void copyGlobalPos(ServerPlayer original) {
        ServerPlayer clone = (ServerPlayer) this.player;
        ServerLevel previousWorld = (ServerLevel) original.level();
        clone.setServerLevel(previousWorld);
        clone.copyPosition(original);
    }

    @Override
    public void serverTick() {
        // NO-OP
    }
}
