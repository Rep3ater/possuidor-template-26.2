package ladysnake.requiem.common.construct.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;

public class KeyTriggerBlock extends Block {
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
    private final String keyName;

    public KeyTriggerBlock(BlockBehaviour.Properties properties, String keyName) {
        super(properties);
        this.keyName = keyName;
        this.registerDefaultState(this.stateDefinition.any().setValue(POWERED, false));
    }

    public static KeyTriggerBlock newR(BlockBehaviour.Properties properties) {
        return new KeyTriggerBlock(properties, "R");
    }

    public static KeyTriggerBlock newV(BlockBehaviour.Properties properties) {
        return new KeyTriggerBlock(properties, "V");
    }

    public String getKeyName() {
        return keyName;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(POWERED);
    }

    @Override
    public boolean isSignalSource(BlockState state) {
        return true;
    }

    @Override
    public int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return state.getValue(POWERED) ? 15 : 0;
    }

    @Override
    public int getDirectSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return state.getValue(POWERED) ? 15 : 0;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide()) {
            pulse(level, pos, state);
        }
        return InteractionResult.SUCCESS;
    }

    public static void pulse(Level level, BlockPos pos, BlockState state) {
        level.setBlock(pos, state.setValue(POWERED, true), 3);
        level.updateNeighborsAt(pos, state.getBlock());
        level.scheduleTick(pos, state.getBlock(), 10);
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (state.getValue(POWERED)) {
            level.setBlock(pos, state.setValue(POWERED, false), 3);
            level.updateNeighborsAt(pos, this);
        }
    }
}
