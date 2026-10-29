package ladysnake.requiem.client.gui;

import ladysnake.requiem.common.network.RequiemNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import org.lwjgl.glfw.GLFW;

/** Small node-chain editor for the soul/program carried by a homunculus. */
public final class HomunculusBrainScreen extends Screen {
    private final int entityId;
    private final String currentProgram;
    private EditBox program;

    public HomunculusBrainScreen(int entityId, String currentProgram) {
        super(Component.literal("Homunculus Brain / Soul Jigsaw"));
        this.entityId = entityId;
        this.currentProgram = currentProgram == null ? "" : currentProgram;
    }

    @Override
    protected void init() {
        super.init();
        this.program = new EditBox(this.font, this.width / 2 - 170, this.height / 2 - 8, 340, 22,
            Component.literal("Node sequence"));
        this.program.setMaxLength(1024);
        this.program.setValue(this.currentProgram);
        this.addRenderableWidget(this.program);
    }

    private void save() {
        if (this.program != null) RequiemNetworking.sendHomunculusBrain(this.entityId, this.program.getValue());
        Minecraft.getInstance().setScreenAndShow(null);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        int left = this.width / 2 - 170;
        int right = this.width / 2 + 170;
        int top = this.height / 2 + 35;
        if (event.x() >= left && event.x() <= right && event.y() >= top && event.y() <= top + 30) {
            save();
            return true;
        }
        int tileTop = this.height / 2 + 74;
        int tileHeight = 24;
        if (event.y() >= tileTop && event.y() <= tileTop + tileHeight * 2 + 6 && event.x() >= left && event.x() <= right) {
            int row = event.y() < tileTop + tileHeight ? 0 : (event.y() >= tileTop + tileHeight + 6 ? 1 : -1);
            if (row >= 0) {
                int column = Math.max(0, Math.min(3, (int) ((event.x() - left) / (340.0 / 4.0))));
                int tile = row * 4 + column;
                String[] nodes = {"find item:<id>", "take item", "goto x y z", "find entity:<name>", "kill entity", "return home", "store chest", "drop item"};
                String text = this.program.getValue();
                this.program.setValue(text.isBlank() ? nodes[tile] : text + "; " + nodes[tile]);
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == GLFW.GLFW_KEY_ENTER && (event.modifiers() & GLFW.GLFW_MOD_CONTROL) != 0) {
            save();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, this.width, this.height, 0xF0090D12);
        int center = this.width / 2;
        graphics.text(this.font, Component.literal("HOMUNCULUS — BRAIN / SOUL PROGRAM"), center - 145, this.height / 2 - 75, 0xFFFFE5B4, false);
        graphics.textWithWordWrap(this.font, Component.literal("Build a sequence of nodes. Separate nodes with semicolons. Examples: find item:apple; take item; goto 20 64 -4; store chest; return home. Find entity and kill entity are supported too."), center - 170, this.height / 2 - 55, 340, 0xFFD4DDD8, false);
        drawBox(graphics, center - 170, this.height / 2 + 35, center + 170, this.height / 2 + 65, mouseX >= center - 170 && mouseX <= center + 170 && mouseY >= this.height / 2 + 35 && mouseY <= this.height / 2 + 65, "SAVE BRAIN PROGRAM");
        int left = center - 170;
        String[] nodes = {"FIND ITEM", "TAKE", "GOTO XYZ", "FIND ENTITY", "KILL", "HOME", "STORE", "DROP"};
        for (int i = 0; i < nodes.length; i++) {
            int row = i / 4;
            int column = i % 4;
            int x1 = left + (int) (column * (340.0 / 4.0));
            int x2 = left + (int) ((column + 1) * (340.0 / 4.0));
            int y1 = this.height / 2 + 74 + row * 30;
            int y2 = y1 + 24;
            drawBox(graphics, x1, y1, x2, y2,
                mouseX >= x1 && mouseX <= x2 && mouseY >= y1 && mouseY <= y2, nodes[i]);
        }
        graphics.text(this.font, Component.literal("Click nodes to append them. Replace placeholders; separate nodes with semicolons. Ctrl+Enter saves."), center - 165, this.height / 2 + 140, 0xFF9BA9A6, false);
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    private void drawBox(GuiGraphicsExtractor graphics, int left, int top, int right, int bottom, boolean hovered, String label) {
        graphics.fill(left, top, right, bottom, hovered ? 0xFF41402E : 0xFF171B1C);
        int border = hovered ? 0xFFFFE5B4 : 0xFF566462;
        graphics.fill(left, top, right, top + 1, border);
        graphics.fill(left, bottom - 1, right, bottom, border);
        graphics.fill(left, top, left + 1, bottom, border);
        graphics.fill(right - 1, top, right, bottom, border);
        graphics.text(this.font, Component.literal(label), left + 3, top + (bottom - top - 8) / 2, 0xFFF1F2EC, false);
    }
}
