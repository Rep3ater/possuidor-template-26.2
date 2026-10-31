package ladysnake.requiem.mixin.client;

import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Invokes the texture lookup declared on EntityRenderer and inherited by LivingEntityRenderer. */
@Mixin(EntityRenderer.class)
public interface EntityRendererAccessor {
    @Invoker("getTextureLocation")
    ResourceLocation requiem$getTextureLocation(Entity entity);
}
