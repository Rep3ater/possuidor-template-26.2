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

import baritone.api.fakeplayer.FakeServerPlayerEntity;
import com.google.common.base.Preconditions;
import ladysnake.requiem.api.v1.event.requiem.PlayerShellEvents;
import ladysnake.requiem.api.v1.event.requiem.RemnantStateChangeCallback;
import ladysnake.requiem.api.v1.event.requiem.RemnantTypeChangeCallback;
import ladysnake.requiem.api.v1.remnant.PlayerSplitResult;
import ladysnake.requiem.api.v1.remnant.RemnantComponent;
import ladysnake.requiem.api.v1.remnant.RemnantState;
import ladysnake.requiem.api.v1.remnant.RemnantType;
import ladysnake.requiem.common.advancement.criterion.RequiemCriteria;
import ladysnake.requiem.common.entity.PlayerShellEntity;
import ladysnake.requiem.common.gamerule.RequiemGamerules;
import ladysnake.requiem.core.remnant.NullRemnantState;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.resources.ResourceLocation;

import java.util.Optional;

public final class RemnantComponentImpl implements RemnantComponent {
    public static final String ETHEREAL_TAG = "ethereal";

    private final Player player;

    private RemnantState state = NullRemnantState.INSTANCE;
    private RemnantType remnantType = RemnantTypes.MORTAL;
    private boolean splitting;
    private boolean chosen;

    @Override
    public boolean hasChosenRemnantType() {
        return this.chosen || this.remnantType.isDemon();
    }

    public boolean isSplitting() {
        return this.splitting;
    }

    public RemnantComponentImpl(Player player) {
        this.player = player;
    }

    @Override
    public void become(RemnantType type, boolean makeChoice) {
        if (makeChoice) {
            this.chosen = true;
            if (this.player instanceof ServerPlayer) {
                RequiemCriteria.MADE_REMNANT_CHOICE.handle((ServerPlayer) player, type);
            }
        }

        RemnantType oldType = this.remnantType;

        if (type == oldType) {
            return;
        }

        // Fake players cannot be remnants
        if (this.player instanceof ServerPlayer && this.player.getClass() != ServerPlayer.class) {
            return;
        }

        boolean wasSoul = this.isVagrant();
        RemnantState oldHandler = this.state;
        RemnantState handler = type.create(this.player);
        this.state.teardown(handler);
        this.state = handler;
        this.remnantType = type;
        this.state.setup(oldHandler);
        RemnantComponent.KEY.sync(this.player);
        RemnantTypeChangeCallback.EVENT.invoker().onRemnantTypeChange(player, oldType, type);
        this.fireRemnantStateChange(wasSoul, RemnantStateChangeCallback.Cause.TYPE_UPDATE);
    }

    @Override
    public RemnantType getRemnantType() {
        return this.remnantType;
    }

    @Override
    public boolean isIncorporeal() {
        return this.state.isIncorporeal();
    }

    @Override
    public boolean isVagrant() {
        return this.state.isVagrant();
    }

    @Override
    public boolean setVagrant(boolean vagrant) {
        return setVagrant(vagrant, RemnantStateChangeCallback.Cause.OTHER);
    }

    private boolean setVagrant(boolean vagrant, RemnantStateChangeCallback.Cause cause) {
        boolean soul = this.isVagrant();

        if (soul != vagrant) {
            if (this.state.setVagrant(vagrant)) {
                this.fireRemnantStateChange(soul, cause);
                return true;
            }
            return false;
        }
        return true;
    }

    private void fireRemnantStateChange(boolean wasSoul, RemnantStateChangeCallback.Cause cause) {
        boolean nowSoul = this.isVagrant();

        if (wasSoul != nowSoul) {
            RemnantStateChangeCallback.EVENT.invoker().onRemnantStateChange(
                this.player,
                this,
                cause == RemnantStateChangeCallback.Cause.OTHER && splitting
                    ? RemnantStateChangeCallback.Cause.DISSOCIATION
                    : cause
            );
        }
    }

    @Override
    public boolean canRegenerateBody() {
        return this.state.canRegenerateBody();
    }

    @Override
    public boolean canCurePossessed(LivingEntity body) {
        boolean noCure = this.player.level().getServer() != null && RequiemGamerules.noCure(this.player.level().getServer().getGameRules());
        return !noCure && this.state.canCurePossessed(body);
    }

    @Override
    public void curePossessed(LivingEntity body) {
        Preconditions.checkState(!this.player.level().isClientSide());
        if (this.canCurePossessed(body)) {
            this.state.curePossessed(body);
        }
    }

    @Override
    public boolean canDissociateFrom(Mob possessed) {
        return this.state.canDissociateFrom(possessed);
    }

    @Override
    public boolean canSplitPlayer(boolean ignoreGameConditions) {
        return !this.player.isRemoved()
            && this.state.canSplit(ignoreGameConditions)
            && !this.isVagrant()
            && (ignoreGameConditions || PlayerShellEvents.PRE_SPLIT.invoker().canSplit(this.player));
    }

    @Override
    public Optional<PlayerSplitResult> splitPlayer(boolean ignoreGameConditions) {
        if (this.player instanceof ServerPlayer sp && this.canSplitPlayer(ignoreGameConditions)) {
            try {
                this.splitting = true;
                return Optional.of(PlayerSplitter.doSplit(sp));
            } finally {
                this.splitting = false;
            }
        }

        return Optional.empty();
    }

    @Override
    public boolean merge(ServerPlayer shell) {
        if (shell instanceof PlayerShellEntity playerShell && PlayerShellEvents.PRE_MERGE.invoker().canMerge(
            this.player,
            playerShell,
            playerShell.getDisplayProfile()
        ) && this.setVagrant(
            false,
            RemnantStateChangeCallback.Cause.MERGE
        )) {
            // if we got a PlayerShellEntity here, we must have a ServerPlayer too
            PlayerSplitter.doMerge(playerShell, (ServerPlayer) this.player);
            return true;
        }
        return false;
    }

    @Override
    public void prepareRespawn(ServerPlayer original, boolean lossless) {
        try {
            this.splitting = ((RemnantComponentImpl) RemnantComponent.get(original)).splitting;
            this.state.prepareRespawn(original, lossless);
        } finally {
            this.splitting = false;
        }
    }

    @Override
    public void serverTick() {
        this.state.serverTick();
    }

    @Override
    public void writeSyncPacket(RegistryFriendlyByteBuf buf, ServerPlayer recipient) {
        buf.writeVarInt(RemnantTypes.getRawId(this.remnantType));
        buf.writeBoolean(this.isVagrant());
        buf.writeBoolean(this.chosen);
    }

    @Override
    public void applySyncPacket(RegistryFriendlyByteBuf buf) {
        int remnantId = buf.readVarInt();
        boolean soul = buf.readBoolean();
        this.chosen = buf.readBoolean();

        this.become(RemnantTypes.get(remnantId));
        this.setVagrant(soul);
    }

    @Override
    public void readFromNbt(CompoundTag compoundTag) {
        RemnantType remnantType = RemnantTypes.get(ResourceLocation.parse((compoundTag.contains("id", net.minecraft.nbt.Tag.TAG_STRING) ? compoundTag.getString("id") : RemnantState.NULL_STATE_ID)));
        this.chosen = compoundTag.getBoolean("chosen");
        this.become(remnantType);
        this.setVagrant(compoundTag.getBoolean(ETHEREAL_TAG));
    }

    @Override
    public void writeToNbt(CompoundTag compoundTag) {
        compoundTag.putString("id", RemnantTypes.getId(this.remnantType).toString());
        compoundTag.putBoolean(ETHEREAL_TAG, this.isVagrant());
        compoundTag.putBoolean("chosen", this.chosen);
    }

    @Override
    public void become(RemnantType type) {
        become(type, false);
    }
}
