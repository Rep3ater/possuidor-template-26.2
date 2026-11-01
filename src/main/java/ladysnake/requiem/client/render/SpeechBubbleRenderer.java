package ladysnake.requiem.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import ladysnake.requiem.client.SpeechBubbleClientState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

import java.util.List;

/**
 * Renders active speech bubbles:
 * 1) In-world 3D billboards above players for third-person view and other viewers.
 * 2) On-screen HUD bubble for the local player when in first-person view.
 */
public final class SpeechBubbleRenderer {
    private static final int MAX_LINE_WIDTH = 164;
    private static final int DARK_BACKGROUND = 0xB91A1727;
    private static final int OUTLINE = 0xFF77D7E7;
    private static final int TEXT = 0xFFFFF6E9;
    private static final int FULL_BRIGHT = 0x00F000F0;
    private static final double MAX_RENDER_DISTANCE_SQR = 128.0D * 128.0D;

    private SpeechBubbleRenderer() { }

    /**
     * Renders 3D in-world speech bubbles above players after entities are drawn.
     */
    public static void renderWorldBubbles(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) return;

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) return;

        PoseStack pose = event.getPoseStack();
        if (pose == null) return;

        Vec3 cameraPosition = event.getCamera().getPosition();
        Font font = minecraft.font;
        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
        float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(false);

        for (Player player : minecraft.level.players()) {
            if (player.isRemoved() || player.distanceToSqr(cameraPosition) > MAX_RENDER_DISTANCE_SQR) {
                continue;
            }

            String text = SpeechBubbleClientState.visibleText(player.getId());
            if (text.isEmpty()) continue;

            List<FormattedCharSequence> lines = font.split(Component.literal(text), MAX_LINE_WIDTH);
            if (lines.isEmpty()) continue;

            double px = Mth.lerp((double) partialTick, player.xo, player.getX()) - cameraPosition.x;
            double py = Mth.lerp((double) partialTick, player.yo, player.getY()) - cameraPosition.y;
            double pz = Mth.lerp((double) partialTick, player.zo, player.getZ()) - cameraPosition.z;

            pose.pushPose();
            // Position safely above player head and nametag
            pose.translate(px, py + player.getBbHeight() + 0.65D + (lines.size() * 0.12D), pz);
            pose.mulPose(minecraft.getEntityRenderDispatcher().cameraOrientation());
            // Positive X scale to maintain standard counter-clockwise polygon winding and prevent back-face culling
            pose.scale(0.025F, -0.025F, 0.025F);

            float y = -((lines.size() - 1) * 5.0F);
            for (FormattedCharSequence line : lines) {
                float x = -font.width(line) / 2.0F;
                var matrix = pose.last().pose();

                // 1. Draw outline using SEE_THROUGH to avoid depth clipping
                font.drawInBatch(line, x - 1.0F, y, OUTLINE, false, matrix,
                    buffers, Font.DisplayMode.SEE_THROUGH, 0, FULL_BRIGHT);
                font.drawInBatch(line, x + 1.0F, y, OUTLINE, false, matrix,
                    buffers, Font.DisplayMode.SEE_THROUGH, 0, FULL_BRIGHT);
                font.drawInBatch(line, x, y - 1.0F, OUTLINE, false, matrix,
                    buffers, Font.DisplayMode.SEE_THROUGH, 0, FULL_BRIGHT);
                font.drawInBatch(line, x, y + 1.0F, OUTLINE, false, matrix,
                    buffers, Font.DisplayMode.SEE_THROUGH, 0, FULL_BRIGHT);

                // 2. Draw dark background plate and base text using SEE_THROUGH
                font.drawInBatch(line, x, y, TEXT, false, matrix,
                    buffers, Font.DisplayMode.SEE_THROUGH, DARK_BACKGROUND, FULL_BRIGHT);

                // 3. Draw foreground text with NORMAL for full crispness when unobstructed
                font.drawInBatch(line, x, y, TEXT, false, matrix,
                    buffers, Font.DisplayMode.NORMAL, 0, FULL_BRIGHT);

                y += 10.0F;
            }

            pose.popPose();
        }
    }

    /**
     * Renders a HUD speech bubble for the local player when they are in first-person view,
     * ensuring they can see what they are speaking without having to tilt their view directly overhead.
     */
    public static void renderHudBubble(GuiGraphics graphics) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        if (!mc.options.getCameraType().isFirstPerson()) return;

        String text = SpeechBubbleClientState.visibleText(mc.player.getId());
        if (text.isEmpty()) return;

        Font font = mc.font;
        List<FormattedCharSequence> lines = font.split(Component.literal(text), 220);
        if (lines.isEmpty()) return;

        int maxLineWidth = 0;
        for (FormattedCharSequence line : lines) {
            maxLineWidth = Math.max(maxLineWidth, font.width(line));
        }

        int padding = 6;
        int boxWidth = maxLineWidth + padding * 2;
        int boxHeight = lines.size() * 10 + padding * 2;

        int screenWidth = graphics.guiWidth();
        int screenHeight = graphics.guiHeight();

        // Position centered horizontally, above the hotbar and status bars
        int x = (screenWidth - boxWidth) / 2;
        int y = screenHeight - 68 - boxHeight;
        if (y < 12) y = 12;

        // Background plate and cyan outline
        graphics.fill(x, y, x + boxWidth, y + boxHeight, DARK_BACKGROUND);
        graphics.renderOutline(x, y, boxWidth, boxHeight, OUTLINE);

        // Render typewriter text lines
        int textY = y + padding;
        for (FormattedCharSequence line : lines) {
            int textX = x + (boxWidth - font.width(line)) / 2;
            graphics.drawString(font, line, textX, textY, TEXT, false);
            textY += 10;
        }
    }
}
