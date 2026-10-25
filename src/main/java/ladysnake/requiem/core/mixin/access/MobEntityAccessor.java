package ladysnake.requiem.core.mixin.access;

import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Mob.class)
public interface MobEntityAccessor {
    @Accessor("targetSelector")
    GoalSelector getTargetSelector();

    @Accessor("goalSelector")
    GoalSelector getGoalSelector();

    @Accessor("navigation")
    PathNavigation requiem$getNavigation();
}
