package ladysnake.requiem.client.render.entity;

import ladysnake.requiem.Requiem;
import ladysnake.requiem.client.model.ImmortalHomunculusModel;
import ladysnake.requiem.common.entity.ImmortalHomunculusEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public class ImmortalHomunculusEntityRenderer extends MobRenderer<ImmortalHomunculusEntity, ImmortalHomunculusModel> {
    private static final ResourceLocation TEXTURE = Requiem.id("textures/entity/immortal_homunculus.png");

    public ImmortalHomunculusEntityRenderer(EntityRendererProvider.Context context) {
        super(context, new ImmortalHomunculusModel(context.bakeLayer(ImmortalHomunculusModel.LAYER_LOCATION)), 0.4F);
    }

    @Override
    public ResourceLocation getTextureLocation(ImmortalHomunculusEntity entity) {
        return TEXTURE;
    }
}
