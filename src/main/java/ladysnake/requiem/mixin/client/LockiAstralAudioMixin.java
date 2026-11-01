package ladysnake.requiem.mixin.client;

import ladysnake.requiem.client.locki.LockiClientState;
import ladysnake.requiem.client.marbas.MarbasRealityDegradationState;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Removes the player's footstep noise while inhabiting the astral plane,
 * and intercepts footsteps to reproduce the multiplayer high-latency delayed sound bug (MC-3250) for Marbas.
 */
@Mixin(Entity.class)
public abstract class LockiAstralAudioMixin {
    @Inject(method = "playStepSound", at = @At("HEAD"), cancellable = true)
    private void requiem$silenceAstralSteps(BlockPos pos, BlockState state, CallbackInfo ci) {
        Entity self = (Entity) (Object) this;
        if (self instanceof Player player && player == Minecraft.getInstance().player) {
            if (LockiClientState.isSpiritVision()) {
                ci.cancel();
                return;
            }
            if (MarbasRealityDegradationState.onPlayerFootstep(player, pos, state)) {
                ci.cancel();
            }
        }
    }
}
