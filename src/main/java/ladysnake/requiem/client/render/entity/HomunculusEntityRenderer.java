package ladysnake.requiem.client.render.entity;

import ladysnake.requiem.Requiem;
import ladysnake.requiem.client.model.HomunculusModel;
import ladysnake.requiem.common.entity.HomunculusEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public class HomunculusEntityRenderer extends MobRenderer<HomunculusEntity, HomunculusModel> {
    private static final ResourceLocation TEXTURE = Requiem.id("textures/entity/homunculus.png");

    public HomunculusEntityRenderer(EntityRendererProvider.Context context) {
        super(context, new HomunculusModel(context.bakeLayer(HomunculusModel.LAYER_LOCATION)), 0.35F);
    }

    @Override
    public ResourceLocation getTextureLocation(HomunculusEntity entity) {
        return TEXTURE;
    }
}
