package ladysnake.requiem.client.render;

import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import ladysnake.requiem.client.SpeechBubbleClientState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;

import java.util.List;

/**
 * Renders active speech bubbles:
 * 1) In-world 3D billboards above players for third-person view and other viewers.
 * 2) On-screen HUD bubble for the local player when in first-person view.
 *
 * Uses a dedicated, isolated buffer source to ensure entity rendering buffers
 * (such as player skin layers) are never touched or corrupted.
 */
public final class SpeechBubbleRenderer {
    private static final int MAX_LINE_WIDTH = 164;
    private static final int DARK_BACKGROUND = 0xB91A1727;
    private static final int OUTLINE = 0xFF77D7E7;
    private static final int TEXT = 0xFFFFFFFF; // Emissive bright white
    private static final int FULL_BRIGHT = 0x00F000F0;
    private static final double MAX_RENDER_DISTANCE_SQR = 128.0D * 128.0D;

    /** Isolated buffer source to prevent flushing or corrupting player skin / entity buffers. */
    private static final MultiBufferSource.BufferSource BUBBLE_BUFFERS =
        MultiBufferSource.immediate(new ByteBufferBuilder(262144));

    private SpeechBubbleRenderer() { }

    private static void drawQuad(VertexConsumer consumer, Matrix4f matrix, float x0, float y0, float x1, float y1, float z, int color) {
        consumer.addVertex(matrix, x0, y0, z).setColor(color).setLight(FULL_BRIGHT);
        consumer.addVertex(matrix, x0, y1, z).setColor(color).setLight(FULL_BRIGHT);
        consumer.addVertex(matrix, x1, y1, z).setColor(color).setLight(FULL_BRIGHT);
        consumer.addVertex(matrix, x1, y0, z).setColor(color).setLight(FULL_BRIGHT);
    }

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
        MultiBufferSource.BufferSource buffers = BUBBLE_BUFFERS;
        boolean renderedAny = false;
        float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(false);

        for (Player player : minecraft.level.players()) {
            if (player.isRemoved() || player.distanceToSqr(cameraPosition) > MAX_RENDER_DISTANCE_SQR) {
                continue;
            }

            String text = SpeechBubbleClientState.visibleText(player.getId());
            if (text.isEmpty()) continue;

            List<FormattedCharSequence> lines = font.split(Component.literal(text), MAX_LINE_WIDTH);
            if (lines.isEmpty()) continue;

            int maxLineWidth = 0;
            for (FormattedCharSequence line : lines) {
                maxLineWidth = Math.max(maxLineWidth, font.width(line));
            }

            int outlineColor = SpeechBubbleClientState.color(player.getId());
            var theme = ladysnake.requiem.common.SpeechBubbleColor.getTheme(outlineColor);
            int currentBg = theme.backgroundColor();
            int currentOutline = theme.outlineColor();

            double px = Mth.lerp((double) partialTick, player.xo, player.getX()) - cameraPosition.x;
            double py = Mth.lerp((double) partialTick, player.yo, player.getY()) - cameraPosition.y;
            double pz = Mth.lerp((double) partialTick, player.zo, player.getZ()) - cameraPosition.z;

            pose.pushPose();
            int lineSpacing = 11;
            float hPad = 8.0F;
            float vPad = 6.0F;
            float boxWidth = maxLineWidth + hPad * 2.0F;
            float boxHeight = (lines.size() - 1) * lineSpacing + 9 + vPad * 2.0F;
            float tailHeight = 4.0F;

            // Position above player head and nametag
            pose.translate(px, py + player.getBbHeight() + 0.65D + (boxHeight * 0.025D * 0.5D), pz);
            pose.mulPose(minecraft.getEntityRenderDispatcher().cameraOrientation());
            pose.scale(0.025F, -0.025F, 0.025F);

            float left = -boxWidth / 2.0F;
            float right = boxWidth / 2.0F;
            float top = -boxHeight / 2.0F - tailHeight / 2.0F;
            float bottom = top + boxHeight;
            var matrix = pose.last().pose();

            VertexConsumer bgConsumer = buffers.getBuffer(RenderType.textBackgroundSeeThrough());

            // 1. Background box and pointer tail (SEE_THROUGH)
            drawQuad(bgConsumer, matrix, left, top, right, bottom, 0.02F, currentBg);
            drawQuad(bgConsumer, matrix, -3.0F, bottom, 3.0F, bottom + tailHeight, 0.02F, currentBg);

            // 2. Themed border outline (SEE_THROUGH)
            float border = 1.0F;
            drawQuad(bgConsumer, matrix, left - border, top - border, right + border, top, 0.015F, currentOutline);
            drawQuad(bgConsumer, matrix, left - border, bottom, -3.0F, bottom + border, 0.015F, currentOutline);
            drawQuad(bgConsumer, matrix, 3.0F, bottom, right + border, bottom + border, 0.015F, currentOutline);
            drawQuad(bgConsumer, matrix, left - border, top, left, bottom, 0.015F, currentOutline);
            drawQuad(bgConsumer, matrix, right, top, right + border, bottom, 0.015F, currentOutline);
            drawQuad(bgConsumer, matrix, -3.0F - border, bottom, -3.0F, bottom + tailHeight, 0.015F, currentOutline);
            drawQuad(bgConsumer, matrix, 3.0F, bottom, 3.0F + border, bottom + tailHeight, 0.015F, currentOutline);
            drawQuad(bgConsumer, matrix, -3.0F, bottom + tailHeight, 3.0F, bottom + tailHeight + border, 0.015F, currentOutline);

            // 3. Draw text lines
            float curY = top + vPad;
            for (FormattedCharSequence line : lines) {
                float curX = -font.width(line) / 2.0F;

                font.drawInBatch(line, curX, curY, TEXT, false, matrix,
                    buffers, Font.DisplayMode.SEE_THROUGH, 0, FULL_BRIGHT);
                font.drawInBatch(line, curX, curY, TEXT, false, matrix,
                    buffers, Font.DisplayMode.NORMAL, 0, FULL_BRIGHT);

                curY += lineSpacing;
            }

            pose.popPose();
            renderedAny = true;
        }

        if (renderedAny) {
            buffers.endBatch();
        }
    }

    /**
     * Renders a HUD speech bubble for the local player when they are in first-person view,
     * ensuring they can see what they are speaking with ample space, rounded bubble styling,
     * and a downward tail pointer.
     */
    public static void renderHudBubble(GuiGraphics graphics) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        if (!mc.options.getCameraType().isFirstPerson()) return;

        String text = SpeechBubbleClientState.visibleText(mc.player.getId());
        if (text.isEmpty()) return;

        Font font = mc.font;
        List<FormattedCharSequence> lines = font.split(Component.literal(text), 230);
        if (lines.isEmpty()) return;

        int maxLineWidth = 0;
        for (FormattedCharSequence line : lines) {
            maxLineWidth = Math.max(maxLineWidth, font.width(line));
        }

        int hPadding = 14;
        int vPadding = 9;
        int lineSpacing = 12;
        int boxWidth = Math.max(maxLineWidth + hPadding * 2, 70);
        int boxHeight = (lines.size() - 1) * lineSpacing + 9 + vPadding * 2;

        int outlineColor = SpeechBubbleClientState.color(mc.player.getId());
        var theme = ladysnake.requiem.common.SpeechBubbleColor.getTheme(outlineColor);
        int currentBg = theme.backgroundColor();
        int currentOutline = theme.outlineColor();

        int screenWidth = graphics.guiWidth();
        int screenHeight = graphics.guiHeight();

        // Position centered horizontally, comfortably above hotbar & status bars
        int x = (screenWidth - boxWidth) / 2;
        int tailHeight = 6;
        int y = screenHeight - 65 - boxHeight - tailHeight;
        if (y < 12) y = 12;

        int tailX = x + boxWidth / 2;
        int bottom = y + boxHeight;

        // 1. Draw rounded dark background body
        graphics.fill(x + 2, y, x + boxWidth - 2, bottom, currentBg);
        graphics.fill(x, y + 2, x + boxWidth, bottom - 2, currentBg);
        graphics.fill(x + 1, y + 1, x + 2, y + 2, currentBg);
        graphics.fill(x + boxWidth - 2, y + 1, x + boxWidth - 1, y + 2, currentBg);
        graphics.fill(x + 1, bottom - 2, x + 2, bottom - 1, currentBg);
        graphics.fill(x + boxWidth - 2, bottom - 2, x + boxWidth - 1, bottom - 1, currentBg);

        // 2. Draw speech bubble tail (downward pointer pointing toward the speaker/hotbar)
        for (int i = 0; i < tailHeight; i++) {
            int w = (tailHeight - i);
            graphics.fill(tailX - w, bottom + i, tailX + w, bottom + i + 1, currentBg);
        }

        // 3. Draw themed outline around rounded bubble and tail
        // Top border
        graphics.fill(x + 2, y, x + boxWidth - 2, y + 1, currentOutline);
        // Corner bevels
        graphics.fill(x + 1, y + 1, x + 2, y + 2, currentOutline);
        graphics.fill(x + boxWidth - 2, y + 1, x + boxWidth - 1, y + 2, currentOutline);
        graphics.fill(x + 1, bottom - 2, x + 2, bottom - 1, currentOutline);
        graphics.fill(x + boxWidth - 2, bottom - 2, x + boxWidth - 1, bottom - 1, currentOutline);
        // Left & right borders
        graphics.fill(x, y + 2, x + 1, bottom - 2, currentOutline);
        graphics.fill(x + boxWidth - 1, y + 2, x + boxWidth, bottom - 2, currentOutline);
        // Bottom border (split around tail)
        int tailBaseWidth = tailHeight;
        graphics.fill(x + 2, bottom - 1, tailX - tailBaseWidth, bottom, currentOutline);
        graphics.fill(tailX + tailBaseWidth, bottom - 1, x + boxWidth - 2, bottom, currentOutline);
        // Tail angled border
        for (int i = 0; i < tailHeight; i++) {
            int w = (tailHeight - i);
            graphics.fill(tailX - w - 1, bottom + i, tailX - w, bottom + i + 1, currentOutline);
            graphics.fill(tailX + w, bottom + i, tailX + w + 1, bottom + i + 1, currentOutline);
        }
        graphics.fill(tailX, bottom + tailHeight, tailX + 1, bottom + tailHeight + 1, currentOutline);

        // 4. Render typewriter text lines centered with generous spacing
        int textY = y + vPadding;
        for (FormattedCharSequence line : lines) {
            int textX = x + (boxWidth - font.width(line)) / 2;
            graphics.drawString(font, line, textX, textY, TEXT, false);
            textY += lineSpacing;
        }
    }
}
