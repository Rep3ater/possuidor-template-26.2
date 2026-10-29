package ladysnake.requiem.client.render.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import ladysnake.requiem.common.construct.ActuatorType;
import ladysnake.requiem.common.construct.ConstructPart;
import ladysnake.requiem.common.construct.ConstructVoxel;
import ladysnake.requiem.common.construct.ConstructVoxelModel;
import ladysnake.requiem.common.construct.entity.RoboticConstructEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.RenderShape;

public class RoboticConstructRenderer extends EntityRenderer<RoboticConstructEntity> {
    public RoboticConstructRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.6F;
    }

    @Override
    public ResourceLocation getTextureLocation(RoboticConstructEntity entity) {
        return ResourceLocation.withDefaultNamespace("textures/block/iron_block.png");
    }

    @Override
    public void render(RoboticConstructEntity entity, float entityYaw, float partialTicks, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight) {
        ConstructVoxelModel model = entity.getVoxelModel();
        if (model != null && !model.isEmpty()) {
            this.shadowRadius = 0.6F * model.getScale();
            poseStack.pushPose();
            poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - entity.yBodyRot));
            float scale = model.getScale();
            poseStack.scale(scale, scale, scale);
            poseStack.translate(0.0D, model.getGroundOffset(), 0.0D);

            float phase = entity.walkAnimation.position(partialTicks) * 0.6662F;
            float legSwing = Mth.cos(phase) * 1.4F * entity.walkAnimation.speed(partialTicks);
            float armSwing = Mth.cos(phase + (float) Math.PI) * 1.4F * entity.walkAnimation.speed(partialTicks);
            int punchTicks = entity.getPunchAnimationTicks();
            float punchProgress = punchTicks > 0
                ? (float) Math.sin((12 - punchTicks) / 12.0F * Math.PI) * 0.8F
                : 0.0F;

            for (ConstructVoxel voxel : model.getVoxels().values()) {
                if (voxel.isDestroyed() || voxel.getState().getRenderShape() != RenderShape.MODEL) continue;
                poseStack.pushPose();
                BlockPos rel = voxel.getRelPos();
                ConstructPart part = voxel.getPart();

                switch (part) {
                    case HEAD -> {
                        poseStack.translate(0, 1.5, 0);
                        poseStack.mulPose(Axis.YP.rotationDegrees(entity.getYRot() - entity.yBodyRot));
                        poseStack.mulPose(Axis.XP.rotationDegrees(entity.getXRot()));
                        poseStack.translate(0, -1.5, 0);
                    }
                    case LEFT_ARM -> {
                        poseStack.translate(-1.0, 1.2, 0);
                        poseStack.mulPose(Axis.XP.rotationDegrees(armSwing * 20.0F));
                        if (voxel.getActuator() == ActuatorType.PISTON_THRUST) poseStack.translate(0, 0, punchProgress);
                        poseStack.translate(1.0, -1.2, 0);
                    }
                    case RIGHT_ARM -> {
                        poseStack.translate(1.0, 1.2, 0);
                        poseStack.mulPose(Axis.XP.rotationDegrees(-armSwing * 20.0F));
                        if (voxel.getActuator() == ActuatorType.PISTON_THRUST) poseStack.translate(0, 0, punchProgress);
                        poseStack.translate(-1.0, -1.2, 0);
                    }
                    case LEFT_LEG -> {
                        poseStack.translate(-0.5, 0.5, 0);
                        poseStack.mulPose(Axis.XP.rotationDegrees(legSwing * 25.0F));
                        poseStack.translate(0.5, -0.5, 0);
                    }
                    case RIGHT_LEG -> {
                        poseStack.translate(0.5, 0.5, 0);
                        poseStack.mulPose(Axis.XP.rotationDegrees(-legSwing * 25.0F));
                        poseStack.translate(-0.5, -0.5, 0);
                    }
                    case TORSO -> { }
                }

                poseStack.translate(rel.getX() - 0.5, rel.getY(), rel.getZ() - 0.5);
                Minecraft.getInstance().getBlockRenderer().renderSingleBlock(
                    voxel.getState(), poseStack, buffer, packedLight, OverlayTexture.NO_OVERLAY
                );
                poseStack.popPose();
            }
            poseStack.popPose();
        }
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }
}
