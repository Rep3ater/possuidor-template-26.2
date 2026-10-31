package ladysnake.requiem.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import ladysnake.requiem.client.SpeechBubbleClientState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

import java.util.List;

/**
 * Renders active speech bubbles in the world pass after entities have been drawn.
 *
 * Rendering from a dedicated level stage avoids depending on the per-player renderer's
 * post-render pose stack, which can be affected by renderer and shader integrations.
 */
public final class SpeechBubbleRenderer {
    private static final int MAX_LINE_WIDTH = 164;
    private static final int DARK_BACKGROUND = 0xB91A1727;
    private static final int OUTLINE = 0xFF77D7E7;
    private static final int TEXT = 0xFFFFF6E9;
    private static final int FULL_BRIGHT = 0x00F000F0;
    private static final double MAX_RENDER_DISTANCE_SQR = 128.0D * 128.0D;

    private SpeechBubbleRenderer() { }

    public static void renderWorldBubbles(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) return;

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) return;

        PoseStack pose = event.getPoseStack();
        if (pose == null) return;

        Vec3 cameraPosition = event.getCamera().getPosition();
        Font font = minecraft.font;
        var buffers = minecraft.renderBuffers().bufferSource();
        boolean renderedAny = false;

        // Level-stage pose stacks are in world-render space. Translate once by the
        // camera position, then place each billboard at its speaker's current position.
        pose.pushPose();
        pose.translate(-cameraPosition.x, -cameraPosition.y, -cameraPosition.z);

        for (Player player : minecraft.level.players()) {
            if (player.isRemoved() || player.distanceToSqr(cameraPosition) > MAX_RENDER_DISTANCE_SQR) {
                continue;
            }

            String text = SpeechBubbleClientState.visibleText(player.getId());
            if (text.isEmpty()) continue;

            List<FormattedCharSequence> lines = font.split(Component.literal(text), MAX_LINE_WIDTH);
            if (lines.isEmpty()) continue;

            pose.pushPose();
            Vec3 playerPosition = player.position();
            pose.translate(playerPosition.x, playerPosition.y + player.getBbHeight() + 0.38D, playerPosition.z);
            pose.mulPose(minecraft.getEntityRenderDispatcher().cameraOrientation());
            pose.scale(-0.025F, -0.025F, 0.025F);

            float y = -((lines.size() - 1) * 5.0F);
            for (FormattedCharSequence line : lines) {
                float x = -font.width(line) / 2.0F;
                var matrix = pose.last().pose();

                font.drawInBatch(line, x - 1.0F, y, OUTLINE, false, matrix,
                    buffers, Font.DisplayMode.NORMAL, 0, FULL_BRIGHT);
                font.drawInBatch(line, x + 1.0F, y, OUTLINE, false, matrix,
                    buffers, Font.DisplayMode.NORMAL, 0, FULL_BRIGHT);
                font.drawInBatch(line, x, y - 1.0F, OUTLINE, false, matrix,
                    buffers, Font.DisplayMode.NORMAL, 0, FULL_BRIGHT);
                font.drawInBatch(line, x, y + 1.0F, OUTLINE, false, matrix,
                    buffers, Font.DisplayMode.NORMAL, 0, FULL_BRIGHT);
                // Font's background-color argument draws the dark plate behind the line.
                font.drawInBatch(line, x, y, TEXT, false, matrix,
                    buffers, Font.DisplayMode.NORMAL, DARK_BACKGROUND, FULL_BRIGHT);
                y += 10.0F;
            }

            pose.popPose();
            renderedAny = true;
        }

        pose.popPose();

        // Font uses texture-specific render types, so flush the source after all lines
        // have been submitted rather than waiting for an unrelated later render pass.
        if (renderedAny) {
            buffers.endBatch();
        }
    }
}
