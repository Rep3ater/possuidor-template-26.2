package ladysnake.requiem.client.render.entity;

import ladysnake.requiem.common.construct.ConstructVoxel;
import ladysnake.requiem.common.construct.ConstructVoxelModel;
import net.minecraft.client.renderer.block.MovingBlockRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

import java.util.ArrayList;
import java.util.List;

public class RoboticConstructRenderState extends LivingEntityRenderState {
    public final List<VoxelRenderEntry> voxels = new ArrayList<>();
    public ConstructVoxelModel voxelModel;
    public int punchTicks = 0;

    public static class VoxelRenderEntry {
        public final ConstructVoxel voxel;
        public final MovingBlockRenderState movingBlockState;

        public VoxelRenderEntry(ConstructVoxel voxel, MovingBlockRenderState movingBlockState) {
            this.voxel = voxel;
            this.movingBlockState = movingBlockState;
        }
    }
}
