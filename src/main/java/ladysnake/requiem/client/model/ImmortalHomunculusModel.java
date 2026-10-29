package ladysnake.requiem.client.model;

import ladysnake.requiem.Requiem;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

/** Blockbench model port with the supplied 128x128 texture and animated limbs. */
public final class ImmortalHomunculusModel extends EntityModel<LivingEntityRenderState> {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(Requiem.id("waist"), "main");
    private final ModelPart waist, head, body, leftArm, rightArm, leftLeg, rightLeg;

    public ImmortalHomunculusModel(ModelPart root) {
        super(root);
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
		.texOffs(0, 32).addBox(-8.0F, -13.0F, 0.0F, 16.0F, 13.0F, 0.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, -12.0F, 0.0F));

		PartDefinition bone = head.addOrReplaceChild("bone", CubeListBuilder.create().texOffs(8, 22).addBox(0.0F, -1.0F, -1.0F, 1.0F, 1.0F, 1.0F, new CubeDeformation(-0.2F))
		.texOffs(8, 22).addBox(4.0F, -1.0F, -1.0F, 1.0F, 1.0F, 1.0F, new CubeDeformation(-0.2F)), PartPose.offset(-2.5F, -4.0F, -3.25F));

		PartDefinition body = waist.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 58).addBox(-4.0F, 0.0F, -2.0F, 8.0F, 12.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, -12.0F, 0.0F));

		PartDefinition rightArm_r1 = body.addOrReplaceChild("rightArm_r1", CubeListBuilder.create().texOffs(64, 32).addBox(0.0F, -5.0F, -1.0F, 2.0F, 10.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-1.0F, 14.0F, 2.0F, -3.0107F, 0.0F, 0.0F));

		PartDefinition rightArm = waist.addOrReplaceChild("rightArm", CubeListBuilder.create().texOffs(48, 58).addBox(-3.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offset(-5.0F, -10.0F, 0.0F));

		PartDefinition leftArm = waist.addOrReplaceChild("leftArm", CubeListBuilder.create().texOffs(64, 0).addBox(-1.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offset(5.0F, -10.0F, 0.0F));

		PartDefinition rightLeg = partdefinition.addOrReplaceChild("rightLeg", CubeListBuilder.create().texOffs(80, 32).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offset(-1.9F, 12.0F, 0.0F));

		PartDefinition leftLeg = partdefinition.addOrReplaceChild("leftLeg", CubeListBuilder.create().texOffs(80, 64).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offset(1.9F, 12.0F, 0.0F));
        return LayerDefinition.create(meshdefinition, 128, 128);
    }

    @Override
    public void setupAnim(LivingEntityRenderState state) {
        float radians = (float) Math.PI / 180.0F;
        this.head.yRot = state.yRot * radians;
        this.head.xRot = state.xRot * radians;
        float phase = state.walkAnimationPos * 0.6662F;
        float swing = 1.4F * state.walkAnimationSpeed;
        this.leftLeg.xRot = net.minecraft.util.Mth.cos(phase) * swing;
        this.rightLeg.xRot = net.minecraft.util.Mth.cos(phase + (float) Math.PI) * swing;
        this.leftArm.xRot = net.minecraft.util.Mth.cos(phase + (float) Math.PI) * swing * 0.7F;
        this.rightArm.xRot = net.minecraft.util.Mth.cos(phase) * swing * 0.7F;
    }
}
