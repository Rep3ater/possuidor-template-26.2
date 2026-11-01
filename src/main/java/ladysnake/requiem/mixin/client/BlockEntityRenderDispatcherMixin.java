package ladysnake.requiem.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import ladysnake.requiem.client.marbas.MarbasRealityDegradationState;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Recreates vanilla ghost block glitch MC-244981: chests and block entities can visually
 * vanish into thin air while remaining interactable by Marbas.
 */
@Mixin(BlockEntityRenderDispatcher.class)
public abstract class BlockEntityRenderDispatcherMixin {
    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private <E extends BlockEntity> void requiem$hideGhostBlockEntity(
            E blockEntity, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, CallbackInfo ci) {
        if (blockEntity != null && MarbasRealityDegradationState.shouldHideBlock(blockEntity.getBlockPos())) {
            ci.cancel();
        }
    }
}
