package ladysnake.requiem.common.entity.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/** Visual-only perception effect used by Locki to see corruption. */
public final class WhiteVisionStatusEffect extends MobEffect {
    public WhiteVisionStatusEffect() {
        super(MobEffectCategory.BENEFICIAL, 0xF5E65C);
    }
}
