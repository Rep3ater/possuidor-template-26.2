package baritone.api.utils;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

public interface IEntityContext {
    LivingEntity entity();
    Level world();
    BlockPos feetPos();
}
