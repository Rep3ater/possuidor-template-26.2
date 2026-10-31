package ladysnake.requiem.mixin.client;

import ladysnake.requiem.client.locki.LockiClientState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Camera;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Adjusts the camera after vanilla has applied its normal first-/third-person transform. */
@Mixin(Camera.class)
public abstract class LockiCameraMixin {
    @Shadow protected abstract void setPosition(double x, double y, double z);

    @Inject(method = "setup(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/world/entity/Entity;ZZF)V", at = @At("TAIL"))
    private void requiem$lockiCamera(BlockGetter level, Entity focusedEntity, boolean thirdPerson,
                                     boolean inverseView, float partialTick, CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || !LockiClientState.isLockiModeActive()) return;

        if (LockiClientState.isRootDiving() && !mc.options.getCameraType().isFirstPerson()) {
            // Keep third-person follow distance, but place the camera at spirit-height rather than eye-height.
            double lowerBy = Math.max(0.0, mc.player.getEyeHeight() - 0.045);
            Vec3 current = ((Camera) (Object) this).getPosition();
            this.setPosition(current.x, current.y - lowerBy, current.z);
            return;
        }

        if ("apple".equals(LockiClientState.mode()) && LockiClientState.cameraMode() == 2
                && mc.getCameraEntity() instanceof Player holder) {
            Vec3 look = holder.getViewVector(1.0F).normalize();
            Vec3 right = new Vec3(-look.z, 0.0, look.x).normalize();
            if (holder.getMainArm() == HumanoidArm.LEFT) right = right.scale(-1.0);
            // A close first-person viewpoint just above the carrier's main hand.
            Vec3 hand = holder.getEyePosition()
                .add(look.scale(0.32))
                .add(right.scale(0.29))
                .add(0.0, -0.31, 0.0);
            this.setPosition(hand.x, hand.y, hand.z);
        }
    }
}
