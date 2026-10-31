package ladysnake.requiem.client.render;

import ladysnake.requiem.client.ParmenidesClientState;
import ladysnake.requiem.client.ParmenidesClientState.PreviewBlock;
import ladysnake.requiem.client.ParmenidesClientState.PreviewChunk;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import com.mojang.blaze3d.vertex.PoseStack;

/**
 * Draws only counterpart-dimension surface blocks. The server strips enclosed blocks
 * from each snapshot, and the client decodes registry IDs once when packets arrive.
 */
public final class ParmenidesChunkPreviewRenderer {
    private ParmenidesChunkPreviewRenderer() { }

    public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS
                || !ParmenidesClientState.hasPreview()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        double radius = ParmenidesClientState.revealRadius();
        if (radius <= 0.0D) return;
        double radiusSquared = radius * radius;
        Vec3 camera = event.getCamera().getPosition();
        double px = mc.player.getX();
        double pz = mc.player.getZ();
        PoseStack pose = event.getPoseStack();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        BlockPos.MutableBlockPos lightPos = new BlockPos.MutableBlockPos();

        for (PreviewChunk chunk : ParmenidesClientState.previewChunks().values()) {
            int baseX = chunk.chunkX() << 4;
            int baseZ = chunk.chunkZ() << 4;
            // Skip a chunk when even its nearest point is outside the expanding circle.
            double nearestX = Math.max(baseX, Math.min(px, baseX + 16.0D));
            double nearestZ = Math.max(baseZ, Math.min(pz, baseZ + 16.0D));
            double ndx = nearestX - px;
            double ndz = nearestZ - pz;
            if (ndx * ndx + ndz * ndz > radiusSquared) continue;

            pose.pushPose();
            pose.translate(baseX - camera.x, -camera.y, baseZ - camera.z);
            for (PreviewBlock block : chunk.surfaceBlocks()) {
                int index = block.localIndex();
                int x = index & 15;
                int z = (index >> 4) & 15;
                int y = index >> 8;
                if (y < 0 || y >= chunk.height()) continue;

                int worldX = baseX + x;
                int worldY = chunk.minY() + y;
                int worldZ = baseZ + z;
                double dx = worldX + 0.5D - px;
                double dz = worldZ + 0.5D - pz;
                if (dx * dx + dz * dz > radiusSquared) continue;

                lightPos.set(worldX, worldY, worldZ);
                // The counterpart dimension has its own lighting; using Nether light values
                // for an Overworld preview (or vice versa) makes much of it nearly invisible.
                // Full-bright preview lighting keeps the remote geometry readable.
                int packedLight = 0x00F000F0;
                pose.pushPose();
                pose.translate(x, worldY, z);
                mc.getBlockRenderer().renderSingleBlock(
                    block.state(), pose, buffers, packedLight, OverlayTexture.NO_OVERLAY
                );
                pose.popPose();
            }
            pose.popPose();
        }
        buffers.endBatch();
    }
}
