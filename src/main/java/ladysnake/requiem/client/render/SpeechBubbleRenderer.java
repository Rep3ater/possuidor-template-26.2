package ladysnake.requiem.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import ladysnake.requiem.client.SpeechBubbleClientState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;
import net.minecraft.client.renderer.MultiBufferSource;
import java.util.List;

/** World-space, typewriter speech bubbles and an intentionally playful arm gesture. */
public final class SpeechBubbleRenderer {
    private static final int MAX_LINE_WIDTH = 164;
    private static final int DARK_BACKGROUND = 0xB91A1727;
    private static final int OUTLINE = 0xFF77D7E7;
    private static final int TEXT = 0xFFFFF6E9;
    private SpeechBubbleRenderer() { }

    public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;

        Vec3 camera = event.getCamera().getPosition();
        PoseStack pose = event.getPoseStack();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        boolean drew = false;

        for (Player player : mc.level.players()) {
            String text = SpeechBubbleClientState.visibleText(player.getId());
            if (text.isEmpty() || player.isRemoved()) continue;

            List<net.minecraft.util.FormattedCharSequence> lines = mc.font.split(Component.literal(text), MAX_LINE_WIDTH);
            if (lines.isEmpty()) continue;
            pose.pushPose();
            pose.translate(player.getX() - camera.x,
                player.getY() + player.getBbHeight() + 0.36D - camera.y,
                player.getZ() - camera.z);
            pose.mulPose(mc.getEntityRenderDispatcher().cameraOrientation());
            pose.scale(-0.025F, -0.025F, 0.025F);

            Font font = mc.font;
            int y = -(lines.size() * 10) - 5;
            for (net.minecraft.util.FormattedCharSequence line : lines) {
                float x = -font.width(line) / 2.0F;
                font.drawInBatch(line, x - 1.0F, y, OUTLINE, false, pose.last().pose(),
                    buffers, Font.DisplayMode.NORMAL, 0, 0x00F000F0);
                font.drawInBatch(line, x + 1.0F, y, OUTLINE, false, pose.last().pose(),
                    buffers, Font.DisplayMode.NORMAL, 0, 0x00F000F0);
                font.drawInBatch(line, x, y - 1.0F, OUTLINE, false, pose.last().pose(),
                    buffers, Font.DisplayMode.NORMAL, 0, 0x00F000F0);
                font.drawInBatch(line, x, y + 1.0F, OUTLINE, false, pose.last().pose(),
                    buffers, Font.DisplayMode.NORMAL, 0, 0x00F000F0);
                font.drawInBatch(line, x, y, TEXT, false, pose.last().pose(),
                    buffers, Font.DisplayMode.NORMAL, DARK_BACKGROUND, 0x00F000F0);
                y += 10;
            }
            pose.popPose();
            drew = true;
        }
        if (drew) buffers.endBatch();
    }

    public static void animate(RenderPlayerEvent.Pre event) {
        Player player = event.getEntity();
        if (!SpeechBubbleClientState.isGesturing(player.getId())) return;

        PlayerModel<?> model = event.getRenderer().getModel();
        float phase = (player.tickCount + Minecraft.getInstance().getFrameTime()) * 0.72F;
        model.rightArm.xRot = -1.05F + Mth.sin(phase) * 0.42F;
        model.rightArm.yRot = -0.48F + Mth.cos(phase * 0.83F) * 0.48F;
        model.leftArm.xRot = -1.08F + Mth.cos(phase * 1.07F) * 0.38F;
        model.leftArm.yRot = 0.48F + Mth.sin(phase * 0.91F) * 0.44F;
        model.rightArm.zRot = Mth.sin(phase * 0.7F) * 0.12F;
        model.leftArm.zRot = Mth.cos(phase * 0.7F) * 0.12F;
    }
}
