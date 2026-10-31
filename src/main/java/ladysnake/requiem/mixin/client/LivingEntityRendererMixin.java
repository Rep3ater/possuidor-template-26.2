package ladysnake.requiem.mixin.client;

import ladysnake.requiem.api.v1.remnant.RemnantComponent;
import ladysnake.requiem.common.entity.PlayerShellEntity;
import baritone.api.fakeplayer.ClientPlayerShellEntity;
import ladysnake.requiem.common.sharedbody.SharedBodyClientState;
import ladysnake.requiem.common.ghost.ParmenidesRole;
import ladysnake.requiem.client.render.OutOfBodyAlphaBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Legacy renderer hook for the 1.21.1 pipeline: ghost and shared-body entities use a translucent render type.
 */
@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin<T extends LivingEntity, M extends net.minecraft.client.model.EntityModel<T>> {
    private static final ThreadLocal<Boolean> REQUIEM$TRANSLUCENT_ENTITY = ThreadLocal.withInitial(() -> false);

    @Inject(method = "render", at = @At("HEAD"))
    private void requiem$setOutOfBodyAlpha(T entity, float entityYaw, float partialTick, PoseStack poseStack,
                                           MultiBufferSource buffer, int packedLight, org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
        REQUIEM$TRANSLUCENT_ENTITY.set(requiem$isOutOfBody(entity));
    }

    @Inject(method = "render", at = @At("RETURN"))
    private void requiem$resetOutOfBodyAlpha(T entity, float entityYaw, float partialTick, PoseStack poseStack,
                                             MultiBufferSource buffer, int packedLight, org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
        REQUIEM$TRANSLUCENT_ENTITY.remove();
    }

    @ModifyVariable(method = "render", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private MultiBufferSource requiem$applyOutOfBodyAlpha(MultiBufferSource buffers) {
        return new OutOfBodyAlphaBufferSource(buffers, () -> Boolean.TRUE.equals(REQUIEM$TRANSLUCENT_ENTITY.get()), 0.38F);
    }

    private boolean requiem$isOutOfBody(T entity) {
        return !(entity instanceof PlayerShellEntity) && !(entity instanceof ClientPlayerShellEntity)
                && entity instanceof net.minecraft.world.entity.player.Player player
                && (RemnantComponent.isIncorporeal(player)
                    || ladysnake.requiem.client.ParmenidesClientState.isParmenides(player)
                    || ladysnake.requiem.client.ParmenidesClientState.isOutOfBodyBody(player.getId()));
    }

    @Inject(method = "getRenderType", at = @At("HEAD"), cancellable = true)
    private void requiem$translucentSharedBody(T entity, boolean bodyVisible, boolean translucent, boolean glowing, CallbackInfoReturnable<RenderType> cir) {
        if (glowing) return;
        if (!(entity instanceof PlayerShellEntity) && !(entity instanceof ClientPlayerShellEntity)
                && (SharedBodyClientState.isControlled(entity.getId()) || RemnantComponent.isIncorporeal(entity)
                || (entity instanceof net.minecraft.world.entity.player.Player player
                    && ladysnake.requiem.client.ParmenidesClientState.isOutOfBodyBody(player.getId()))
                || (entity instanceof net.minecraft.world.entity.player.Player player
                    && ladysnake.requiem.client.ParmenidesClientState.isParmenides(player)))) {
            cir.setReturnValue(RenderType.entityTranslucentCull(
                    ((EntityRendererAccessor) (Object) this).requiem$getTextureLocation(entity)));
        }
    }
}
