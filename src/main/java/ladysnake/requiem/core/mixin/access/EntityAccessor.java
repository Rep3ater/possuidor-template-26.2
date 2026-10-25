package ladysnake.requiem.core.mixin.access;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Entity.class)
public interface EntityAccessor {
    @Invoker("collide")
    Vec3 requiem$invokeCollide(Vec3 movement);

    @Invoker("getInputVector")
    static Vec3 requiem$invokeGetInputVector(Vec3 movementInput, float speed, float yaw) {
        throw new IllegalStateException(movementInput + "" + speed + "" + yaw);
    }
}
