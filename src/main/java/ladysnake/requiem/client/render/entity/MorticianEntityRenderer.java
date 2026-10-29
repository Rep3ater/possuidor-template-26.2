package ladysnake.requiem.client.render.entity;

import ladysnake.requiem.Requiem;
import ladysnake.requiem.common.entity.MorticianEntity;
import net.minecraft.client.model.VillagerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public class MorticianEntityRenderer extends MobRenderer<MorticianEntity, VillagerModel<MorticianEntity>> {
    private static final ResourceLocation TEXTURE = Requiem.id("textures/entity/mortician.png");

    public MorticianEntityRenderer(EntityRendererProvider.Context context) {
        super(context, new VillagerModel<>(context.bakeLayer(ModelLayers.VILLAGER)), 0.5F);
    }

    @Override
    public ResourceLocation getTextureLocation(MorticianEntity entity) {
        return TEXTURE;
    }
}
