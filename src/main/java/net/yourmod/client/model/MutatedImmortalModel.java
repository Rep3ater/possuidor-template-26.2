package net.yourmod.client.model;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.resources.ResourceLocation;

public class MutatedImmortalModel extends EntityModel<LivingEntity> {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
        ResourceLocation.fromNamespaceAndPath("yourmodid", "mutated_immortal"), "main"
    );

    public MutatedImmortalModel(ModelPart root) {
        super(root);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();

        partdefinition.addOrReplaceChild("head", CubeListBuilder.create()
            .texOffs(0, 0)
            .addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F, new CubeDeformation(0.0F)), 
            PartPose.offset(0.0F, 0.0F, 0.0F)
        );

        partdefinition.addOrReplaceChild("body", CubeListBuilder.create()
            .texOffs(16, 16)
            .addBox(-4.0F, 0.0F, -2.0F, 8.0F, 12.0F, 4.0F, new CubeDeformation(0.0F)), 
            PartPose.offset(0.0F, 0.0F, 0.0F)
        );

        return LayerDefinition.create(meshdefinition, 64, 64);
    }

    @Override
    public void setupAnim(LivingEntity state) {
        // Lógica de animação
    }
}
