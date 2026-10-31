package ladysnake.requiem.client.render;

import ladysnake.requiem.client.ParmenidesClientState;
import ladysnake.requiem.client.ParmenidesClientState.PreviewChunk;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import com.mojang.blaze3d.vertex.PoseStack;

/** Renders counterpart-dimension terrain as a shaded, expanding 3x3 chunk domain. */
public final class ParmenidesChunkPreviewRenderer {
    private ParmenidesChunkPreviewRenderer() { }

    public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS
                || !ParmenidesClientState.hasPreview()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        double revealRadius = ParmenidesClientState.revealRadius();
        if (revealRadius <= 0.0D) return;

        Vec3 camera = event.getCamera().getPosition();
        PoseStack pose = event.getPoseStack();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();

        for (PreviewChunk chunk : ParmenidesClientState.previewChunks().values()) {
            pose.pushPose();
            pose.translate((chunk.chunkX() << 4) - camera.x, -camera.y, (chunk.chunkZ() << 4) - camera.z);

            for (int entry = 0; entry < chunk.blockCount(); entry++) {
                int localIndex = chunk.localIndex(entry);
                int x = localIndex & 15;
                int z = (localIndex >> 4) & 15;
                int y = localIndex >> 8;
                if (y >= chunk.height()) continue;

                int worldX = (chunk.chunkX() << 4) + x;
                int worldY = chunk.minY() + y;
                int worldZ = (chunk.chunkZ() << 4) + z;
                double dx = worldX + 0.5D - mc.player.getX();
                double dz = worldZ + 0.5D - mc.player.getZ();
                if (dx * dx + dz * dz > revealRadius * revealRadius) continue;

                BlockState state = chunk.blockState(entry);
                if (state.isAir()) continue;

                BlockPos lightPos = new BlockPos(worldX, worldY, worldZ);
                int packedLight = LevelRenderer.getLightColor(mc.level, lightPos);
                pose.pushPose();
                pose.translate(x, worldY, z);
                mc.getBlockRenderer().renderSingleBlock(
                    state, pose, buffers, packedLight, OverlayTexture.NO_OVERLAY
                );
                pose.popPose();
            }

            pose.popPose();
        }
        buffers.endBatch();
    }
}
