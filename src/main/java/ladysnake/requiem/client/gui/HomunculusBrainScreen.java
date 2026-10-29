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

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Connected block-chain editor for a homunculus brain program.
 *
 * The chain is stored as semicolon-separated nodes for backwards compatibility
 * with existing homunculi and their saved programs.
 */
public final class HomunculusBrainScreen extends Screen {
    private static final String[] PALETTE_LABELS = {
        "RETURN HOME", "WAIT", "GUARD", "FIND ITEM", "PICKUP", "TAKE ITEM",
        "FIND ENTITY", "KILL", "GOTO XYZ", "STORE CHEST", "DROP ITEM", "KILL TARGET"
    };
    private static final String[] PALETTE_COMMANDS = {
        "return home", "wait", "guard", "find item:<id>", "pickup", "take item",
        "find entity:<name>", "kill", "goto x y z", "store chest", "drop item", "kill:<name>"
    };

    private final int entityId;
    private final String currentProgram;
    private EditBox program;
    private int selectedNode = -1;
    private String validation = "Click a block on the right to connect it to the end of the chain.";

    public HomunculusBrainScreen(int entityId, String currentProgram) {
        super(Component.literal("Homunculus Brain / Soul Jigsaw"));
        this.entityId = entityId;
        this.currentProgram = currentProgram == null ? "" : currentProgram;
    }

    @Override
    protected void init() {
        super.init();
        int left = this.width / 2 - 215;
        this.program = new EditBox(this.font, left, this.height / 2 - 109, 430, 20,
            Component.literal("Connected brain blocks"));
        this.program.setMaxLength(1024);
        this.program.setResponder(value -> this.validation = validationMessage());
        this.program.setValue(this.currentProgram);
        this.addRenderableWidget(this.program);
        this.validation = validationMessage();
    }

    private int left() {
        return this.width / 2 - 215;
    }

    private int chainTop() {
        return this.height / 2 - 75;
    }

    private int controlsTop() {
        return this.height / 2 + 64;
    }

    private List<String> nodes() {
        List<String> result = new ArrayList<>();
        if (this.program == null || this.program.getValue().isBlank()) return result;
        for (String raw : this.program.getValue().split(";")) {
            String node = raw.trim();
            if (!node.isEmpty()) result.add(node);
        }
        return result;
    }

    private void setNodes(List<String> nodes) {
        this.program.setValue(String.join("; ", nodes));
        if (nodes.isEmpty()) this.selectedNode = -1;
        else this.selectedNode = Math.max(0, Math.min(this.selectedNode, nodes.size() - 1));
        this.validation = validationMessage();
    }

    private void appendNode(String node) {
        List<String> chain = nodes();
        chain.add(node);
        this.selectedNode = chain.size() - 1;
        setNodes(chain);
    }

    private void moveSelected(int offset) {
        List<String> chain = nodes();
        if (this.selectedNode < 0 || this.selectedNode >= chain.size()) return;
        int destination = this.selectedNode + offset;
        if (destination < 0 || destination >= chain.size()) return;
        String node = chain.remove(this.selectedNode);
        chain.add(destination, node);
        this.selectedNode = destination;
        setNodes(chain);
    }

    private void removeSelected() {
        List<String> chain = nodes();
        if (this.selectedNode < 0 || this.selectedNode >= chain.size()) return;
        chain.remove(this.selectedNode);
        if (chain.isEmpty()) this.selectedNode = -1;
        else this.selectedNode = Math.min(this.selectedNode, chain.size() - 1);
        setNodes(chain);
    }

    private void autoFix() {
        List<String> chain = nodes();
        List<String> fixed = new ArrayList<>(chain.size());
        for (String node : chain) fixed.add(autoFixNode(node));
        setNodes(fixed);
    }

    private static String autoFixNode(String raw) {
        String node = raw.trim().replaceAll("\\s+", " ");
        String lower = node.toLowerCase(Locale.ROOT);
        if (lower.isBlank()) return "";
        if (lower.equals("home") || lower.equals("go home")) return "return home";
        if (lower.equals("take")) return "take item";
        if (lower.equals("drop")) return "drop item";
        if (lower.equals("store")) return "store chest";
        if (lower.equals("pickup item")) return "pickup";
        if (lower.startsWith("pickup ") && !lower.equals("pickup item")) {
            return "pickup:" + node.substring("pickup ".length()).trim();
        }
        if (lower.startsWith("find item ") && !lower.startsWith("find item:")) {
            return "find item:" + node.substring("find item ".length()).trim();
        }
        if (lower.startsWith("find entity ") && !lower.startsWith("find entity:")) {
            return "find entity:" + node.substring("find entity ".length()).trim();
        }
        if (lower.startsWith("kill entity ")) {
            return "kill:" + node.substring("kill entity ".length()).trim();
        }
        if (lower.startsWith("kill ") && !lower.equals("kill entity")) {
            return "kill:" + node.substring("kill ".length()).trim();
        }
        if (lower.equals("kill:")) return "kill";
        if (lower.equals("pickup:")) return "pickup";
        if (lower.equals("find item:")) return "find item";
        if (lower.equals("find entity:")) return "find entity";
        return node;
    }

    private List<String> validationErrors() {
        List<String> errors = new ArrayList<>();
        List<String> chain = nodes();
        if (chain.isEmpty()) {
            errors.add("The chain is empty; saving will restore the default RETURN HOME block.");
            return errors;
        }
        for (int i = 0; i < chain.size(); i++) {
            String node = chain.get(i);
            String lower = node.toLowerCase(Locale.ROOT);
            boolean known = lower.equals("return home") || lower.equals("home")
                || lower.equals("wait") || lower.equals("guard")
                || lower.equals("take") || lower.equals("take item")
                || lower.equals("pickup") || lower.equals("pickup item") || lower.startsWith("pickup:")
                || lower.equals("drop") || lower.equals("drop item")
                || lower.equals("store chest") || lower.startsWith("store chest")
                || lower.equals("find item") || lower.startsWith("find item:")
                || lower.equals("find entity") || lower.startsWith("find entity:")
                || lower.equals("kill") || lower.equals("kill entity") || lower.startsWith("kill:");
            if (lower.startsWith("goto ")) {
                String[] coords = lower.substring(5).trim().split("\\s+");
                known = coords.length == 3;
                if (known) {
                    try {
                        Integer.parseInt(coords[0]);
                        Integer.parseInt(coords[1]);
                        Integer.parseInt(coords[2]);
                    } catch (NumberFormatException invalid) {
                        known = false;
                    }
                }
            } else if (lower.equals("goto")) {
                known = false;
            }
            if (!known) errors.add("Block " + (i + 1) + " needs fixing: " + node);
            if (lower.contains("<id>") || lower.contains("<name>") || lower.contains("x y z")) {
                errors.add("Block " + (i + 1) + " still has a placeholder: " + node);
            }
        }
        return errors;
    }

    private String validationMessage() {
        List<String> errors = validationErrors();
        if (errors.isEmpty()) {
            return "Valid chain. PICKUP and KILL without a target choose the nearest eligible target.";
        }
        String first = errors.get(0);
        return errors.size() == 1 ? first : first + "  (+" + (errors.size() - 1) + " more)";
    }

    private void save() {
        if (this.program == null) return;
        autoFix();
        List<String> errors = validationErrors();
        if (!errors.isEmpty() && !nodes().isEmpty()) {
            this.validation = errors.get(0) + " — use AUTO-FIX or correct the highlighted command.";
            return;
        }
        RequiemNetworking.sendHomunculusBrain(this.entityId, String.join("; ", nodes()));
        Minecraft.getInstance().setScreenAndShow(null);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        int left = left();
        int centerY = this.height / 2;
        int chainX = left;
        int chainTop = chainTop();
        int chainWidth = 210;
        int rowHeight = 22;

        if (event.x() >= chainX && event.x() <= chainX + chainWidth
                && event.y() >= chainTop && event.y() <= chainTop + 6 * rowHeight) {
            int row = (int) ((event.y() - chainTop) / rowHeight);
            List<String> chain = nodes();
            if (row < chain.size() && row < 6) {
                this.selectedNode = row;
                this.validation = validationMessage();
                return true;
            }
        }

        int paletteX = left + 230;
        int paletteTop = chainTop;
        int paletteWidth = 200;
        if (event.x() >= paletteX && event.x() <= paletteX + paletteWidth
                && event.y() >= paletteTop && event.y() <= paletteTop + 6 * rowHeight) {
            int row = (int) ((event.y() - paletteTop) / rowHeight);
            int column = event.x() < paletteX + paletteWidth / 2 ? 0 : 1;
            int tile = row * 2 + column;
            if (tile >= 0 && tile < PALETTE_COMMANDS.length) {
                appendNode(PALETTE_COMMANDS[tile]);
                return true;
            }
        }

        int controlsY = controlsTop();
        if (event.y() >= controlsY && event.y() <= controlsY + 24
                && event.x() >= left && event.x() <= left + 430) {
            int control = Math.min(4, Math.max(0, (int) ((event.x() - left) / (430.0 / 5.0))));
            switch (control) {
                case 0 -> moveSelected(-1);
                case 1 -> moveSelected(1);
                case 2 -> removeSelected();
                case 3 -> autoFix();
                case 4 -> setNodes(new ArrayList<>());
                default -> { }
            }
            this.validation = validationMessage();
            return true;
        }

        int saveY = controlsY + 29;
        if (event.x() >= left && event.x() <= left + 430
                && event.y() >= saveY && event.y() <= saveY + 27) {
            save();
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == GLFW.GLFW_KEY_ENTER && (event.modifiers() & GLFW.GLFW_MOD_CONTROL) != 0) {
            save();
            return true;
        }
        if (event.key() == GLFW.GLFW_KEY_DELETE && this.program != null) {
            removeSelected();
            return true;
        }
        if (event.key() == GLFW.GLFW_KEY_UP && (event.modifiers() & GLFW.GLFW_MOD_CONTROL) != 0) {
            moveSelected(-1);
            return true;
        }
        if (event.key() == GLFW.GLFW_KEY_DOWN && (event.modifiers() & GLFW.GLFW_MOD_CONTROL) != 0) {
            moveSelected(1);
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, this.width, this.height, 0xF0090D12);
        int center = this.width / 2;
        int left = left();
        int chainTop = chainTop();
        int rowHeight = 22;
        int chainWidth = 210;
        int paletteX = left + 230;

        graphics.text(this.font, Component.literal("HOMUNCULUS — BRAIN / SOUL JIGSAW"), left, center - 153, 0xFFFFE5B4, false);
        graphics.textWithWordWrap(this.font, Component.literal("Each block is connected in order. Edit targets or coordinates in the chain field; use AUTO-FIX to normalize common commands."), left, center - 136, 430, 0xFFD4DDD8, false);

        List<String> chain = nodes();
        List<String> errors = validationErrors();
        boolean valid = errors.isEmpty();
        graphics.text(this.font, Component.literal("CONNECTED CHAIN (" + chain.size() + ")"), left, chainTop - 13, 0xFFC5D7D1, false);
        graphics.text(this.font, Component.literal("BLOCK PALETTE"), paletteX, chainTop - 13, 0xFFC5D7D1, false);

        for (int i = 0; i < 6; i++) {
            int y = chainTop + i * rowHeight;
            if (i < chain.size()) {
                String label = (i + 1) + ". " + chain.get(i);
                if (label.length() > 29) label = label.substring(0, 26) + "...";
                boolean hovered = mouseX >= left && mouseX <= left + chainWidth && mouseY >= y && mouseY <= y + 20;
                drawBox(graphics, left, y, left + chainWidth, y + 20,
                    this.selectedNode == i || hovered, label);
                if (i < chain.size() - 1) {
                    graphics.fill(left + chainWidth / 2 - 1, y + 20, left + chainWidth / 2 + 1, y + rowHeight, 0xFF8FA89D);
                }
            } else {
                graphics.fill(left, y, left + chainWidth, y + 20, 0xFF111617);
                graphics.fill(left, y, left + chainWidth, y + 1, 0xFF303A3B);
            }
            int paletteIndexLeft = i * 2;
            int paletteIndexRight = paletteIndexLeft + 1;
            int tileW = 97;
            if (paletteIndexLeft < PALETTE_LABELS.length) {
                int x1 = paletteX;
                int x2 = paletteX + tileW;
                boolean hovered = mouseX >= x1 && mouseX <= x2 && mouseY >= y && mouseY <= y + 20;
                drawBox(graphics, x1, y, x2, y + 20, hovered, PALETTE_LABELS[paletteIndexLeft]);
            }
            if (paletteIndexRight < PALETTE_LABELS.length) {
                int x1 = paletteX + 101;
                int x2 = paletteX + 200;
                boolean hovered = mouseX >= x1 && mouseX <= x2 && mouseY >= y && mouseY <= y + 20;
                drawBox(graphics, x1, y, x2, y + 20, hovered, PALETTE_LABELS[paletteIndexRight]);
            }
        }

        int controlsY = controlsTop();
        String[] controls = {"UP", "DOWN", "REMOVE", "AUTO-FIX", "CLEAR"};
        for (int i = 0; i < controls.length; i++) {
            int x1 = left + (int) (i * (430.0 / controls.length));
            int x2 = left + (int) ((i + 1) * (430.0 / controls.length));
            boolean hovered = mouseX >= x1 && mouseX <= x2 && mouseY >= controlsY && mouseY <= controlsY + 24;
            drawBox(graphics, x1, controlsY, x2, controlsY + 24, hovered, controls[i]);
        }

        int saveY = controlsY + 29;
        boolean saveHovered = mouseX >= left && mouseX <= left + 430 && mouseY >= saveY && mouseY <= saveY + 27;
        drawBox(graphics, left, saveY, left + 430, saveY + 27, saveHovered, "VALIDATE & SAVE BRAIN");

        int validationColor = valid ? 0xFFB9E7B5 : 0xFFFF958A;
        String displayValidation = this.validation == null || this.validation.isBlank() ? validationMessage() : this.validation;
        if (displayValidation.length() > 86) displayValidation = displayValidation.substring(0, 83) + "...";
        graphics.text(this.font, Component.literal(displayValidation), left, saveY + 33, validationColor, false);
        graphics.text(this.font, Component.literal("Select a connected block to reorder or delete it. Ctrl+Enter saves; Ctrl+Arrow moves the selected block."), left, saveY + 46, 0xFF9BA9A6, false);

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
