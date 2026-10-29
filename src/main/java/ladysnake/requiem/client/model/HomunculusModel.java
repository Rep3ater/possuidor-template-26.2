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
import ladysnake.requiem.common.entity.HomunculusEntity;

/** Blockbench model port with the supplied 128x128 texture and animated limbs. */
public final class HomunculusModel extends EntityModel<HomunculusEntity> {
    private final ModelPart root;
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(Requiem.id("homunculus"), "main");
    private final ModelPart homunculus, head, body, leftArm, rightArm, leftLeg, rightLeg;

    public HomunculusModel(ModelPart root) {
        super(RenderType::entityCutoutNoCull);
        this.root = root;
        this.homunculus = root.getChild("homunculus");
        this.head = this.homunculus.getChild("head");
        this.body = this.homunculus.getChild("body");
        this.leftArm = this.homunculus.getChild("leftArm");
        this.rightArm = this.homunculus.getChild("rightArm");
        this.leftLeg = this.homunculus.getChild("leftLeg");
        this.rightLeg = this.homunculus.getChild("rightLeg");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();
PartDefinition homunculus = partdefinition.addOrReplaceChild("homunculus", CubeListBuilder.create(), PartPose.offset(0.0F, 17.0F, 0.0F));

		PartDefinition head = homunculus.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0).addBox(-3.0F, -4.0F, -2.0F, 5.0F, 5.0F, 3.0F, new CubeDeformation(0.0F))
		.texOffs(12, 27).addBox(-2.0F, -5.0F, -1.0F, 3.0F, 1.0F, 2.0F, new CubeDeformation(0.0F))
		.texOffs(16, 12).addBox(-2.0F, -0.5F, -6.0F, 3.0F, 1.0F, 4.0F, new CubeDeformation(0.0F))
		.texOffs(26, 37).addBox(1.0F, -3.0F, -3.0F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.5F))
		.texOffs(30, 37).addBox(-3.0F, -3.0F, -3.0F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.7F)), PartPose.offset(1.0F, -6.0F, 0.0F));

		PartDefinition cube_r1 = head.addOrReplaceChild("cube_r1", CubeListBuilder.create().texOffs(0, 22).addBox(-2.0F, -1.0F, -1.0F, 3.0F, 1.0F, 4.0F, new CubeDeformation(-0.3F)), PartPose.offsetAndRotation(0.0F, 2.0F, -5.0F, 0.6545F, 0.0F, 0.0F));

		PartDefinition cube_r2 = head.addOrReplaceChild("cube_r2", CubeListBuilder.create().texOffs(18, 19).addBox(-1.0F, -2.0F, -1.0F, 3.0F, 1.0F, 2.0F, new CubeDeformation(0.3F)), PartPose.offsetAndRotation(-1.0F, 2.5F, -2.0F, 0.6545F, 0.0F, 0.0F));

		PartDefinition cube_r3 = head.addOrReplaceChild("cube_r3", CubeListBuilder.create().texOffs(34, 25).addBox(-1.5F, -2.0F, -1.0F, 2.0F, 1.0F, 1.0F, new CubeDeformation(0.2F)), PartPose.offsetAndRotation(0.0F, 0.0F, -4.0F, -0.3491F, 0.0F, 0.0F));

		PartDefinition cube_r4 = head.addOrReplaceChild("cube_r4", CubeListBuilder.create().texOffs(22, 27).addBox(0.0F, -2.0F, -2.0F, 1.0F, 1.0F, 3.0F, new CubeDeformation(0.3F)), PartPose.offsetAndRotation(-1.0F, 0.5F, -2.0F, -0.1309F, 0.0F, 0.0F));

		PartDefinition body = homunculus.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 8).addBox(-2.0F, -1.0F, -2.0F, 5.0F, 4.0F, 3.0F, new CubeDeformation(0.5F))
		.texOffs(0, 15).addBox(-2.0F, 3.0F, -2.0F, 5.0F, 4.0F, 3.0F, new CubeDeformation(-0.5F)), PartPose.offset(0.0F, -4.0F, 2.0F));

		PartDefinition leftArm = homunculus.addOrReplaceChild("leftArm", CubeListBuilder.create().texOffs(30, 2).addBox(2.0F, 2.0F, -6.0F, 1.0F, 1.0F, 3.0F, new CubeDeformation(0.3F))
		.texOffs(10, 37).addBox(3.0F, 3.0F, -3.0F, 1.0F, 2.0F, 1.0F, new CubeDeformation(0.2F)), PartPose.offset(3.0F, -4.0F, 2.0F));

		PartDefinition cube_r5 = leftArm.addOrReplaceChild("cube_r5", CubeListBuilder.create().texOffs(30, 0).addBox(-1.0F, -1.0F, -1.0F, 4.0F, 1.0F, 1.0F, new CubeDeformation(-0.2F)), PartPose.offsetAndRotation(2.5F, 4.0F, -3.0F, -3.1416F, -1.0472F, -2.3126F));

		PartDefinition cube_r6 = leftArm.addOrReplaceChild("cube_r6", CubeListBuilder.create().texOffs(30, 10).addBox(0.0F, -3.0F, -2.0F, 1.0F, 1.0F, 3.0F, new CubeDeformation(-0.2F)), PartPose.offsetAndRotation(3.5F, 7.0F, -1.0F, 0.829F, 0.0F, 0.0F));

		PartDefinition cube_r7 = leftArm.addOrReplaceChild("cube_r7", CubeListBuilder.create().texOffs(10, 30).addBox(0.0F, -3.0F, -2.0F, 1.0F, 1.0F, 3.0F, new CubeDeformation(-0.2F)), PartPose.offsetAndRotation(3.5F, 7.0F, -2.0F, 0.6545F, 0.0F, 0.0F));

		PartDefinition cube_r8 = leftArm.addOrReplaceChild("cube_r8", CubeListBuilder.create().texOffs(30, 6).addBox(0.0F, -3.0F, -2.0F, 1.0F, 1.0F, 3.0F, new CubeDeformation(-0.2F)), PartPose.offsetAndRotation(3.5F, 6.0F, -2.0F, 0.5236F, 0.0F, 0.0F));

		PartDefinition cube_r9 = leftArm.addOrReplaceChild("cube_r9", CubeListBuilder.create().texOffs(8, 34).addBox(0.0F, -3.0F, -3.0F, 1.0F, 1.0F, 2.0F, new CubeDeformation(-0.2F)), PartPose.offsetAndRotation(1.8F, 8.4F, -5.0F, 1.5708F, -1.3963F, -1.5708F));

		PartDefinition cube_r10 = leftArm.addOrReplaceChild("cube_r10", CubeListBuilder.create().texOffs(18, 33).addBox(0.0F, -3.0F, -3.0F, 1.0F, 1.0F, 2.0F, new CubeDeformation(-0.2F)), PartPose.offsetAndRotation(1.3F, 9.0F, -4.0F, 1.5708F, -1.3963F, -1.5708F));

		PartDefinition cube_r11 = leftArm.addOrReplaceChild("cube_r11", CubeListBuilder.create().texOffs(32, 31).addBox(0.0F, -3.0F, -3.0F, 1.0F, 1.0F, 2.0F, new CubeDeformation(-0.2F)), PartPose.offsetAndRotation(1.8F, 7.3F, -5.0F, 1.5708F, -1.3963F, -1.5708F));

		PartDefinition cube_r12 = leftArm.addOrReplaceChild("cube_r12", CubeListBuilder.create().texOffs(26, 31).addBox(0.0F, -3.0F, -3.0F, 1.0F, 1.0F, 2.0F, new CubeDeformation(-0.1F)), PartPose.offsetAndRotation(1.5F, 6.5F, -6.0F, 1.5708F, -1.3963F, -1.5708F));

		PartDefinition cube_r13 = leftArm.addOrReplaceChild("cube_r13", CubeListBuilder.create().texOffs(14, 22).addBox(0.0F, -3.0F, -3.0F, 1.0F, 1.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(3.5F, 6.0F, -3.0F, 0.1745F, 0.0F, 0.0F));

		PartDefinition cube_r14 = leftArm.addOrReplaceChild("cube_r14", CubeListBuilder.create().texOffs(0, 29).addBox(-1.0F, -1.0F, -1.0F, 4.0F, 1.0F, 1.0F, new CubeDeformation(0.3F)), PartPose.offsetAndRotation(0.0F, 1.0F, 0.0F, 0.0F, 0.0F, 0.829F));

		PartDefinition rightArm = homunculus.addOrReplaceChild("rightArm", CubeListBuilder.create().texOffs(22, 37).addBox(-3.0F, 2.0F, -2.0F, 1.0F, 2.0F, 1.0F, new CubeDeformation(0.2F))
		.texOffs(30, 14).addBox(-2.5F, 1.0F, -5.0F, 1.0F, 1.0F, 3.0F, new CubeDeformation(0.3F)), PartPose.offset(-2.0F, -3.0F, 1.0F));

		PartDefinition cube_r15 = rightArm.addOrReplaceChild("cube_r15", CubeListBuilder.create().texOffs(0, 35).addBox(0.0F, -3.0F, -3.0F, 1.0F, 1.0F, 2.0F, new CubeDeformation(-0.2F)), PartPose.offsetAndRotation(-4.7F, 8.0F, -3.0F, 1.5708F, -1.3963F, -1.5708F));

		PartDefinition cube_r16 = rightArm.addOrReplaceChild("cube_r16", CubeListBuilder.create().texOffs(18, 31).addBox(-1.0F, -1.0F, -1.0F, 3.0F, 1.0F, 1.0F, new CubeDeformation(-0.2F)), PartPose.offsetAndRotation(-3.5F, 3.0F, -0.3F, 1.6468F, -0.9586F, -0.804F));

		PartDefinition cube_r17 = rightArm.addOrReplaceChild("cube_r17", CubeListBuilder.create().texOffs(0, 27).addBox(-1.0F, -1.0F, -1.0F, 5.0F, 1.0F, 1.0F, new CubeDeformation(0.3F)), PartPose.offsetAndRotation(0.0F, -1.0F, 1.0F, 0.0F, 0.0F, 2.1817F));

		PartDefinition cube_r18 = rightArm.addOrReplaceChild("cube_r18", CubeListBuilder.create().texOffs(0, 31).addBox(0.0F, -3.0F, -2.0F, 1.0F, 1.0F, 3.0F, new CubeDeformation(-0.2F)), PartPose.offsetAndRotation(-3.5F, 6.0F, 0.0F, 0.829F, 0.0F, 0.0F));

		PartDefinition cube_r19 = rightArm.addOrReplaceChild("cube_r19", CubeListBuilder.create().texOffs(24, 22).addBox(0.0F, -3.0F, -3.0F, 1.0F, 1.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-3.5F, 5.0F, -2.0F, 0.1745F, 0.0F, 0.0F));

		PartDefinition cube_r20 = rightArm.addOrReplaceChild("cube_r20", CubeListBuilder.create().texOffs(30, 34).addBox(0.0F, -3.0F, -3.0F, 1.0F, 1.0F, 2.0F, new CubeDeformation(-0.2F)), PartPose.offsetAndRotation(-4.2F, 7.4F, -4.0F, 1.5708F, -1.3963F, -1.5708F));

		PartDefinition cube_r21 = rightArm.addOrReplaceChild("cube_r21", CubeListBuilder.create().texOffs(24, 34).addBox(0.0F, -3.0F, -3.0F, 1.0F, 1.0F, 2.0F, new CubeDeformation(-0.2F)), PartPose.offsetAndRotation(-4.2F, 6.3F, -4.0F, 1.5708F, -1.3963F, -1.5708F));

		PartDefinition cube_r22 = rightArm.addOrReplaceChild("cube_r22", CubeListBuilder.create().texOffs(30, 18).addBox(0.0F, -3.0F, -2.0F, 1.0F, 1.0F, 3.0F, new CubeDeformation(-0.2F)), PartPose.offsetAndRotation(-3.5F, 5.0F, -1.0F, 0.5236F, 0.0F, 0.0F));

		PartDefinition cube_r23 = rightArm.addOrReplaceChild("cube_r23", CubeListBuilder.create().texOffs(34, 22).addBox(0.0F, -3.0F, -3.0F, 1.0F, 1.0F, 2.0F, new CubeDeformation(-0.1F)), PartPose.offsetAndRotation(-4.5F, 5.5F, -5.0F, 1.5708F, -1.3963F, -1.5708F));

		PartDefinition cube_r24 = rightArm.addOrReplaceChild("cube_r24", CubeListBuilder.create().texOffs(30, 27).addBox(0.0F, -3.0F, -2.0F, 1.0F, 1.0F, 3.0F, new CubeDeformation(-0.2F)), PartPose.offsetAndRotation(-3.5F, 6.0F, -1.0F, 0.6545F, 0.0F, 0.0F));

		PartDefinition leftLeg = homunculus.addOrReplaceChild("leftLeg", CubeListBuilder.create().texOffs(14, 34).addBox(0.0F, 0.0F, -1.0F, 1.0F, 3.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(18, 36).addBox(0.0F, 2.0F, -1.0F, 1.0F, 3.0F, 1.0F, new CubeDeformation(-0.2F))
		.texOffs(16, 6).addBox(-0.5F, 4.0F, -5.0F, 2.0F, 1.0F, 5.0F, new CubeDeformation(0.3F)), PartPose.offset(-1.0F, 2.0F, 2.0F));

		PartDefinition rightLeg = homunculus.addOrReplaceChild("rightLeg", CubeListBuilder.create().texOffs(36, 34).addBox(-1.0F, 2.0F, -1.0F, 1.0F, 3.0F, 1.0F, new CubeDeformation(-0.2F))
		.texOffs(6, 37).addBox(-1.0F, 0.0F, -1.0F, 1.0F, 3.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(16, 0).addBox(-1.5F, 4.0F, -5.0F, 2.0F, 1.0F, 5.0F, new CubeDeformation(0.3F)), PartPose.offset(2.0F, 2.0F, 2.0F));
        return LayerDefinition.create(meshdefinition, 64, 64);
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, int packedColor) {
        this.root.render(poseStack, buffer, packedLight, packedOverlay, packedColor);
    }

    @Override
    public void setupAnim(HomunculusEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        float radians = (float) Math.PI / 180.0F;
        this.head.yRot = entity.getYRot() * radians;
        this.head.xRot = entity.getXRot() * radians;
        if (entity.isInWater()) {
            // A compact, exaggerated dog-paddle so the small model visibly swims like a player.
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
