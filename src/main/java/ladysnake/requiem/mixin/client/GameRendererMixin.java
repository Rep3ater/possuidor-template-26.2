package ladysnake.requiem.mixin.client;

import ladysnake.requiem.api.v1.remnant.RemnantComponent;
import ladysnake.requiem.client.RequiemClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
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

    @Inject(method = "checkEntityPostEffect", at = @At("HEAD"), cancellable = true)
    private void requiem$checkEntityPostEffect(@Nullable Entity cameraEntity, CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && RemnantComponent.isIncorporeal(mc.player)) {
            this.setPostEffect(RequiemClient.SPECTRE_POST_EFFECT);
            ci.cancel();
        }
    }
}
