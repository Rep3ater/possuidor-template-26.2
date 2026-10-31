package ladysnake.requiem.mixin.client;

import ladysnake.requiem.client.SpeechBubbleClientState;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Applies the Requiem talking gesture after vanilla has reset the player model's arm poses.
 */
@Mixin(PlayerModel.class)
public abstract class SpeechBubblePlayerModelMixin<T extends LivingEntity> {
    @Inject(method = "setupAnim", at = @At("TAIL"))
    private void requiem$animateSpeech(T entity, float limbSwing, float limbSwingAmount,
                                       float ageInTicks, float netHeadYaw, float headPitch,
                                       CallbackInfo ci) {
        if (!(entity instanceof Player player) || !SpeechBubbleClientState.isGesturing(player.getId())) {
            return;
        }

        PlayerModel<?> model = (PlayerModel<?>) (Object) this;
        float phase = ageInTicks * 0.72F;
        model.rightArm.xRot = -1.05F + Mth.sin(phase) * 0.42F;
        model.rightArm.yRot = -0.48F + Mth.cos(phase * 0.83F) * 0.48F;
        model.leftArm.xRot = -1.08F + Mth.cos(phase * 1.07F) * 0.38F;
        model.leftArm.yRot = 0.48F + Mth.sin(phase * 0.91F) * 0.44F;
        model.rightArm.zRot = Mth.sin(phase * 0.7F) * 0.12F;
        model.leftArm.zRot = Mth.cos(phase * 0.7F) * 0.12F;

        // PlayerModel's skin overlay is rendered from separate sleeve parts.
        // setupAnim normally copies the vanilla arm pose to them before this
        // mixin runs, so explicitly mirror our talking pose onto both sleeves.
        model.rightSleeve.xRot = model.rightArm.xRot;
        model.rightSleeve.yRot = model.rightArm.yRot;
        model.rightSleeve.zRot = model.rightArm.zRot;
        model.leftSleeve.xRot = model.leftArm.xRot;
        model.leftSleeve.yRot = model.leftArm.yRot;
        model.leftSleeve.zRot = model.leftArm.zRot;
    }
}
