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
 */
package ladysnake.requiem.common.particle;

import com.mojang.serialization.MapCodec;
import io.netty.buffer.ByteBuf;
import ladysnake.requiem.Requiem;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.neoforge.registries.RegisterEvent;

public final class RequiemParticleTypes {
    public static final SimpleParticleType ATTRITION = new SimpleParticleType(false);
    public static final SimpleParticleType ATTUNED = new SimpleParticleType(false);
    public static final SimpleParticleType CURE = new SimpleParticleType(false);
    public static final ParticleType<RequiemEntityParticleEffect> ENTITY_DUST = new ParticleType<>(false) {
        @Override
        public MapCodec<RequiemEntityParticleEffect> codec() {
            return RequiemEntityParticleEffect.codec(this);
        }

        @Override
        public StreamCodec<? super ByteBuf, RequiemEntityParticleEffect> streamCodec() {
            return RequiemEntityParticleEffect.streamCodec(this);
        }
    };
    public static final SimpleParticleType GHOST = new SimpleParticleType(true);
    public static final SimpleParticleType OBELISK_SOUL = new SimpleParticleType(false);
    public static final SimpleParticleType PENANCE = new SimpleParticleType(false);
    public static final ParticleType<WispTrailParticleEffect> SOUL_TRAIL = new ParticleType<>(true) {
        @Override
        public MapCodec<WispTrailParticleEffect> codec() {
            return WispTrailParticleEffect.CODEC;
        }

        @Override
        public StreamCodec<? super ByteBuf, WispTrailParticleEffect> streamCodec() {
            return WispTrailParticleEffect.STREAM_CODEC;
        }
    };

    public static void register(RegisterEvent.RegisterHelper<ParticleType<?>> helper) {
        helper.register(Requiem.id("attrition"), ATTRITION);
        helper.register(Requiem.id("attuned"), ATTUNED);
        helper.register(Requiem.id("cure"), CURE);
        helper.register(Requiem.id("entity_dust"), ENTITY_DUST);
        helper.register(Requiem.id("ghost"), GHOST);
        helper.register(Requiem.id("obelisk_soul"), OBELISK_SOUL);
        helper.register(Requiem.id("penance"), PENANCE);
        helper.register(Requiem.id("soul_trail"), SOUL_TRAIL);
    }

    public static void init() {
        // Registered via RegisterEvent
    }
}
