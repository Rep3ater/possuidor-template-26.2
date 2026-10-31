package ladysnake.requiem.mixin.common.compat;

import ladysnake.requiem.common.SayItCompatibilityManager;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Optional Say It English hook; @Pseudo allows servers without that mod. */
@Pseudo
@Mixin(targets = "com.spicesfire.sayitenglish.ChallengeManager", remap = false)
public abstract class SayItChallengeMixin {
    @Inject(method = "start", at = @At("HEAD"), cancellable = true, require = 0, remap = false)
    private static void requiem$filterSayItChallenges(
        ServerPlayer player, @Coerce Object state, long now,
        String kind, String subject, String arg, int seconds, CallbackInfo ci
    ) {
        if (!SayItCompatibilityManager.shouldRunVanillaSayIt(player)) ci.cancel();
    }
}
