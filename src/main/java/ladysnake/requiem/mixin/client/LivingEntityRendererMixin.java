package ladysnake.requiem.mixin.client;

import ladysnake.requiem.client.RequiemClient;
import ladysnake.requiem.common.entity.effect.RequiemStatusEffects;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.util.ARGB;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Renders mobs driven through the Shared Body effect with a variable opacity, so they never block the view of the
 * player controlling them.
 */
@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin {
    @SuppressWarnings({"rawtypes", "unchecked"})
    @Inject(method = "getRenderType", at = @At("HEAD"), cancellable = true)
    private void requiem$translucentSharedBody(LivingEntityRenderState state, boolean bodyVisible, boolean translucent, boolean glowing, CallbackInfoReturnable<RenderType> cir) {
        if (!glowing && (state.getRenderData(RequiemClient.SHARED_BODY_ALPHA) != null || state.getRenderData(RequiemClient.GHOST_ALPHA) != null)) {
            cir.setReturnValue(RenderTypes.entityTranslucentCullItemTarget(((LivingEntityRenderer) (Object) this).getTextureLocation(state)));
        }
    }

    @Inject(method = "getModelTint", at = @At("RETURN"), cancellable = true)
    private void requiem$fadeSharedBody(LivingEntityRenderState state, CallbackInfoReturnable<Integer> cir) {
        Float alpha = state.getRenderData(RequiemClient.GHOST_ALPHA);
        if (alpha == null) {
            alpha = state.getRenderData(RequiemClient.SHARED_BODY_ALPHA);
        }
        if (alpha != null) {
            cir.setReturnValue(ARGB.multiply(cir.getReturnValue(), ARGB.colorFromFloat(alpha, 1.0F, 1.0F, 1.0F)));
        }

        Integer rotStage = state.getRenderData(RequiemClient.LOCKI_ROT_STAGE);
        if (rotStage != null && rotStage > 0) {
            int tint = switch (Math.min(4, rotStage)) {
                case 1 -> ARGB.colorFromFloat(1.0F, 0.90F, 1.0F, 0.70F);
                case 2 -> ARGB.colorFromFloat(1.0F, 0.68F, 0.82F, 0.43F);
                case 3 -> ARGB.colorFromFloat(1.0F, 0.48F, 0.62F, 0.30F);
                default -> ARGB.colorFromFloat(1.0F, 0.30F, 0.43F, 0.24F);
            };
            cir.setReturnValue(ARGB.multiply(cir.getReturnValue(), tint));
        }

        // White Vision highlights Attrition, ash-lung disease, brain damage and homunculus
        // mental-health corruption with a gold silhouette; higher levels become more saturated.
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && mc.level != null && mc.player.hasEffect(RequiemStatusEffects.WHITE_VISION)) {
            Integer level = state.getRenderData(RequiemClient.CORRUPTION_LEVEL);
            if (level != null && level > 0) {
                float corruption = Math.min(10, level) / 10.0F;
                int goldTint = ARGB.colorFromFloat(1.0F, 1.0F, 0.96F - corruption * 0.50F, 0.73F - corruption * 0.55F);
                cir.setReturnValue(ARGB.multiply(cir.getReturnValue(), goldTint));
            }
        }
    }
}
