package ladysnake.requiem.mixin.client;

import ladysnake.requiem.client.ParmenidesClientState;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Suppresses the local dimension's complete block column inside the revealed domain.
 * The counterpart renderer then draws the other dimension in the same coordinates.
 */
@Mixin(BlockRenderDispatcher.class)
public abstract class BlockRenderDispatcherMixin {
    @Inject(method = "renderBatched", at = @At("HEAD"), cancellable = true)
    private void requiem$hideReplacedDimension(BlockState state, BlockPos pos, BlockAndTintGetter level,
                                               PoseStack poseStack, VertexConsumer consumer,
                                               boolean checkSides, RandomSource random, CallbackInfo ci) {
        if (ParmenidesClientState.shouldHideOriginalBlock(pos)) {
            ci.cancel();
        }
    }
}
