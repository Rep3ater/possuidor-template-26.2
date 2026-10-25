package ladysnake.requiem.common.block.obelisk;

import com.mojang.serialization.MapCodec;
import ladysnake.requiem.api.v1.event.minecraft.BlockReplacedCallback;
import ladysnake.requiem.common.block.RequiemBlockEntities;
import ladysnake.requiem.common.item.RequiemItems;
import ladysnake.requiem.common.sound.RequiemSoundEvents;
import ladysnake.requiem.common.tag.RequiemBlockTags;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

public class InertRunestoneBlock extends BaseEntityBlock {
    public static final MapCodec<InertRunestoneBlock> CODEC = simpleCodec(InertRunestoneBlock::new);
    public static final BooleanProperty ACTIVATED = BooleanProperty.create("activated");
    public static final BooleanProperty HEAD = BooleanProperty.create("head");

    public InertRunestoneBlock(BlockBehaviour.Properties settings) {
        super(settings);
        this.registerDefaultState(this.stateDefinition.any().setValue(ACTIVATED, false).setValue(HEAD, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    public static void registerCallbacks() {
        BlockReplacedCallback.EVENT.register((oldState, world, pos, newState, moved) -> {
            if (oldState.is(RequiemBlockTags.OBELISK_FRAME) != newState.is(RequiemBlockTags.OBELISK_FRAME)) {
                for (BlockPos checkedPos : BlockPos.betweenClosed(pos.getX() - 1, pos.getY() - 1, pos.getZ() - 1, pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1)) {
                    if (checkedPos.distManhattan(pos) > 1) {
                        BlockState checkedState = world.getBlockState(checkedPos);
                        if (checkedState.is(RequiemBlockTags.OBELISK_CORE)) {
                            world.scheduleTick(checkedPos, checkedState.getBlock(), 0);
                        }
                    }
                }
            }
        });
    }

    public static void tryActivateObelisk(ServerLevel world, BlockPos pos, boolean firstActivation) {
        ObeliskMatcher.findObeliskOrigin(world, pos).flatMap(origin -> ObeliskMatcher.matchObelisk(world, origin).result())
            .ifPresentOrElse(
                match -> {
                    if (toggleRune(world, pos, match)) {
                        if (firstActivation && match.origin().equals(pos)) {
                            world.playSound(null, pos, RequiemSoundEvents.BLOCK_OBELISK_ACTIVATE, SoundSource.BLOCKS, 1, 0.7F);
                        }
                        world.playSound(null, pos, RequiemSoundEvents.BLOCK_OBELISK_CHARGE, SoundSource.BLOCKS, 1, 0.6f);
                    }
                },
                () -> toggleRune(world, pos, null)
            );
    }

    private static boolean toggleRune(ServerLevel world, BlockPos runePos, @Nullable ObeliskMatch match) {
        BlockState blockState = world.getBlockState(runePos);
        if (blockState.getBlock() instanceof InertRunestoneBlock runestone) {
            return runestone.toggleRune(world, runePos, match, blockState);
        }
        return false;
    }

    protected boolean toggleRune(ServerLevel world, BlockPos runePos, @Nullable ObeliskMatch match, BlockState blockState) {
        boolean active = match != null;
        boolean head = active && match.origin().equals(runePos);
        boolean changed = (blockState.getValue(ACTIVATED) != active) || (blockState.getValue(HEAD) != head);
        if (changed) {
            world.setBlock(runePos, blockState.setValue(ACTIVATED, active).setValue(HEAD, head), Block.UPDATE_ALL);
            if (head) {
                world.getBlockEntity(runePos, RequiemBlockEntities.RUNIC_OBSIDIAN).ifPresent(controller -> controller.initialize(match));
            }
            return true;
        }
        return false;
    }

    @Override
    protected InteractionResult useItemOn(ItemStack itemStack, BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (itemStack.getItem() == RequiemItems.DEBUG_ITEM) {
            if (!world.isClientSide() && ObeliskMatcher.findObeliskOrigin(world, pos).map(world::getBlockEntity).orElse(null) instanceof RunestoneBlockEntity controller) {
                player.sendSystemMessage(Component.literal("Width: %d, Height: %d".formatted(controller.getCoreWidth(), controller.getCoreHeight())));
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    @Override
    protected void onPlace(BlockState state, Level world, BlockPos pos, BlockState oldState, boolean isMoving) {
        if (!oldState.is(RequiemBlockTags.OBELISK_CORE)) {
            world.scheduleTick(pos, this, 0);
        }
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState oldState, ServerLevel world, BlockPos pos, boolean isMoving) {
        if (this.shouldHaveBlockEntity(oldState)) {
            if (world.getBlockEntity(pos) instanceof RunestoneBlockEntity runestone) {
                runestone.onDestroyed();
            }
        }
    }

    protected boolean shouldHaveBlockEntity(BlockState state) {
        return state.getValue(ACTIVATED);
    }

    @Override
    protected void neighborChanged(BlockState state, Level world, BlockPos pos, Block block, @Nullable net.minecraft.world.level.redstone.Orientation orientation, boolean notify) {
        world.scheduleTick(pos, this, 0);
    }

    @Override
    protected void tick(BlockState state, ServerLevel world, BlockPos pos, RandomSource random) {
        tryActivateObelisk(world, pos, true);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(ACTIVATED).add(HEAD);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return shouldHaveBlockEntity(state) ? new RunestoneBlockEntity(pos, state) : null;
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level world, BlockState state, BlockEntityType<T> type) {
        if (world.isClientSide() || !state.getValue(HEAD)) return null;
        return createTickerHelper(type, RequiemBlockEntities.RUNIC_OBSIDIAN, RunestoneBlockEntity::tick);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }
}
