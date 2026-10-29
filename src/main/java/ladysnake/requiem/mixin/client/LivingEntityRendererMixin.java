package ladysnake.requiem.mixin.client;

import ladysnake.requiem.api.v1.remnant.RemnantComponent;
import ladysnake.requiem.common.sharedbody.SharedBodyClientState;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Legacy renderer hook for the 1.21.1 pipeline: ghost and shared-body entities use a translucent render type.
 */
@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin<T extends LivingEntity, M extends net.minecraft.client.model.EntityModel<T>> {
    @Shadow
    public abstract net.minecraft.resources.ResourceLocation getTextureLocation(T entity);

    @Inject(method = "getRenderType", at = @At("HEAD"), cancellable = true)
    private void requiem$translucentSharedBody(T entity, boolean bodyVisible, boolean translucent, boolean glowing, CallbackInfoReturnable<RenderType> cir) {
        if (glowing) return;
        if (SharedBodyClientState.isControlled(entity.getId()) || RemnantComponent.isIncorporeal(entity)) {
            cir.setReturnValue(RenderType.entityTranslucentCull(this.getTextureLocation(entity)));
        }
    }
}
