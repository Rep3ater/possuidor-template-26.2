package ladysnake.requiem.client.render.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import ladysnake.requiem.common.construct.ActuatorType;
import ladysnake.requiem.common.construct.ConstructPart;
import ladysnake.requiem.common.construct.ConstructVoxel;
import ladysnake.requiem.common.construct.ConstructVoxelModel;
import ladysnake.requiem.common.construct.entity.RoboticConstructEntity;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.MovingBlockRenderState;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.RenderShape;

public class RoboticConstructRenderer extends EntityRenderer<RoboticConstructEntity, RoboticConstructRenderState> {

    public RoboticConstructRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.6f;
    }

    @Override
    public RoboticConstructRenderState createRenderState() {
        return new RoboticConstructRenderState();
    }

    @Override
    public void extractRenderState(RoboticConstructEntity entity, RoboticConstructRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);

        state.voxels.clear();
        state.punchTicks = entity.getPunchAnimationTicks();
        state.walkAnimationPos = entity.walkAnimation.position(partialTicks);
        state.walkAnimationSpeed = entity.walkAnimation.speed(partialTicks);
        state.yRot = entity.getViewYRot(partialTicks);
        state.xRot = entity.getViewXRot(partialTicks);
        state.bodyRot = entity.yBodyRot;

        ConstructVoxelModel model = entity.getVoxelModel();
        state.voxelModel = model;
        if (model != null && !model.isEmpty()) {
            this.shadowRadius = 0.6f * model.getScale();
            for (ConstructVoxel voxel : model.getVoxels().values()) {
                if (voxel.isDestroyed() || voxel.getState().getRenderShape() != RenderShape.MODEL) continue;

                MovingBlockRenderState blockState = new MovingBlockRenderState();
                blockState.blockState = voxel.getState();
                BlockPos worldPos = entity.blockPosition().offset(voxel.getRelPos());
                blockState.blockPos = worldPos;
                blockState.randomSeedPos = worldPos;

                if (entity.level() instanceof ClientLevel clientLevel) {
                    blockState.biome = clientLevel.getBiome(worldPos);
                    blockState.cardinalLighting = clientLevel.cardinalLighting();
                    blockState.lightEngine = clientLevel.getLightEngine();
                }

                state.voxels.add(new RoboticConstructRenderState.VoxelRenderEntry(voxel, blockState));
            }
        }
    }

    @Override
    public void submit(RoboticConstructRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState cameraRenderState) {
        if (!state.voxels.isEmpty() && state.voxelModel != null) {
            poseStack.pushPose();

            // Rotate whole construct by body rotation
            poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - state.bodyRot));

            // Apply scale (1.0 for mecha, 1/16 for micro robot)
            float scale = state.voxelModel.getScale();
            poseStack.scale(scale, scale, scale);

            // Ground offset fix: lifts the lowest voxel to Y = 0 (on the ground)!
            float groundOffset = state.voxelModel.getGroundOffset();
            poseStack.translate(0, groundOffset, 0);

            // Walking animations
            float legSwing = Mth.cos(state.walkAnimationPos * 0.6662F) * 1.4F * state.walkAnimationSpeed;
            float armSwing = Mth.cos(state.walkAnimationPos * 0.6662F + (float) Math.PI) * 1.4F * state.walkAnimationSpeed;

            // Punch extension
            float punchProgress = state.punchTicks > 0 ? (float) Math.sin((12 - state.punchTicks) / 12.0f * Math.PI) * 0.8f : 0.0f;

            for (RoboticConstructRenderState.VoxelRenderEntry entry : state.voxels) {
                ConstructVoxel voxel = entry.voxel;
                MovingBlockRenderState blockState = entry.movingBlockState;

                poseStack.pushPose();

                BlockPos rel = voxel.getRelPos();
                ConstructPart part = voxel.getPart();

                switch (part) {
                    case HEAD -> {
                        float netHeadYaw = state.yRot - state.bodyRot;
                        poseStack.translate(0, 1.5, 0);
                        poseStack.mulPose(Axis.YP.rotationDegrees(-netHeadYaw));
                        poseStack.mulPose(Axis.XP.rotationDegrees(state.xRot));
                        poseStack.translate(0, -1.5, 0);
                    }
                    case LEFT_ARM -> {
                        poseStack.translate(-1.0, 1.2, 0);
                        poseStack.mulPose(Axis.XP.rotationDegrees(armSwing * 20.0f));
                        if (voxel.getActuator() == ActuatorType.PISTON_THRUST) {
                            poseStack.translate(0, 0, punchProgress);
                        }
                        poseStack.translate(1.0, -1.2, 0);
                    }
                    case RIGHT_ARM -> {
                        poseStack.translate(1.0, 1.2, 0);
                        poseStack.mulPose(Axis.XP.rotationDegrees(-armSwing * 20.0f));
                        if (voxel.getActuator() == ActuatorType.PISTON_THRUST) {
                            poseStack.translate(0, 0, punchProgress);
                        }
                        poseStack.translate(-1.0, -1.2, 0);
                    }
                    case LEFT_LEG -> {
                        poseStack.translate(-0.5, 0.5, 0);
                        poseStack.mulPose(Axis.XP.rotationDegrees(legSwing * 25.0f));
                        poseStack.translate(0.5, -0.5, 0);
                    }
                    case RIGHT_LEG -> {
                        poseStack.translate(0.5, 0.5, 0);
                        poseStack.mulPose(Axis.XP.rotationDegrees(-legSwing * 25.0f));
                        poseStack.translate(-0.5, -0.5, 0);
                    }
                    case TORSO -> {
                        // Torso remains centered at root
                    }
                }

                // Center block model on relative coordinate
                poseStack.translate(rel.getX() - 0.5, rel.getY(), rel.getZ() - 0.5);

                submitNodeCollector.submitMovingBlock(poseStack, blockState, 0);

                poseStack.popPose();
            }

            poseStack.popPose();
        }

        super.submit(state, poseStack, submitNodeCollector, cameraRenderState);
    }
}
