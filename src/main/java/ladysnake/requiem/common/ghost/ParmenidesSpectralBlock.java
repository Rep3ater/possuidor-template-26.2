package ladysnake.requiem.common.ghost;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A server-managed spectral glass platform. Its model is always visible, but collision is
 * returned only for the Shift-materialized Parmenides and its active counterpart projection.
 */
public final class ParmenidesSpectralBlock extends Block {
    public static final IntegerProperty FADE = IntegerProperty.create("fade", 0, 4);
    public static final BooleanProperty STEP = BooleanProperty.create("step");

    private static final VoxelShape FULL_COLLISION = Shapes.block();
    private static final VoxelShape LOWER_STEP = Shapes.box(0.0D, 0.0D, 0.0D, 1.0D, 0.5D, 1.0D);
    private static final VoxelShape UPPER_STEP = Shapes.box(0.0D, 0.5D, 0.0D, 1.0D, 1.0D, 1.0D);

    public ParmenidesSpectralBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FADE, 0).setValue(STEP, false));
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (!(context instanceof EntityCollisionContext entityContext)) return Shapes.empty();
        Entity entity = entityContext.getEntity();

        boolean canCollide;
        if (entity instanceof ParmenidesProjectionEntity projection) {
            canCollide = ParmenidesManager.canUseSpectralBlocks(projection);
        } else if (entity instanceof Player player) {
            canCollide = ParmenidesRole.isParmenides(player) && player.isShiftKeyDown();
        } else {
            canCollide = false;
        }
        if (!canCollide) return Shapes.empty();
        if (!state.getValue(STEP)) return FULL_COLLISION;

        double footOffset = entity.getY() - pos.getY();
        return footOffset < 0.5D ? LOWER_STEP : UPPER_STEP;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return FULL_COLLISION;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FADE, STEP);
    }
}
