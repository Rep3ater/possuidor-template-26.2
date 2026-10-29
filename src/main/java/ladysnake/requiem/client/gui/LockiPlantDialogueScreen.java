package ladysnake.requiem.client.gui;

import ladysnake.requiem.common.network.RequiemNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import org.lwjgl.glfw.GLFW;

/** A deliberately stark, black-screen conversation with a tree spirit. */
public final class LockiPlantDialogueScreen extends Screen {
    private int hoveredChoice = -1;

    public LockiPlantDialogueScreen() {
        super(Component.literal("A Plant's Spirit"));
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    private void choose(int choice) {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK.value(), 0.75F));
        RequiemNetworking.sendLockiPlantChoice(choice);
    }

    private int choiceAt(double x, double y) {
        int left = this.width / 2 - 170;
        int right = this.width / 2 + 170;
        int top = this.height / 2 + 42;
        if (x < left || x > right) return -1;
        if (y >= top && y <= top + 32) return 1;
        if (y >= top + 42 && y <= top + 74) return 0;
        return -1;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int choice = choiceAt(mouseX, mouseY);
            if (choice >= 0) {
                choose(choice);
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_1 || keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_SPACE) {
            choose(1);
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_2 || keyCode == GLFW.GLFW_KEY_BACKSPACE) {
            choose(0);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, this.width, this.height, 0xFF000000);
        int center = this.width / 2;
        graphics.drawString(this.font, Component.literal("A voice answers from somewhere inside the plant."), center - 170, this.height / 2 - 76, 0xFFE5EDE9, false);
        graphics.drawWordWrap(this.font, Component.literal("You feel the roots remembering a thousand springs. The plant's spirit asks what you would become if it lent you one of its children."), center - 170, this.height / 2 - 50, 340, 0xFFB9C8C4);

        int top = this.height / 2 + 42;
        boolean first = choiceAt(mouseX, mouseY) == 1;
        boolean second = choiceAt(mouseX, mouseY) == 0;
        drawChoice(graphics, center - 170, top, center + 170, top + 32, first,
            Component.literal("[1]  Ask to become one of its children"));
        drawChoice(graphics, center - 170, top + 42, center + 170, top + 74, second,
            Component.literal("[2]  Refuse and return to the roots"));
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void drawChoice(GuiGraphics graphics, int left, int top, int right, int bottom, boolean hovered, Component text) {
        int border = hovered ? 0xFFBDEFF5 : 0xFF34514C;
        int fill = hovered ? 0xFF183B37 : 0xFF090F0E;
        graphics.fill(left, top, right, bottom, fill);
        graphics.fill(left, top, right, top + 1, border);
        graphics.fill(left, bottom - 1, right, bottom, border);
        graphics.fill(left, top, left + 1, bottom, border);
        graphics.fill(right - 1, top, right, bottom, border);
        graphics.drawString(this.font, text, left + 10, top + 11, hovered ? 0xFFFFFFFF : 0xFFCAD8D4, false);
    }
}
