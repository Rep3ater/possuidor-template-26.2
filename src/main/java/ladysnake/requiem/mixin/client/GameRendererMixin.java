package ladysnake.requiem.mixin.client;

import ladysnake.requiem.api.v1.remnant.RemnantComponent;
import ladysnake.requiem.client.RequiemClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {

    @Shadow
    private void setPostEffect(Identifier id) {}

    @Shadow
    public abstract void clearPostEffect();

    @Inject(method = "bobView", at = @At("HEAD"), cancellable = true)
    private void requiem$disableAstralViewBobbing(CameraRenderState cameraState, PoseStack poseStack, CallbackInfo ci) {
        if (ladysnake.requiem.client.locki.LockiClientState.isSpiritVision()) ci.cancel();
    }

    @Inject(method = "checkEntityPostEffect", at = @At("HEAD"), cancellable = true)
    private void requiem$checkEntityPostEffect(@Nullable Entity cameraEntity, CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && mc.player.hasEffect(ladysnake.requiem.common.entity.effect.RequiemStatusEffects.NUCLEUS_TRANSITION)) {
            this.setPostEffect(RequiemClient.NUCLEUS_POST_EFFECT);
            ci.cancel();
        } else if (mc.player != null && ladysnake.requiem.client.locki.LockiClientState.isLockiModeActive()) {
            // Always replace/clear the effect while Locki owns the camera. Leaving this untouched
            // when desiredPostEffect() becomes null left the astral shader stuck on the corporeal body.
            Identifier lockiEffect = ladysnake.requiem.client.locki.LockiClientState.desiredPostEffect();
            this.setPostEffect(lockiEffect);
            ci.cancel();
        } else if (mc.player != null && RemnantComponent.isIncorporeal(mc.player)) {
            this.setPostEffect(RequiemClient.SPECTRE_POST_EFFECT);
            ci.cancel();
        } else if (mc.player != null) {
            // Clear a stale Locki shader immediately when the state packet returns to "none";
            // the regular client tick also clears it, but the renderer can run before that tick.
            Identifier current = mc.gameRenderer.currentPostEffect();
            if (RequiemClient.LOCKI_SPIRIT_POST_EFFECT.equals(current)
                    || RequiemClient.LOCKI_APPLE_POST_EFFECT.equals(current)
                    || RequiemClient.LOCKI_WHITE_VISION_POST_EFFECT.equals(current)
                    || RequiemClient.NUCLEUS_POST_EFFECT.equals(current)) {
                this.clearPostEffect();
                ci.cancel();
            }
        }
    }
}
