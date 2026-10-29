package ladysnake.requiem.client.model;

import ladysnake.requiem.Requiem;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.model.geom.ModelLayerLocation;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import ladysnake.requiem.common.entity.ImmortalHomunculusEntity;

/** Blockbench suffering-form model port. Its supplied skin uses 64x64 UV coordinates. */
public final class ImmortalHomunculusModel extends EntityModel<ImmortalHomunculusEntity> {
    private final ModelPart root;
    public static final ModelLayerLocation LAYER_LOCATION =
        new ModelLayerLocation(Requiem.id("immortal_homunculus"), "main");

    private final ModelPart waist, head, body, leftArm, rightArm, leftLeg, rightLeg;

    public ImmortalHomunculusModel(ModelPart root) {
        super(RenderType::entityCutoutNoCull);
        this.root = root;
        this.waist = root.getChild("waist");
        this.head = this.waist.getChild("head");
        this.body = this.waist.getChild("body");
        this.leftArm = this.waist.getChild("leftArm");
        this.rightArm = this.waist.getChild("rightArm");
        this.leftLeg = root.getChild("leftLeg");
        this.rightLeg = root.getChild("rightLeg");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();
PartDefinition waist = partdefinition.addOrReplaceChild("waist", CubeListBuilder.create(), PartPose.offset(0.0F, 12.0F, 0.0F));

		PartDefinition head = waist.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F, new CubeDeformation(0.0F))
		.texOffs(0, 16).addBox(-8.0F, -13.0F, 0.0F, 16.0F, 13.0F, 0.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, -12.0F, 0.0F));

		PartDefinition bone = head.addOrReplaceChild("bone", CubeListBuilder.create().texOffs(4, 11).addBox(0.0F, -1.0F, -1.0F, 1.0F, 1.0F, 1.0F, new CubeDeformation(-0.2F))
		.texOffs(4, 11).addBox(4.0F, -1.0F, -1.0F, 1.0F, 1.0F, 1.0F, new CubeDeformation(-0.2F)), PartPose.offset(-2.5F, -4.0F, -3.25F));

		PartDefinition body = waist.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 29).addBox(-4.0F, 0.0F, -2.0F, 8.0F, 12.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, -12.0F, 0.0F));

		PartDefinition rightArm_r1 = body.addOrReplaceChild("rightArm_r1", CubeListBuilder.create().texOffs(32, 16).addBox(0.0F, -5.0F, -1.0F, 2.0F, 10.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-1.0F, 14.0F, 2.0F, -3.0107F, 0.0F, 0.0F));

		PartDefinition rightArm = waist.addOrReplaceChild("rightArm", CubeListBuilder.create().texOffs(24, 29).addBox(-3.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offset(-5.0F, -10.0F, 0.0F));

		PartDefinition leftArm = waist.addOrReplaceChild("leftArm", CubeListBuilder.create().texOffs(32, 0).addBox(-1.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offset(5.0F, -10.0F, 0.0F));

		PartDefinition rightLeg = partdefinition.addOrReplaceChild("rightLeg", CubeListBuilder.create().texOffs(40, 16).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offset(-1.9F, 12.0F, 0.0F));

		PartDefinition leftLeg = partdefinition.addOrReplaceChild("leftLeg", CubeListBuilder.create().texOffs(40, 32).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offset(1.9F, 12.0F, 0.0F));
        return LayerDefinition.create(meshdefinition, 64, 64);
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, int packedColor) {
        this.root.render(poseStack, buffer, packedLight, packedOverlay, packedColor);
    }

    @Override
    public void setupAnim(ImmortalHomunculusEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        float radians = (float) Math.PI / 180.0F;
        this.head.yRot = entity.getYRot() * radians;
        this.head.xRot = entity.getXRot() * radians;
        if (entity.isInWater()) {
            float phase = entity.tickCount * 0.18F;
            this.body.xRot = 1.1F;
            this.leftArm.xRot = -1.25F + net.minecraft.util.Mth.cos(phase) * 0.65F;
            this.rightArm.xRot = -1.25F + net.minecraft.util.Mth.cos(phase + (float) Math.PI) * 0.65F;
            this.leftLeg.xRot = net.minecraft.util.Mth.cos(phase + (float) Math.PI) * 0.7F;
            this.rightLeg.xRot = net.minecraft.util.Mth.cos(phase) * 0.7F;
            return;
        }
        this.body.xRot = 0.0F;
        float phase = entity.walkAnimation.position(1.0F) * 0.6662F;
        float swing = 1.4F * entity.walkAnimation.speed(1.0F);
        this.leftLeg.xRot = net.minecraft.util.Mth.cos(phase) * swing;
        this.rightLeg.xRot = net.minecraft.util.Mth.cos(phase + (float) Math.PI) * swing;
        this.leftArm.xRot = net.minecraft.util.Mth.cos(phase + (float) Math.PI) * swing * 0.7F;
        this.rightArm.xRot = net.minecraft.util.Mth.cos(phase) * swing * 0.7F;
    }
}