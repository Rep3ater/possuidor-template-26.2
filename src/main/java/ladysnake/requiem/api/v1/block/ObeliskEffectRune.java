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
package ladysnake.requiem.api.v1.block;

import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import org.apiguardian.api.API;

/**
 * An {@link ObeliskRune} that applies status effects to affected players.
 */
@API(status = API.Status.EXPERIMENTAL)
public interface ObeliskEffectRune extends ObeliskRune {
    Holder<MobEffect> getEffect();

    @Override
    default void applyEffect(ServerPlayer target, int runeLevel, int obeliskWidth) {
        int effectDuration = (9 + obeliskWidth * 2) * 20;
        target.addEffect(new MobEffectInstance(this.getEffect(), effectDuration, runeLevel - 1, true, false, true));
    }
}
