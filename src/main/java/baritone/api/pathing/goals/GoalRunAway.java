package baritone.api.pathing.goals;

import net.minecraft.core.BlockPos;

public class GoalRunAway implements Goal {
    private final BlockPos pos;
    private final double distance;
    public GoalRunAway(double distance, BlockPos pos) {
        this.distance = distance;
        this.pos = pos;
    }
    @Override
    public boolean isInGoal(int x, int y, int z) {
        return pos.distSqr(new BlockPos(x, y, z)) >= distance * distance;
    }
    @Override
    public double heuristic(int x, int y, int z) {
        return -pos.distSqr(new BlockPos(x, y, z));
    }
}