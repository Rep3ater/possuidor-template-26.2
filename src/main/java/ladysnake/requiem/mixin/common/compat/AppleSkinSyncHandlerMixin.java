/*
 * Requiem
 * Copyright (C) 2017-2023 Ladysnake
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package ladysnake.requiem.mixin.common.compat;

import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * AppleSkin's hunger synchronizer treats every ServerPlayer as a connected player.
 * NeoForge fake players (including Requiem's player shell) have no Netty channel,
 * so its hasChannel check throws while the fake player is ticking.
 *
 * Keep AppleSkin's normal synchronization for real players, but skip its listener
 * for fake players that cannot receive clientbound packets.
 */
@Pseudo
@Mixin(targets = "squeek.appleskin.network.SyncHandler", remap = false)
public abstract class AppleSkinSyncHandlerMixin {
    @Inject(
        method = "onLivingTickEvent(Lnet/neoforged/neoforge/event/tick/EntityTickEvent$Pre;)V",
        at = @At("HEAD"),
        cancellable = true,
        remap = false
    )
    private void requiem$skipFakePlayerSync(EntityTickEvent.Pre event, CallbackInfo ci) {
        if (event.getEntity() instanceof FakePlayer) {
            ci.cancel();
        }
    }
}
