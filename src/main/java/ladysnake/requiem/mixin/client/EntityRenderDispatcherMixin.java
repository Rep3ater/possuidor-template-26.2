package ladysnake.requiem.mixin.client;

import ladysnake.requiem.client.locki.LockiClientState;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Optional compatibility hook for versions that include the hitbox debug renderer. */
@Pseudo
@Mixin(targets = "net.minecraft.client.renderer.debug.EntityHitboxDebugRenderer")
public abstract class EntityRenderDispatcherMixin {
    @Inject(method = "showHitboxes", at = @At("HEAD"), cancellable = true, require = 0)
    private void requiem$hideLockiHitbox(Entity entity, float partialTicks, boolean isServerEntity, CallbackInfo ci) {
        if (entity != null && LockiClientState.isInvisibleMode(entity.getId())) ci.cancel();
    }
}
