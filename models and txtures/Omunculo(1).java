// Made with Blockbench 5.2.2
// Exported for Minecraft version 1.17 or later with Mojang mappings
// Paste this class into your mod and generate all required imports


public class Omunculo(1)<T extends Entity> extends EntityModel<T> {
	// This layer location should be baked with EntityRendererProvider.Context in the entity renderer and passed into this model's constructor
	public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(new ResourceLocation("modid", "omunculo(1)"), "main");
	private final ModelPart Homunculus;
	private final ModelPart Head;
	private final ModelPart Body;
	private final ModelPart Left arm;
	private final ModelPart Right arm;
	private final ModelPart Left leg;
	private final ModelPart Right leg;

	public Omunculo(1)(ModelPart root) {
		this.Homunculus = root.getChild("Homunculus");
		this.Head = this.Homunculus.getChild("Head");
		this.Body = this.Homunculus.getChild("Body");
		this.Left arm = this.Homunculus.getChild("Left arm");
		this.Right arm = this.Homunculus.getChild("Right arm");
		this.Left leg = this.Homunculus.getChild("Left leg");
		this.Right leg = this.Homunculus.getChild("Right leg");
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();

		PartDefinition Homunculus = partdefinition.addOrReplaceChild("Homunculus", CubeListBuilder.create(), PartPose.offset(0.0F, 17.0F, 0.0F));

		PartDefinition Head = Homunculus.addOrReplaceChild("Head", CubeListBuilder.create().texOffs(0, 0).addBox(-3.0F, -4.0F, -2.0F, 5.0F, 5.0F, 3.0F, new CubeDeformation(0.0F))
		.texOffs(12, 27).addBox(-2.0F, -5.0F, -1.0F, 3.0F, 1.0F, 2.0F, new CubeDeformation(0.0F))
		.texOffs(16, 12).addBox(-2.0F, -0.5F, -6.0F, 3.0F, 1.0F, 4.0F, new CubeDeformation(0.0F))
		.texOffs(26, 37).addBox(1.0F, -3.0F, -3.0F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.5F))
		.texOffs(30, 37).addBox(-3.0F, -3.0F, -3.0F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.7F)), PartPose.offset(1.0F, -6.0F, 0.0F));

		PartDefinition cube_r1 = Head.addOrReplaceChild("cube_r1", CubeListBuilder.create().texOffs(0, 22).addBox(-2.0F, -1.0F, -1.0F, 3.0F, 1.0F, 4.0F, new CubeDeformation(-0.3F)), PartPose.offsetAndRotation(0.0F, 2.0F, -5.0F, 0.6545F, 0.0F, 0.0F));

		PartDefinition cube_r2 = Head.addOrReplaceChild("cube_r2", CubeListBuilder.create().texOffs(18, 19).addBox(-1.0F, -2.0F, -1.0F, 3.0F, 1.0F, 2.0F, new CubeDeformation(0.3F)), PartPose.offsetAndRotation(-1.0F, 2.5F, -2.0F, 0.6545F, 0.0F, 0.0F));

		PartDefinition cube_r3 = Head.addOrReplaceChild("cube_r3", CubeListBuilder.create().texOffs(34, 25).addBox(-1.5F, -2.0F, -1.0F, 2.0F, 1.0F, 1.0F, new CubeDeformation(0.2F)), PartPose.offsetAndRotation(0.0F, 0.0F, -4.0F, -0.3491F, 0.0F, 0.0F));

		PartDefinition cube_r4 = Head.addOrReplaceChild("cube_r4", CubeListBuilder.create().texOffs(22, 27).addBox(0.0F, -2.0F, -2.0F, 1.0F, 1.0F, 3.0F, new CubeDeformation(0.3F)), PartPose.offsetAndRotation(-1.0F, 0.5F, -2.0F, -0.1309F, 0.0F, 0.0F));

		PartDefinition Body = Homunculus.addOrReplaceChild("Body", CubeListBuilder.create().texOffs(0, 8).addBox(-2.0F, -1.0F, -2.0F, 5.0F, 4.0F, 3.0F, new CubeDeformation(0.5F))
		.texOffs(0, 15).addBox(-2.0F, 3.0F, -2.0F, 5.0F, 4.0F, 3.0F, new CubeDeformation(-0.5F)), PartPose.offset(0.0F, -4.0F, 2.0F));

		PartDefinition Left arm = Homunculus.addOrReplaceChild("Left arm", CubeListBuilder.create().texOffs(30, 2).addBox(2.0F, 2.0F, -6.0F, 1.0F, 1.0F, 3.0F, new CubeDeformation(0.3F))
		.texOffs(10, 37).addBox(3.0F, 3.0F, -3.0F, 1.0F, 2.0F, 1.0F, new CubeDeformation(0.2F)), PartPose.offset(3.0F, -4.0F, 2.0F));

		PartDefinition cube_r5 = Left arm.addOrReplaceChild("cube_r5", CubeListBuilder.create().texOffs(30, 0).addBox(-1.0F, -1.0F, -1.0F, 4.0F, 1.0F, 1.0F, new CubeDeformation(-0.2F)), PartPose.offsetAndRotation(2.5F, 4.0F, -3.0F, -3.1416F, -1.0472F, -2.3126F));

		PartDefinition cube_r6 = Left arm.addOrReplaceChild("cube_r6", CubeListBuilder.create().texOffs(30, 10).addBox(0.0F, -3.0F, -2.0F, 1.0F, 1.0F, 3.0F, new CubeDeformation(-0.2F)), PartPose.offsetAndRotation(3.5F, 7.0F, -1.0F, 0.829F, 0.0F, 0.0F));

		PartDefinition cube_r7 = Left arm.addOrReplaceChild("cube_r7", CubeListBuilder.create().texOffs(10, 30).addBox(0.0F, -3.0F, -2.0F, 1.0F, 1.0F, 3.0F, new CubeDeformation(-0.2F)), PartPose.offsetAndRotation(3.5F, 7.0F, -2.0F, 0.6545F, 0.0F, 0.0F));

		PartDefinition cube_r8 = Left arm.addOrReplaceChild("cube_r8", CubeListBuilder.create().texOffs(30, 6).addBox(0.0F, -3.0F, -2.0F, 1.0F, 1.0F, 3.0F, new CubeDeformation(-0.2F)), PartPose.offsetAndRotation(3.5F, 6.0F, -2.0F, 0.5236F, 0.0F, 0.0F));

		PartDefinition cube_r9 = Left arm.addOrReplaceChild("cube_r9", CubeListBuilder.create().texOffs(8, 34).addBox(0.0F, -3.0F, -3.0F, 1.0F, 1.0F, 2.0F, new CubeDeformation(-0.2F)), PartPose.offsetAndRotation(1.8F, 8.4F, -5.0F, 1.5708F, -1.3963F, -1.5708F));

		PartDefinition cube_r10 = Left arm.addOrReplaceChild("cube_r10", CubeListBuilder.create().texOffs(18, 33).addBox(0.0F, -3.0F, -3.0F, 1.0F, 1.0F, 2.0F, new CubeDeformation(-0.2F)), PartPose.offsetAndRotation(1.3F, 9.0F, -4.0F, 1.5708F, -1.3963F, -1.5708F));

		PartDefinition cube_r11 = Left arm.addOrReplaceChild("cube_r11", CubeListBuilder.create().texOffs(32, 31).addBox(0.0F, -3.0F, -3.0F, 1.0F, 1.0F, 2.0F, new CubeDeformation(-0.2F)), PartPose.offsetAndRotation(1.8F, 7.3F, -5.0F, 1.5708F, -1.3963F, -1.5708F));

		PartDefinition cube_r12 = Left arm.addOrReplaceChild("cube_r12", CubeListBuilder.create().texOffs(26, 31).addBox(0.0F, -3.0F, -3.0F, 1.0F, 1.0F, 2.0F, new CubeDeformation(-0.1F)), PartPose.offsetAndRotation(1.5F, 6.5F, -6.0F, 1.5708F, -1.3963F, -1.5708F));

		PartDefinition cube_r13 = Left arm.addOrReplaceChild("cube_r13", CubeListBuilder.create().texOffs(14, 22).addBox(0.0F, -3.0F, -3.0F, 1.0F, 1.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(3.5F, 6.0F, -3.0F, 0.1745F, 0.0F, 0.0F));

		PartDefinition cube_r14 = Left arm.addOrReplaceChild("cube_r14", CubeListBuilder.create().texOffs(0, 29).addBox(-1.0F, -1.0F, -1.0F, 4.0F, 1.0F, 1.0F, new CubeDeformation(0.3F)), PartPose.offsetAndRotation(0.0F, 1.0F, 0.0F, 0.0F, 0.0F, 0.829F));

		PartDefinition Right arm = Homunculus.addOrReplaceChild("Right arm", CubeListBuilder.create().texOffs(22, 37).addBox(-3.0F, 2.0F, -2.0F, 1.0F, 2.0F, 1.0F, new CubeDeformation(0.2F))
		.texOffs(30, 14).addBox(-2.5F, 1.0F, -5.0F, 1.0F, 1.0F, 3.0F, new CubeDeformation(0.3F)), PartPose.offset(-2.0F, -3.0F, 1.0F));

		PartDefinition cube_r15 = Right arm.addOrReplaceChild("cube_r15", CubeListBuilder.create().texOffs(0, 35).addBox(0.0F, -3.0F, -3.0F, 1.0F, 1.0F, 2.0F, new CubeDeformation(-0.2F)), PartPose.offsetAndRotation(-4.7F, 8.0F, -3.0F, 1.5708F, -1.3963F, -1.5708F));

		PartDefinition cube_r16 = Right arm.addOrReplaceChild("cube_r16", CubeListBuilder.create().texOffs(18, 31).addBox(-1.0F, -1.0F, -1.0F, 3.0F, 1.0F, 1.0F, new CubeDeformation(-0.2F)), PartPose.offsetAndRotation(-3.5F, 3.0F, -0.3F, 1.6468F, -0.9586F, -0.804F));

		PartDefinition cube_r17 = Right arm.addOrReplaceChild("cube_r17", CubeListBuilder.create().texOffs(0, 27).addBox(-1.0F, -1.0F, -1.0F, 5.0F, 1.0F, 1.0F, new CubeDeformation(0.3F)), PartPose.offsetAndRotation(0.0F, -1.0F, 1.0F, 0.0F, 0.0F, 2.1817F));

		PartDefinition cube_r18 = Right arm.addOrReplaceChild("cube_r18", CubeListBuilder.create().texOffs(0, 31).addBox(0.0F, -3.0F, -2.0F, 1.0F, 1.0F, 3.0F, new CubeDeformation(-0.2F)), PartPose.offsetAndRotation(-3.5F, 6.0F, 0.0F, 0.829F, 0.0F, 0.0F));

		PartDefinition cube_r19 = Right arm.addOrReplaceChild("cube_r19", CubeListBuilder.create().texOffs(24, 22).addBox(0.0F, -3.0F, -3.0F, 1.0F, 1.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-3.5F, 5.0F, -2.0F, 0.1745F, 0.0F, 0.0F));

		PartDefinition cube_r20 = Right arm.addOrReplaceChild("cube_r20", CubeListBuilder.create().texOffs(30, 34).addBox(0.0F, -3.0F, -3.0F, 1.0F, 1.0F, 2.0F, new CubeDeformation(-0.2F)), PartPose.offsetAndRotation(-4.2F, 7.4F, -4.0F, 1.5708F, -1.3963F, -1.5708F));

		PartDefinition cube_r21 = Right arm.addOrReplaceChild("cube_r21", CubeListBuilder.create().texOffs(24, 34).addBox(0.0F, -3.0F, -3.0F, 1.0F, 1.0F, 2.0F, new CubeDeformation(-0.2F)), PartPose.offsetAndRotation(-4.2F, 6.3F, -4.0F, 1.5708F, -1.3963F, -1.5708F));

		PartDefinition cube_r22 = Right arm.addOrReplaceChild("cube_r22", CubeListBuilder.create().texOffs(30, 18).addBox(0.0F, -3.0F, -2.0F, 1.0F, 1.0F, 3.0F, new CubeDeformation(-0.2F)), PartPose.offsetAndRotation(-3.5F, 5.0F, -1.0F, 0.5236F, 0.0F, 0.0F));

		PartDefinition cube_r23 = Right arm.addOrReplaceChild("cube_r23", CubeListBuilder.create().texOffs(34, 22).addBox(0.0F, -3.0F, -3.0F, 1.0F, 1.0F, 2.0F, new CubeDeformation(-0.1F)), PartPose.offsetAndRotation(-4.5F, 5.5F, -5.0F, 1.5708F, -1.3963F, -1.5708F));

		PartDefinition cube_r24 = Right arm.addOrReplaceChild("cube_r24", CubeListBuilder.create().texOffs(30, 27).addBox(0.0F, -3.0F, -2.0F, 1.0F, 1.0F, 3.0F, new CubeDeformation(-0.2F)), PartPose.offsetAndRotation(-3.5F, 6.0F, -1.0F, 0.6545F, 0.0F, 0.0F));

		PartDefinition Left leg = Homunculus.addOrReplaceChild("Left leg", CubeListBuilder.create().texOffs(14, 34).addBox(0.0F, 0.0F, -1.0F, 1.0F, 3.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(18, 36).addBox(0.0F, 2.0F, -1.0F, 1.0F, 3.0F, 1.0F, new CubeDeformation(-0.2F))
		.texOffs(16, 6).addBox(-0.5F, 4.0F, -5.0F, 2.0F, 1.0F, 5.0F, new CubeDeformation(0.3F)), PartPose.offset(-1.0F, 2.0F, 2.0F));

		PartDefinition Right leg = Homunculus.addOrReplaceChild("Right leg", CubeListBuilder.create().texOffs(36, 34).addBox(-1.0F, 2.0F, -1.0F, 1.0F, 3.0F, 1.0F, new CubeDeformation(-0.2F))
		.texOffs(6, 37).addBox(-1.0F, 0.0F, -1.0F, 1.0F, 3.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(16, 0).addBox(-1.5F, 4.0F, -5.0F, 2.0F, 1.0F, 5.0F, new CubeDeformation(0.3F)), PartPose.offset(2.0F, 2.0F, 2.0F));

		return LayerDefinition.create(meshdefinition, 64, 64);
	}

	@Override
	public void setupAnim(Entity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {

	}

	@Override
	public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
		Homunculus.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
	}
}