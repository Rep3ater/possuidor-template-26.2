package baritone.api.pathing.goals;

import net.minecraft.core.BlockPos;

public interface Goal {
    boolean isInGoal(int x, int y, int z);
    double heuristic(int x, int y, int z);
    default boolean isInGoal(BlockPos pos) {
        return isInGoal(pos.getX(), pos.getY(), pos.getZ());
    }
}