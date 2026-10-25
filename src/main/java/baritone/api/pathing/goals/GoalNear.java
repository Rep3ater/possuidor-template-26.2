package baritone.api.pathing.goals;

import net.minecraft.core.BlockPos;

public class GoalNear implements Goal {
    private final BlockPos pos;
    private final int range;
    public GoalNear(BlockPos pos, int range) {
        this.pos = pos;
        this.range = range;
    }
    @Override
    public boolean isInGoal(int x, int y, int z) {
        return pos.distManhattan(new BlockPos(x, y, z)) <= range;
    }
    @Override
    public double heuristic(int x, int y, int z) {
        return pos.distManhattan(new BlockPos(x, y, z));
    }
}