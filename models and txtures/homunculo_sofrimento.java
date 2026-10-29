// Made with Blockbench 5.2.2
// Exported for Minecraft version 1.17 or later with Mojang mappings
// Paste this class into your mod and generate all required imports


public class CustomModel<T extends Entity> extends EntityModel<T> {
	// This layer location should be baked with EntityRendererProvider.Context in the entity renderer and passed into this model's constructor
	public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(new ResourceLocation("modid", "custommodel"), "main");
	private final ModelPart Waist;
	private final ModelPart Head;
	private final ModelPart bone;
	private final ModelPart Body;
	private final ModelPart Right Arm;
	private final ModelPart Left Arm;
	private final ModelPart Right Leg;
	private final ModelPart Left Leg;

	public CustomModel(ModelPart root) {
		this.Waist = root.getChild("Waist");
		this.Head = this.Waist.getChild("Head");
		this.bone = this.Head.getChild("bone");
		this.Body = this.Waist.getChild("Body");
		this.Right Arm = this.Waist.getChild("Right Arm");
		this.Left Arm = this.Waist.getChild("Left Arm");
		this.Right Leg = root.getChild("Right Leg");
		this.Left Leg = root.getChild("Left Leg");
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();

		PartDefinition Waist = partdefinition.addOrReplaceChild("Waist", CubeListBuilder.create(), PartPose.offset(0.0F, 12.0F, 0.0F));

		PartDefinition Head = Waist.addOrReplaceChild("Head", CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F, new CubeDeformation(0.0F))
		.texOffs(0, 16).addBox(-8.0F, -13.0F, 0.0F, 16.0F, 13.0F, 0.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, -12.0F, 0.0F));

		PartDefinition bone = Head.addOrReplaceChild("bone", CubeListBuilder.create().texOffs(4, 11).addBox(0.0F, -1.0F, -1.0F, 1.0F, 1.0F, 1.0F, new CubeDeformation(-0.2F))
		.texOffs(4, 11).addBox(4.0F, -1.0F, -1.0F, 1.0F, 1.0F, 1.0F, new CubeDeformation(-0.2F)), PartPose.offset(-2.5F, -4.0F, -3.25F));

		PartDefinition Body = Waist.addOrReplaceChild("Body", CubeListBuilder.create().texOffs(0, 29).addBox(-4.0F, 0.0F, -2.0F, 8.0F, 12.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, -12.0F, 0.0F));

		PartDefinition Right Arm_r1 = Body.addOrReplaceChild("Right Arm_r1", CubeListBuilder.create().texOffs(32, 16).addBox(0.0F, -5.0F, -1.0F, 2.0F, 10.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-1.0F, 14.0F, 2.0F, -3.0107F, 0.0F, 0.0F));

		PartDefinition Right Arm = Waist.addOrReplaceChild("Right Arm", CubeListBuilder.create().texOffs(24, 29).addBox(-3.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offset(-5.0F, -10.0F, 0.0F));

		PartDefinition Left Arm = Waist.addOrReplaceChild("Left Arm", CubeListBuilder.create().texOffs(32, 0).addBox(-1.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offset(5.0F, -10.0F, 0.0F));

		PartDefinition Right Leg = partdefinition.addOrReplaceChild("Right Leg", CubeListBuilder.create().texOffs(40, 16).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offset(-1.9F, 12.0F, 0.0F));

		PartDefinition Left Leg = partdefinition.addOrReplaceChild("Left Leg", CubeListBuilder.create().texOffs(40, 32).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offset(1.9F, 12.0F, 0.0F));

		return LayerDefinition.create(meshdefinition, 64, 64);
	}

	@Override
	public void setupAnim(Entity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {

	}

	@Override
	public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
		Waist.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
		Right Leg.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
		Left Leg.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
	}
}