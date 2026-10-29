package ladysnake.requiem.common.construct.block;

import ladysnake.requiem.common.construct.ConstructWorkshopManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

public class ConstructHeartBlock extends Block {

    public ConstructHeartBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide() && placer instanceof ServerPlayer player) {
            if (!level.dimension().equals(ConstructWorkshopManager.WORKSHOP_DIMENSION)) {
                ConstructWorkshopManager.enterWorkshop(player, pos, level.dimension(), false);
            }
        }
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            if (level.dimension().equals(ConstructWorkshopManager.WORKSHOP_DIMENSION)) {
                if (player.isShiftKeyDown()) {
                    serverPlayer.sendSystemMessage(Component.literal("§eAnalyzing mecha kinematics and awakening..."));
                    ConstructWorkshopManager.exitWorkshop(serverPlayer, pos, true);
                    return InteractionResult.SUCCESS;
                } else {
                    serverPlayer.sendSystemMessage(Component.literal("§6Construct Heart: §fShift-Right-Click to finalize & awaken."));
                    return InteractionResult.SUCCESS;
                }
            } else {
                ConstructWorkshopManager.enterWorkshop(serverPlayer, pos, level.dimension(), false);
                return InteractionResult.SUCCESS;
            }
        }
        return InteractionResult.SUCCESS;
    }
}
