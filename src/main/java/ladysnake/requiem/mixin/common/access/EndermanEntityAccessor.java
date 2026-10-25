package ladysnake.requiem.mixin.common.access;

import net.minecraft.world.entity.monster.EnderMan;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(EnderMan.class)
public interface EndermanEntityAccessor {
    @Invoker("teleport")
    boolean requiem$invokeTeleport();
}
