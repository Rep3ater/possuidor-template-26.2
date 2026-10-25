package ladysnake.requiem.common.block.obelisk;

import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.level.block.state.BlockBehaviour;

import java.util.function.Supplier;

public class ReclamationRunestoneBlock extends RunestoneBlock {
    public ReclamationRunestoneBlock(BlockBehaviour.Properties settings, Supplier<Holder<MobEffect>> effect, int maxLevel) {
        super(settings, effect, maxLevel);
    }

    @Override
    public void applyEffect(ServerPlayer target, int runeLevel, int obeliskWidth) {
        Holder<MobEffect> effectHolder = this.getEffect();
        if (!target.hasEffect(effectHolder)) {
            int effectDuration = (10 - obeliskWidth) * 20 * 60;
            target.addEffect(new MobEffectInstance(effectHolder, effectDuration, runeLevel - 1, true, false, true));
        }
    }
}
