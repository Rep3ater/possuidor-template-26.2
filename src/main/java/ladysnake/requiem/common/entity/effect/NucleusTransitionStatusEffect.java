package ladysnake.requiem.common.entity.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/** Short-lived marker for the yellow Nucleus arrival transition. */
public final class NucleusTransitionStatusEffect extends MobEffect {
    public NucleusTransitionStatusEffect() {
        super(MobEffectCategory.NEUTRAL, 0xE8D64F);
    }
}
