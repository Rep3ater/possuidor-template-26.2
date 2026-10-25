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

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record WispTrailParticleEffect(float red, float green, float blue, float redEvolution, float greenEvolution,
                                      float blueEvolution) implements ParticleOptions {
    public static final MapCodec<WispTrailParticleEffect> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        Codec.FLOAT.fieldOf("r").forGetter(WispTrailParticleEffect::red),
        Codec.FLOAT.fieldOf("g").forGetter(WispTrailParticleEffect::green),
        Codec.FLOAT.fieldOf("b").forGetter(WispTrailParticleEffect::blue),
        Codec.FLOAT.fieldOf("re").forGetter(WispTrailParticleEffect::redEvolution),
        Codec.FLOAT.fieldOf("ge").forGetter(WispTrailParticleEffect::greenEvolution),
        Codec.FLOAT.fieldOf("be").forGetter(WispTrailParticleEffect::blueEvolution)
    ).apply(instance, WispTrailParticleEffect::new));

    public static final StreamCodec<ByteBuf, WispTrailParticleEffect> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.FLOAT, WispTrailParticleEffect::red,
        ByteBufCodecs.FLOAT, WispTrailParticleEffect::green,
        ByteBufCodecs.FLOAT, WispTrailParticleEffect::blue,
        ByteBufCodecs.FLOAT, WispTrailParticleEffect::redEvolution,
        ByteBufCodecs.FLOAT, WispTrailParticleEffect::greenEvolution,
        ByteBufCodecs.FLOAT, WispTrailParticleEffect::blueEvolution,
        WispTrailParticleEffect::new
    );

    @Override
    public ParticleType<WispTrailParticleEffect> getType() {
        return RequiemParticleTypes.SOUL_TRAIL;
    }
}
