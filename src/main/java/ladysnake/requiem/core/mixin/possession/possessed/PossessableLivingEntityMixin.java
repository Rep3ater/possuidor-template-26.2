package ladysnake.requiem.core.mixin.possession.possessed;

import ladysnake.requiem.api.v1.possession.Possessable;
import ladysnake.requiem.client.ClientPossessionHelper;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class PossessableLivingEntityMixin {

    @Inject(method = "push", at = @At("HEAD"), cancellable = true)
    private void requiem$noPushPossessor(Entity entity, CallbackInfo ci) {
        if ((Object) this instanceof Possessable p && p.getPossessor() == entity) {
            ci.cancel();
        } else if (entity instanceof Possessable p && p.getPossessor() == (Object) this) {
            ci.cancel();
        }
    }

    @Inject(method = "doPush", at = @At("HEAD"), cancellable = true)
    private void requiem$noDoPushPossessor(Entity entity, CallbackInfo ci) {
        if ((Object) this instanceof Possessable p && p.getPossessor() == entity) {
            ci.cancel();
        } else if (entity instanceof Possessable p && p.getPossessor() == (Object) this) {
            ci.cancel();
        }
    }

    @Inject(method = "isPushable", at = @At("HEAD"), cancellable = true)
    private void requiem$isPushableWhenPossessed(CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this instanceof Possessable p && p.isBeingPossessed()) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "isPickable", at = @At("HEAD"), cancellable = true)
    private void requiem$isPickableWhenPossessed(CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this instanceof Possessable p && p.isBeingPossessed()) {
            LivingEntity self = (LivingEntity) (Object) this;
            if (self.level().isClientSide()) {
                if (ClientPossessionHelper.isLocalPlayerPossessor(p)) {
                    cir.setReturnValue(false);
                }
            }
        }
    }
}
