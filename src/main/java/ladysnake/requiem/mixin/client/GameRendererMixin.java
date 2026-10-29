package ladysnake.requiem.mixin.client;

import ladysnake.requiem.api.v1.remnant.RemnantComponent;
import ladysnake.requiem.client.RequiemClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
    @Inject(method = "bobView", at = @At("HEAD"), cancellable = true)
    private void requiem$disableAstralViewBobbing(PoseStack poseStack, float partialTicks, CallbackInfo ci) {
        if (ladysnake.requiem.client.locki.LockiClientState.isSpiritVision()) ci.cancel();
    }

    @Inject(method = "checkEntityPostEffect", at = @At("HEAD"), cancellable = true)
    private void requiem$checkEntityPostEffect(@Nullable Entity cameraEntity, CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && mc.player.hasEffect(ladysnake.requiem.common.entity.effect.RequiemStatusEffects.NUCLEUS_TRANSITION)) {
            RequiemClient.applyPostEffect(RequiemClient.NUCLEUS_POST_EFFECT);
            ci.cancel();
        } else if (mc.player != null && ladysnake.requiem.client.locki.LockiClientState.isLockiModeActive()) {
            RequiemClient.applyPostEffect(ladysnake.requiem.client.locki.LockiClientState.desiredPostEffect());
            ci.cancel();
        } else if (mc.player != null && RemnantComponent.isIncorporeal(mc.player)) {
            RequiemClient.applyPostEffect(RequiemClient.SPECTRE_POST_EFFECT);
            ci.cancel();
        } else {
            ResourceLocation active = RequiemClient.activePostEffect();
            if (active != null && (active.equals(RequiemClient.LOCKI_SPIRIT_POST_EFFECT)
                    || active.equals(RequiemClient.LOCKI_APPLE_POST_EFFECT)
                    || active.equals(RequiemClient.LOCKI_WHITE_VISION_POST_EFFECT)
                    || active.equals(RequiemClient.NUCLEUS_POST_EFFECT)
                    || active.equals(RequiemClient.SPECTRE_POST_EFFECT))) {
                RequiemClient.applyPostEffect(null);
                ci.cancel();
            }
        }
    }
}
