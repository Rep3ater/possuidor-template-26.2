package ladysnake.requiem.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import ladysnake.requiem.client.SpeechBubbleClientState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;

import java.util.List;

/**
 * Renders a speech bubble from the player's own render pass. Using RenderPlayerEvent.Post
 * keeps the bubble attached to the exact interpolated player position and avoids the
 * translucent-level render stage, which is unreliable with Fabulous/shader pipelines.
 */
public final class SpeechBubbleRenderer {
    private static final int MAX_LINE_WIDTH = 164;
    private static final int DARK_BACKGROUND = 0xB91A1727;
    private static final int OUTLINE = 0xFF77D7E7;
    private static final int TEXT = 0xFFFFF6E9;
    private static final int FULL_BRIGHT = 0x00F000F0;

    private SpeechBubbleRenderer() { }

    public static void renderPlayerBubble(RenderPlayerEvent.Post event) {
        Player player = event.getEntity();
        String text = SpeechBubbleClientState.visibleText(player.getId());
        if (text.isEmpty() || player.isRemoved()) return;

        Minecraft minecraft = Minecraft.getInstance();
        List<FormattedCharSequence> lines = minecraft.font.split(Component.literal(text), MAX_LINE_WIDTH);
        if (lines.isEmpty()) return;

        PoseStack pose = event.getPoseStack();
        pose.pushPose();
        // RenderPlayerEvent is fired with the dispatcher’s camera-relative entity matrix.
        pose.translate(0.0D, player.getBbHeight() + 0.38D, 0.0D);
        pose.mulPose(minecraft.getEntityRenderDispatcher().cameraOrientation());
        pose.scale(-0.025F, -0.025F, 0.025F);

        Font font = minecraft.font;
        int y = -(lines.size() * 10) - 5;
        for (FormattedCharSequence line : lines) {
            float x = -font.width(line) / 2.0F;
            font.drawInBatch(line, x - 1.0F, y, OUTLINE, false, pose.last().pose(),
                event.getMultiBufferSource(), Font.DisplayMode.NORMAL, 0, FULL_BRIGHT);
            font.drawInBatch(line, x + 1.0F, y, OUTLINE, false, pose.last().pose(),
                event.getMultiBufferSource(), Font.DisplayMode.NORMAL, 0, FULL_BRIGHT);
            font.drawInBatch(line, x, y - 1.0F, OUTLINE, false, pose.last().pose(),
                event.getMultiBufferSource(), Font.DisplayMode.NORMAL, 0, FULL_BRIGHT);
            font.drawInBatch(line, x, y + 1.0F, OUTLINE, false, pose.last().pose(),
                event.getMultiBufferSource(), Font.DisplayMode.NORMAL, 0, FULL_BRIGHT);
            font.drawInBatch(line, x, y, TEXT, false, pose.last().pose(),
                event.getMultiBufferSource(), Font.DisplayMode.NORMAL, DARK_BACKGROUND, FULL_BRIGHT);
            y += 10;
        }
        pose.popPose();
    }
}
