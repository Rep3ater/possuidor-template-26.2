package ladysnake.requiem.client.render;

import ladysnake.requiem.client.ParmenidesClientState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.OverlayTexture;
import net.minecraft.client.renderer.MultiBufferSource;
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

        // Replace the visible terrain in this chunk with the counterpart dimension's
        // terrain. Keeping the same world coordinates makes this a true counterpart view,
        // rather than an unrelated miniature placed beside the player.
        PoseStack pose = event.getPoseStack();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        pose.pushPose();
        pose.translate((chunkX << 4) - camera.x, -camera.y, (chunkZ << 4) - camera.z);

        for (int entry = 0; entry < ParmenidesClientState.previewBlockCount(); entry++) {
            int localIndex = ParmenidesClientState.previewLocalIndex(entry);
            int x = localIndex & 15;
            int z = (localIndex >> 4) & 15;
            int y = localIndex >> 8;
            if (y >= height) continue;
            BlockState state = ParmenidesClientState.previewBlockState(entry);
            if (state.isAir()) continue;
            pose.pushPose();
            pose.translate(x, minY + y, z);
            mc.getBlockRenderer().renderSingleBlock(
                state, pose, buffers, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY
            );
            pose.popPose();
        }

        pose.popPose();
        buffers.endBatch();
    }
}
