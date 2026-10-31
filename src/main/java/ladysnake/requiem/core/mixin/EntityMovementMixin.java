package ladysnake.requiem.core.mixin;

import ladysnake.requiem.api.v1.entity.MovementAlterer;
import ladysnake.requiem.common.ghost.ParmenidesRole;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class EntityMovementMixin {

    @Inject(method = "collide", at = @At("HEAD"), cancellable = true)
    private void requiem$noClipCollide(Vec3 movement, CallbackInfoReturnable<Vec3> cir) {
        if ((Object) this instanceof Player player) {
            try {
                boolean materializing = ParmenidesRole.isParmenides(player) && player.isShiftKeyDown();
                if (MovementAlterer.get(player).isNoClipping() && !materializing) {
                    cir.setReturnValue(movement);
                }
            } catch (Throwable ignored) {
            }
        }
    }
}
