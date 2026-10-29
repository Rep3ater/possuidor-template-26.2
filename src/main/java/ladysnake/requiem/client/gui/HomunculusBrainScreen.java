package ladysnake.requiem.client.gui;

import ladysnake.requiem.common.network.RequiemNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
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
        "FIND ENTITY", "KILL", "GOTO XYZ", "STORE CHEST", "DROP ITEM", "KILL TARGET", "SAY"
    };
    private static final String[] PALETTE_COMMANDS = {
        "return home", "wait", "guard", "find item:<id>", "pickup", "take item",
        "find entity:<name>", "kill", "goto x y z", "store chest", "drop item", "kill:<name>", "say:<text>"
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
        this.program.setHint(Component.literal("Separate commands with ; — e.g. goto 10 64 -4; kill:Chicken; say:Hello there"));
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
        return this.height / 2 + 86;
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
        if (lower.equals("say")) return "say:";
        if (lower.startsWith("say ")) return "say:" + node.substring("say ".length()).trim();
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
            errors.add("The chain is empty; saving restores the default RETURN HOME command.");
            return errors;
        }
        for (int i = 0; i < chain.size(); i++) {
            String node = chain.get(i);
            String lower = node.toLowerCase(Locale.ROOT);
            boolean known = lower.equals("return home") || lower.equals("home") || lower.equals("go home")
                || lower.equals("wait") || lower.equals("guard")
                || lower.equals("take") || lower.equals("take item")
                || lower.equals("pickup") || lower.equals("pickup item") || lower.startsWith("pickup:") || lower.startsWith("pickup ")
                || lower.equals("drop") || lower.equals("drop item")
                || lower.equals("store") || lower.equals("store chest") || lower.startsWith("store chest ")
                || lower.equals("find item") || lower.startsWith("find item:") || lower.startsWith("find item ")
                || lower.equals("find entity") || lower.startsWith("find entity:") || lower.startsWith("find entity ")
                || lower.equals("kill") || lower.equals("kill entity") || lower.startsWith("kill:") || lower.startsWith("kill ")
                || lower.equals("say") || lower.startsWith("say:") || lower.startsWith("say ");

            if (lower.equals("goto") || lower.startsWith("goto ")) {
                String[] coords = lower.equals("goto") ? new String[0] : lower.substring(5).trim().split("\\s+");
                boolean validCoordinates = coords.length == 3;
                if (validCoordinates) {
                    try {
                        Integer.parseInt(coords[0]);
                        Integer.parseInt(coords[1]);
                        Integer.parseInt(coords[2]);
                    } catch (NumberFormatException invalid) {
                        validCoordinates = false;
                    }
                }
                if (!validCoordinates) {
                    errors.add("Command " + (i + 1) + ": expected `goto X Y Z` with three whole numbers (example: `goto 10 64 -4`).");
                    known = true;
                } else {
                    known = true;
                }
            }

            if (!known) {
                errors.add("Command " + (i + 1) + ": unknown command `" + node + "`. Use return home, wait, guard, goto X Y Z, find item:<item id>, pickup[:<item id>], take item, find entity:<name>, kill[:<name>], say:<text>, store chest, or drop item.");
            }
            if (lower.startsWith("say:") && lower.substring("say:".length()).trim().isEmpty()) {
                errors.add("Command " + (i + 1) + ": SAY needs message text after the colon, for example `say:Hello there`.");
            }
            if (lower.equals("say") || lower.equals("say ")) {
                errors.add("Command " + (i + 1) + ": use `say:<text>`, for example `say:Hello there`.");
            }
            if (lower.contains("<id>") || lower.contains("<name>") || lower.contains("<text>") || lower.contains("x y z")) {
                errors.add("Command " + (i + 1) + ": replace the placeholder with a real item/entity name, message text, or three numeric coordinates.");
            }
        }
        return errors;
    }

    private String validationMessage() {
        List<String> errors = validationErrors();
        if (errors.isEmpty()) {
            return "Valid chain. SAY broadcasts the mob's text to nearby players; PICKUP and KILL can select the nearest eligible target.";
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
        Minecraft.getInstance().setScreen(null);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int left = left();
        int centerY = this.height / 2;
        int chainX = left;
        int chainTop = chainTop();
        int chainWidth = 210;
        int rowHeight = 22;

        if (mouseX >= chainX && mouseX <= chainX + chainWidth
                && mouseY >= chainTop && mouseY <= chainTop + 6 * rowHeight) {
            int row = (int) ((mouseY - chainTop) / rowHeight);
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
        if (mouseX >= paletteX && mouseX <= paletteX + paletteWidth
                && mouseY >= paletteTop && mouseY <= paletteTop + 7 * rowHeight) {
            int row = (int) ((mouseY - paletteTop) / rowHeight);
            int column = mouseX < paletteX + paletteWidth / 2 ? 0 : 1;
            int tile = row * 2 + column;
            if (tile >= 0 && tile < PALETTE_COMMANDS.length) {
                appendNode(PALETTE_COMMANDS[tile]);
                return true;
            }
        }

        int controlsY = controlsTop();
        if (mouseY >= controlsY && mouseY <= controlsY + 24
                && mouseX >= left && mouseX <= left + 430) {
            int control = Math.min(4, Math.max(0, (int) ((mouseX - left) / (430.0 / 5.0))));
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
        if (mouseX >= left && mouseX <= left + 430
                && mouseY >= saveY && mouseY <= saveY + 27) {
            save();
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ENTER && (modifiers & GLFW.GLFW_MOD_CONTROL) != 0) {
            save();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_DELETE && this.program != null) {
            removeSelected();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_UP && (modifiers & GLFW.GLFW_MOD_CONTROL) != 0) {
            moveSelected(-1);
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_DOWN && (modifiers & GLFW.GLFW_MOD_CONTROL) != 0) {
            moveSelected(1);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, this.width, this.height, 0xF0090D12);
        int center = this.width / 2;
        int left = left();
        int chainTop = chainTop();
        int rowHeight = 22;
        int chainWidth = 210;
        int paletteX = left + 230;

        graphics.drawString(this.font, Component.literal("HOMUNCULUS — BRAIN / SOUL JIGSAW"), left, center - 153, 0xFFFFE5B4, false);
        graphics.drawWordWrap(this.font, Component.literal("Use the command line, or click a palette entry to append it. Separate commands with ;. Examples: goto 10 64 -4, find item:minecraft:diamond, find entity:Chicken, kill:Zombie. Validation below explains mistakes; AUTO-FIX normalizes common forms."), left, center - 136, 430, 0xFFD4DDD8);

        List<String> chain = nodes();
        List<String> errors = validationErrors();
        boolean valid = errors.isEmpty();
        graphics.drawString(this.font, Component.literal("CONNECTED CHAIN (" + chain.size() + ")"), left, chainTop - 13, 0xFFC5D7D1, false);
        graphics.drawString(this.font, Component.literal("BLOCK PALETTE"), paletteX, chainTop - 13, 0xFFC5D7D1, false);

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
        }

        for (int i = 0; i < 7; i++) {
            int y = chainTop + i * rowHeight;
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
        graphics.drawString(this.font, Component.literal(displayValidation), left, saveY + 33, validationColor, false);
        graphics.drawString(this.font, Component.literal("Select a connected block to reorder or delete it. Ctrl+Enter saves; Ctrl+Arrow moves the selected block."), left, saveY + 46, 0xFF9BA9A6, false);

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void drawBox(GuiGraphics graphics, int left, int top, int right, int bottom, boolean hovered, String label) {
        graphics.fill(left, top, right, bottom, hovered ? 0xFF41402E : 0xFF171B1C);
        int border = hovered ? 0xFFFFE5B4 : 0xFF566462;
        graphics.fill(left, top, right, top + 1, border);
        graphics.fill(left, bottom - 1, right, bottom, border);
        graphics.fill(left, top, left + 1, bottom, border);
        graphics.fill(right - 1, top, right, bottom, border);
        graphics.drawString(this.font, Component.literal(label), left + 3, top + (bottom - top - 8) / 2, 0xFFF1F2EC, false);
    }
}
