package ladysnake.requiem.common.entity.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * Marker effect for the Potion of Shared Body. The actual puppeteering is handled by
 * {@link ladysnake.requiem.common.sharedbody.SharedBodyManager}, which links the afflicted mob to whoever splashed it.
 */
public class SharedBodyStatusEffect extends MobEffect {
    public SharedBodyStatusEffect(MobEffectCategory category, int color) {
        super(category, color);
    }
}
