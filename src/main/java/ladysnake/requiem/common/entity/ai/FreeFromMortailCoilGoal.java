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
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package ladysnake.requiem.common.entity.ai;

import ladysnake.requiem.api.v1.possession.PossessionComponent;
import ladysnake.requiem.common.entity.MorticianEntity;
import ladysnake.requiem.common.entity.effect.PenanceComponent;
import ladysnake.requiem.common.entity.effect.PenanceStatusEffect;
import ladysnake.requiem.common.entity.effect.RequiemStatusEffects;
import ladysnake.requiem.common.sound.RequiemSoundEvents;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;

public class FreeFromMortailCoilGoal extends MorticianSpellGoal {
    public static final int PENANCE_TIME = 10 * 20 * 60;

    public FreeFromMortailCoilGoal(MorticianEntity mortician) {
        super(mortician);
    }

    @Override
    protected SoundEvent getSoundPrepare() {
        return RequiemSoundEvents.ENTITY_MORTICIAN_PREPARE_ATTACK;
    }

    @Override
    protected int getCooldown() {
        return 100;
    }

    @Override
    protected int getWarmupTime() {
        return 20;
    }

    @Override
    protected void castSpell() {
        LivingEntity target = this.mortician.getTarget();
        if (target instanceof ServerPlayer player) {
            int penanceAmplifier = getRequiredPenance(player);
            player.addEffect(new MobEffectInstance(RequiemStatusEffects.PENANCE, PENANCE_TIME, penanceAmplifier, false, false, true));
            ServerPlayer soul = PenanceComponent.KEY.get(player).maxOutPenance(penanceAmplifier);
            ServerPlayer affected = soul != null ? soul : player;
            affected.knockback(1.0, (double) Mth.sin(this.mortician.getYRot() * (float) Math.PI / 180), (double) -Mth.cos(this.mortician.getYRot() * (float) Math.PI / 180));
            affected.connection.send(new ClientboundSetEntityMotionPacket(affected));
        }
    }

    private int getRequiredPenance(LivingEntity player) {
        if (PossessionComponent.getHost(player) != null) return PenanceStatusEffect.MOB_BAN_THRESHOLD;
        return PenanceStatusEffect.PLAYER_BAN_THRESHOLD;
    }
}
