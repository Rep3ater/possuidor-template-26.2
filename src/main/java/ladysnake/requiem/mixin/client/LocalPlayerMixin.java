package ladysnake.requiem.mixin.client;

import ladysnake.requiem.api.v1.entity.MovementAlterer;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LocalPlayer.class)
public abstract class LocalPlayerMixin {

    @Inject(method = "moveTowardsClosestSpace", at = @At("HEAD"), cancellable = true)
    private void requiem$noClipClosestSpace(double x, double z, CallbackInfo ci) {
        try {
            if (MovementAlterer.get((LocalPlayer) (Object) this).isNoClipping()) {
                ci.cancel();
            }
        } catch (Throwable ignored) {
        }
    }
}
