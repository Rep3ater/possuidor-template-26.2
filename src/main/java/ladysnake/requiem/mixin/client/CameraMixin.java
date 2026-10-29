package ladysnake.requiem.mixin.client;

import ladysnake.requiem.common.entity.effect.RequiemStatusEffects;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Ash-lung corruption makes Phenex's field of view narrow and pulse in/out with each coughing fit. */
@Mixin(Camera.class)
public abstract class CameraMixin {
    @Inject(method = "getFov", at = @At("RETURN"), cancellable = true, require = 0)
    private void requiem$ashLungFov(CallbackInfoReturnable<Float> cir) {
        Entity cameraEntity = ((Camera) (Object) this).getEntity();
        if (!(cameraEntity instanceof Player player)) return;
        var effect = player.getEffect(RequiemStatusEffects.ASH_LUNG);
        if (effect == null) return;

        int level = Mth.clamp(effect.getAmplifier() + 1, 1, 10);
        float pulse = (player.tickCount / 5) % 2 == 0 ? 1.055F : 0.945F;
        float narrowing = 1.0F - (0.045F + level * 0.009F);
        // A little extra narrowing while a cough is active gives the player a distinct fit cue.
        if (Minecraft.getInstance().level != null && player.getPersistentData().getLong("requiem:phenex_cough_until") > Minecraft.getInstance().level.getGameTime()) {
            narrowing -= 0.06F;
        }
        cir.setReturnValue(Mth.clamp(cir.getReturnValue() * narrowing * pulse, 25.0F, 130.0F));
    }
}
