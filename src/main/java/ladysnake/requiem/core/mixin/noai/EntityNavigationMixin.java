package ladysnake.requiem.core.mixin.noai;

import ladysnake.requiem.core.entity.ai.DisableableAiController;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PathNavigation.class)
public abstract class EntityNavigationMixin implements DisableableAiController {
    private boolean requiem$disabled;

    @Override
    public void requiem$setDisabled(boolean disabled) {
        this.requiem$disabled = disabled;
    }

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void tick(CallbackInfo ci) {
        if (this.requiem$disabled) {
            ci.cancel();
        }
    }
}
