package ladysnake.requiem.mixin.client;

import ladysnake.requiem.client.marbas.MarbasRealityDegradationState;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Simulates vanilla chunk rendering glitches (MC-129 / MC-228420) for Marbas' brain damage:
 * sections return an empty compiled state, allowing the caves and terrain behind to be viewed,
 * or creating the pre-chunk void fall hallucination while keeping the sky visible.
 */
@Mixin(targets = "net.minecraft.client.renderer.chunk.SectionRenderDispatcher$RenderSection")
public abstract class RenderSectionMixin {
    @Shadow
    public abstract BlockPos getOrigin();

    @Inject(method = "getCompiled", at = @At("HEAD"), cancellable = true)
    private void requiem$glitchCompiledSection(CallbackInfoReturnable<SectionRenderDispatcher.CompiledSection> cir) {
        if (MarbasRealityDegradationState.shouldHideSection(this.getOrigin())) {
            cir.setReturnValue(SectionRenderDispatcher.CompiledSection.EMPTY);
        }
    }
}
