package ladysnake.requiem.common.entity.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/** Marker effect whose amplifier encodes the visible corruption level for White Vision. */
public final class CorruptionMarkerStatusEffect extends MobEffect {
    public CorruptionMarkerStatusEffect(int color) {
        super(MobEffectCategory.HARMFUL, color);
    }
}
