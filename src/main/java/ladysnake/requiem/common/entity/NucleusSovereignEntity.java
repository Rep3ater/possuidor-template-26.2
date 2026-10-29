package ladysnake.requiem.common.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.zombie.Husk;
import net.minecraft.world.level.Level;

/**
 * Nucleus' sovereign is a dedicated entity type, not a Warden and not Marbas' immortal infected homunculus.
 * Its current model uses the vanilla husk rig until a dedicated Suffering boss model is supplied.
 */
public final class NucleusSovereignEntity extends Husk {
    public NucleusSovereignEntity(EntityType<? extends NucleusSovereignEntity> type, Level level) {
        super(type, level);
    }

    @Override
    protected boolean isSunSensitive() {
        return false;
    }

    @Override
    protected boolean convertsInWater() {
        return false;
    }
}
