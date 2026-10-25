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

public class RequiemEntityParticleEffect implements ParticleOptions {
    private final ParticleType<RequiemEntityParticleEffect> type;
    private final int targetEntityId;
    private final int sourceEntityId;

    public static MapCodec<RequiemEntityParticleEffect> codec(ParticleType<RequiemEntityParticleEffect> particleType) {
        return RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.INT.fieldOf("srcId").forGetter(RequiemEntityParticleEffect::getSourceEntityId),
            Codec.INT.fieldOf("destId").forGetter(RequiemEntityParticleEffect::getTargetEntityId)
        ).apply(instance, (src, dest) -> new RequiemEntityParticleEffect(particleType, src, dest)));
    }

    public static StreamCodec<ByteBuf, RequiemEntityParticleEffect> streamCodec(ParticleType<RequiemEntityParticleEffect> particleType) {
        return StreamCodec.composite(
            ByteBufCodecs.VAR_INT, RequiemEntityParticleEffect::getSourceEntityId,
            ByteBufCodecs.VAR_INT, RequiemEntityParticleEffect::getTargetEntityId,
            (src, dest) -> new RequiemEntityParticleEffect(particleType, src, dest)
        );
    }

    public RequiemEntityParticleEffect(ParticleType<RequiemEntityParticleEffect> type, int sourceEntityId, int targetEntityId) {
        this.type = type;
        this.sourceEntityId = sourceEntityId;
        this.targetEntityId = targetEntityId;
    }

    @Override
    public ParticleType<RequiemEntityParticleEffect> getType() {
        return this.type;
    }

    public int getTargetEntityId() {
        return targetEntityId;
    }

    public int getSourceEntityId() {
        return sourceEntityId;
    }
}
