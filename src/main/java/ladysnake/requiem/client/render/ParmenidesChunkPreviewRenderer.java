package ladysnake.requiem.client.render;

import ladysnake.requiem.client.ParmenidesClientState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.OverlayTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.world.phys.Vec3;

/**
 * Draws the other dimension's matching chunk as a client-only neighboring slice.
 * It is visual only: it has no collision, block entities, or interaction.
 */
public final class ParmenidesChunkPreviewRenderer {
    private ParmenidesChunkPreviewRenderer() { }

    public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS
                || !ParmenidesClientState.hasPreview()) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        Vec3 camera = event.getCamera().getPosition();
        int chunkX = ParmenidesClientState.previewChunkX();
        int chunkZ = ParmenidesClientState.previewChunkZ();
        int minY = ParmenidesClientState.previewMinY();
        int height = ParmenidesClientState.previewHeight();

        // Keep the counterpart view immediately beside the real chunk, with a one-block gap.
        int displayX = (chunkX << 4) + 17;
        int displayZ = chunkZ << 4;
        PoseStack pose = event.getPoseStack();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        pose.pushPose();
        pose.translate(displayX - camera.x, -camera.y, displayZ - camera.z);

        for (int y = 0; y < height; y++) {
            int worldY = minY + y;
            for (int z = 0; z < 16; z++) {
                for (int x = 0; x < 16; x++) {
                    BlockState state = ParmenidesClientState.previewState(x, y, z);
                    if (state.isAir()) continue;
                    pose.pushPose();
                    pose.translate(x, worldY, z);
                    mc.getBlockRenderer().renderSingleBlock(
                        state, pose, buffers, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY
                    );
                    pose.popPose();
                }
            }
        }

        pose.popPose();
        buffers.endBatch();
    }
}
