package ladysnake.requiem.client.gui;

import ladysnake.requiem.common.network.RequiemNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Scratch-inspired, connected visual editor for Marbas' brain programs.
 * Commands are fixed block types. Only their explicit argument sockets are text-editable.
 * Programs are serialized to the existing semicolon-delimited wire/save format.
 */
public final class HomunculusBrainScreen extends Screen {
    private enum Kind {
        HOME("go home", "MOTION", 0xFF4C97FF, "return home", 0),
        GOTO("go to coordinates", "MOTION", 0xFF4C97FF, "goto 0 64 0", 3),
        WAIT("wait", "CONTROL", 0xFFFFAB19, "wait", 0),
        GUARD("guard here", "CONTROL", 0xFFFFAB19, "guard", 0),
        FIND_ITEM("find item", "ITEMS", 0xFFFF8C1A, "find item", 1),
        PICKUP("pick up item", "ITEMS", 0xFFFF8C1A, "pickup", 1),
        TAKE_ITEM("take target item", "ITEMS", 0xFFFF8C1A, "take item", 0),
        STORE_CHEST("store in chest", "ITEMS", 0xFFFF8C1A, "store chest", 0),
        DROP_ITEM("drop held item", "ITEMS", 0xFFFF8C1A, "drop item", 0),
        FIND_ENTITY("find entity", "COMBAT", 0xFFFF6680, "find entity", 1),
        KILL("attack entity", "COMBAT", 0xFFFF6680, "kill", 1),
        UNKNOWN("unsupported block", "ERROR", 0xFFB84F68, "", 0);

        final String label;
        final String category;
        final int color;
        final String baseCommand;
        final int inputCount;

        Kind(String label, String category, int color, String baseCommand, int inputCount) {
            this.label = label;
            this.category = category;
            this.color = color;
            this.baseCommand = baseCommand;
            this.inputCount = inputCount;
        }

        static Kind[] palette() {
            return new Kind[] {HOME, GOTO, WAIT, GUARD, FIND_ITEM, PICKUP, TAKE_ITEM, STORE_CHEST, DROP_ITEM, FIND_ENTITY, KILL};
        }
    }

    private static final int PANEL = 0xFF161C26;
    private static final int PANEL_DARK = 0xFF10151D;
    private static final int TEXT = 0xFFF7F7F2;
    private static final int MUTED = 0xFFB9C5D2;
    private static final int FIELD_COLOR = 0xFF303746;
    private static final int SELECTED = 0xFFFFD166;

    private final int entityId;
    private final String initialProgram;
    private final List<BrainBlock> blocks = new ArrayList<>();
    private final List<EditBox> argumentFields = new ArrayList<>();

    private int selectedIndex = -1;
    private int scrollOffset = 0;
    private int draggingPaletteIndex = -1;
    private int draggingBlockIndex = -1;
    private String status = "Drag a coloured block into the script, or click a palette block to append it.";

    private static final class BrainBlock {
        Kind kind;
        final String[] inputs;
        final String unsupportedCommand;

        BrainBlock(Kind kind, String... inputs) {
            this(kind, inputs, "");
        }

        BrainBlock(Kind kind, String[] inputs, String unsupportedCommand) {
            this.kind = kind;
            this.inputs = inputs;
            this.unsupportedCommand = unsupportedCommand;
        }
    }

    public HomunculusBrainScreen(int entityId, String currentProgram) {
        super(Component.literal("Homunculus Brain — Block Editor"));
        this.entityId = entityId;
        this.initialProgram = currentProgram == null ? "" : currentProgram;
        parseProgram(this.initialProgram);
    }

    @Override
    protected void init() {
        super.init();
        clampScroll();
        rebuildArgumentFields();
    }

    private int paletteX() { return 16; }
    private int paletteWidth() { return 194; }
    private int paletteTop() { return 91; }
    private int paletteStep() { return 43; }
    private int workspaceX() { return 226; }
    private int workspaceTop() { return 91; }
    private int workspaceWidth() { return Math.max(230, this.width - workspaceX() - 16); }
    private int rowHeight() { return 34; }
    private int rowStep() { return 43; }
    private int footerY() { return this.height - 48; }
    private int visibleRows() { return Math.max(1, (footerY() - workspaceTop() - 8) / rowStep()); }
    private int workspaceRight() { return workspaceX() + workspaceWidth(); }

    private void parseProgram(String program) {
        blocks.clear();
        if (program == null || program.isBlank()) {
            blocks.add(new BrainBlock(Kind.HOME));
            selectedIndex = 0;
            return;
        }
        for (String raw : program.split(";")) {
            String command = raw.trim();
            if (!command.isEmpty()) blocks.add(parseBlock(command));
        }
        if (blocks.isEmpty()) blocks.add(new BrainBlock(Kind.HOME));
        selectedIndex = 0;
    }

    private static BrainBlock parseBlock(String command) {
        String lower = command.trim().toLowerCase(Locale.ROOT);
        if (lower.equals("return home") || lower.equals("home") || lower.equals("go home")) return new BrainBlock(Kind.HOME);
        if (lower.equals("wait")) return new BrainBlock(Kind.WAIT);
        if (lower.equals("guard")) return new BrainBlock(Kind.GUARD);
        if (lower.equals("take") || lower.equals("take item")) return new BrainBlock(Kind.TAKE_ITEM);
        if (lower.equals("drop") || lower.equals("drop item")) return new BrainBlock(Kind.DROP_ITEM);
        if (lower.equals("store") || lower.equals("store chest") || lower.startsWith("store chest ")) return new BrainBlock(Kind.STORE_CHEST);

        if (lower.startsWith("goto ")) {
            String[] xyz = lower.substring(5).trim().split("\\s+");
            if (xyz.length == 3) return new BrainBlock(Kind.GOTO, xyz[0], xyz[1], xyz[2]);
            return new BrainBlock(Kind.UNKNOWN, new String[0], command);
        }
        if (lower.equals("goto")) return new BrainBlock(Kind.UNKNOWN, new String[0], command);

        if (lower.equals("find item") || lower.startsWith("find item:") || lower.startsWith("find item ")) {
            String target = "";
            if (lower.startsWith("find item:")) target = command.substring(command.indexOf(':') + 1).trim();
            else if (lower.startsWith("find item ")) target = command.substring("find item ".length()).trim();
            return new BrainBlock(Kind.FIND_ITEM, target);
        }
        if (lower.equals("pickup") || lower.equals("pickup item") || lower.startsWith("pickup:") || lower.startsWith("pickup ")) {
            String target = "";
            if (lower.startsWith("pickup:")) target = command.substring(command.indexOf(':') + 1).trim();
            else if (lower.startsWith("pickup ") && !lower.equals("pickup item")) target = command.substring("pickup ".length()).trim();
            return new BrainBlock(Kind.PICKUP, target);
        }
        if (lower.equals("find entity") || lower.startsWith("find entity:") || lower.startsWith("find entity ")) {
            String target = "";
            if (lower.startsWith("find entity:")) target = command.substring(command.indexOf(':') + 1).trim();
            else if (lower.startsWith("find entity ")) target = command.substring("find entity ".length()).trim();
            return new BrainBlock(Kind.FIND_ENTITY, target);
        }
        if (lower.equals("kill") || lower.equals("kill entity") || lower.startsWith("kill:") || lower.startsWith("kill ")) {
            String target = "";
            if (lower.startsWith("kill:")) target = command.substring(command.indexOf(':') + 1).trim();
            else if (lower.startsWith("kill ") && !lower.equals("kill entity")) target = command.substring("kill ".length()).trim();
            return new BrainBlock(Kind.KILL, target);
        }
        return new BrainBlock(Kind.UNKNOWN, new String[0], command);
    }

    private static BrainBlock makeBlock(Kind kind) {
        return switch (kind) {
            case GOTO -> new BrainBlock(kind, "0", "64", "0");
            case FIND_ITEM -> new BrainBlock(kind, "");
            case PICKUP -> new BrainBlock(kind, "");
            case FIND_ENTITY -> new BrainBlock(kind, "");
            case KILL -> new BrainBlock(kind, "");
            case UNKNOWN -> new BrainBlock(kind, new String[0], "");
            default -> new BrainBlock(kind);
        };
    }

    private void rebuildArgumentFields() {
        clearWidgets();
        argumentFields.clear();
        int count = Math.min(visibleRows(), Math.max(0, blocks.size() - scrollOffset));
        for (int row = 0; row < count; row++) {
            int blockIndex = scrollOffset + row;
            BrainBlock block = blocks.get(blockIndex);
            if (block.kind == Kind.UNKNOWN) continue;
            int x = workspaceX();
            int y = workspaceTop() + row * rowStep();
            int w = workspaceWidth();
            if (block.kind == Kind.GOTO) {
                int startX = x + 93;
                int available = Math.max(150, w - 105);
                int gap = 7;
                int fieldWidth = Math.max(35, Math.min(72, (available - gap * 2) / 3));
                for (int arg = 0; arg < 3; arg++) {
                    int fieldX = Math.min(x + w - fieldWidth - 8, startX + arg * (fieldWidth + gap));
                    addArgumentField(blockIndex, arg, fieldX, y + 8, fieldWidth, "xyz".substring(arg, arg + 1));
                }
            } else if (block.inputCount == 1) {
                int labelWidth = switch (block.kind) {
                    case FIND_ITEM -> 92;
                    case PICKUP -> 71;
                    case FIND_ENTITY -> 106;
                    case KILL -> 55;
                    default -> 92;
                };
                int fieldX = x + labelWidth;
                int fieldWidth = Math.max(75, w - labelWidth - 12);
                addArgumentField(blockIndex, 0, fieldX, y + 8, fieldWidth,
                    switch (block.kind) {
                        case FIND_ITEM -> "item ID or name (optional)";
                        case PICKUP -> "filter (optional)";
                        case FIND_ENTITY -> "entity name (optional)";
                        case KILL -> "target name (blank = nearest)";
                        default -> "";
                    });
            }
        }
    }

    private void addArgumentField(int blockIndex, int argIndex, int x, int y, int width, String hint) {
        BrainBlock block = blocks.get(blockIndex);
        EditBox field = new EditBox(this.font, x, y, Math.max(32, width), 18,
            Component.literal("Editable block argument"));
        field.setMaxLength(96);
        field.setHint(Component.literal(hint));
        field.setTextColor(0xFFFFFFFF);
        field.setTextColorUneditable(0xFFB7BCC6);
        field.setResponder(value -> {
            if (blockIndex < blocks.size() && blocks.get(blockIndex) == block && argIndex < block.inputs.length) {
                block.inputs[argIndex] = value;
                this.status = validationMessage();
            }
        });
        field.setValue(argIndex < block.inputs.length ? block.inputs[argIndex] : "");
        this.addRenderableWidget(field);
        argumentFields.add(field);
    }

    private String serialize(BrainBlock block) {
        if (block.kind == Kind.UNKNOWN) return block.unsupportedCommand;
        if (block.kind == Kind.HOME) return "return home";
        if (block.kind == Kind.WAIT) return "wait";
        if (block.kind == Kind.GUARD) return "guard";
        if (block.kind == Kind.TAKE_ITEM) return "take item";
        if (block.kind == Kind.STORE_CHEST) return "store chest";
        if (block.kind == Kind.DROP_ITEM) return "drop item";
        if (block.kind == Kind.GOTO) {
            return "goto " + cleanArg(block.inputs[0]) + " " + cleanArg(block.inputs[1]) + " " + cleanArg(block.inputs[2]);
        }
        String target = cleanArg(block.inputs[0]);
        return switch (block.kind) {
            case FIND_ITEM -> target.isEmpty() ? "find item" : "find item:" + target;
            case PICKUP -> target.isEmpty() ? "pickup" : "pickup:" + target;
            case FIND_ENTITY -> target.isEmpty() ? "find entity" : "find entity:" + target;
            case KILL -> target.isEmpty() ? "kill" : "kill:" + target;
            default -> block.kind.baseCommand;
        };
    }

    private static String cleanArg(String value) {
        return value == null ? "" : value.replace(';', ' ').replace('\n', ' ').replace('\r', ' ').trim();
    }

    private List<String> validationErrors() {
        List<String> errors = new ArrayList<>();
        for (int i = 0; i < blocks.size(); i++) {
            BrainBlock block = blocks.get(i);
            if (block.kind == Kind.UNKNOWN) {
                errors.add("Block " + (i + 1) + " is unsupported (" + block.unsupportedCommand + "); remove it and add a supported block.");
                continue;
            }
            if (block.kind == Kind.GOTO) {
                if (block.inputs.length != 3) {
                    errors.add("Block " + (i + 1) + ": the coordinate block has missing inputs.");
                    continue;
                }
                try {
                    Integer.parseInt(cleanArg(block.inputs[0]));
                    Integer.parseInt(cleanArg(block.inputs[1]));
                    Integer.parseInt(cleanArg(block.inputs[2]));
                } catch (NumberFormatException ex) {
                    errors.add("Block " + (i + 1) + ": X, Y and Z must each be whole numbers.");
                }
            }
        }
        return errors;
    }

    private String validationMessage() {
        List<String> errors = validationErrors();
        if (errors.isEmpty()) return blocks.isEmpty() ? "Empty scripts save as GO HOME." : "All blocks are valid. Fixed block commands cannot be accidentally edited.";
        return errors.get(0) + (errors.size() > 1 ? " (+" + (errors.size() - 1) + " more)" : "");
    }

    private void save() {
        List<String> errors = validationErrors();
        if (!errors.isEmpty()) {
            status = errors.get(0);
            return;
        }
        List<String> program = new ArrayList<>();
        for (BrainBlock block : blocks) program.add(serialize(block));
        String safe = program.isEmpty() ? "return home" : String.join("; ", program);
        if (safe.length() > 1024) {
            status = "This script is over the 1024-character limit. Remove blocks or shorten their arguments.";
            return;
        }
        RequiemNetworking.sendHomunculusBrain(this.entityId, safe);
        Minecraft.getInstance().setScreenAndShow(null);
    }

    private void removeSelected() {
        if (selectedIndex < 0 || selectedIndex >= blocks.size()) return;
        blocks.remove(selectedIndex);
        if (blocks.isEmpty()) selectedIndex = -1;
        else selectedIndex = Math.min(selectedIndex, blocks.size() - 1);
        clampScroll();
        rebuildArgumentFields();
        status = validationMessage();
    }

    private void moveSelected(int delta) {
        if (selectedIndex < 0 || selectedIndex >= blocks.size()) return;
        int destination = selectedIndex + delta;
        if (destination < 0 || destination >= blocks.size()) return;
        BrainBlock block = blocks.remove(selectedIndex);
        blocks.add(destination, block);
        selectedIndex = destination;
        if (selectedIndex < scrollOffset) scrollOffset = selectedIndex;
        if (selectedIndex >= scrollOffset + visibleRows()) scrollOffset = selectedIndex - visibleRows() + 1;
        clampScroll();
        rebuildArgumentFields();
        status = validationMessage();
    }

    private void insertBlock(Kind kind, int at) {
        if (kind == null || kind == Kind.UNKNOWN) return;
        int index = Math.max(0, Math.min(at, blocks.size()));
        blocks.add(index, makeBlock(kind));
        selectedIndex = index;
        if (selectedIndex < scrollOffset) scrollOffset = selectedIndex;
        if (selectedIndex >= scrollOffset + visibleRows()) scrollOffset = selectedIndex - visibleRows() + 1;
        clampScroll();
        rebuildArgumentFields();
        status = validationMessage();
    }

    private void clampScroll() {
        scrollOffset = Math.max(0, Math.min(scrollOffset, Math.max(0, blocks.size() - visibleRows())));
    }

    private int paletteIndexAt(double x, double y) {
        if (x < paletteX() || x >= paletteX() + paletteWidth() || y < paletteTop()) return -1;
        int col = x < paletteX() + paletteWidth() / 2 ? 0 : 1;
        int colWidth = paletteWidth() / 2;
        int row = (int) ((y - paletteTop()) / paletteStep());
        if (row < 0 || row >= 6 || y >= paletteTop() + 6 * paletteStep()) return -1;
        int index = row * 2 + col;
        return index < Kind.palette().length ? index : -1;
    }

    private int workspaceRowAt(double y) {
        if (y < workspaceTop() || y >= footerY() - 3) return -1;
        int row = (int) ((y - workspaceTop()) / rowStep());
        return row < visibleRows() ? row : -1;
    }

    private void dropExistingBlock(int original, double mouseY) {
        int row = workspaceRowAt(mouseY);
        if (row < 0 || original < 0 || original >= blocks.size()) return;
        int target = Math.max(0, Math.min(scrollOffset + row, blocks.size() - 1));
        if (target == original) return;
        BrainBlock moved = blocks.remove(original);
        if (target > original) target--;
        blocks.add(target, moved);
        selectedIndex = target;
        clampScroll();
        rebuildArgumentFields();
        status = validationMessage();
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (super.mouseClicked(event, doubleClick)) return true;
        if (event.button() != 0) return false;

        int paletteIndex = paletteIndexAt(event.x(), event.y());
        if (paletteIndex >= 0) {
            draggingPaletteIndex = paletteIndex;
            draggingBlockIndex = -1;
            return true;
        }

        int row = workspaceRowAt(event.y());
        if (row >= 0) {
            int index = scrollOffset + row;
            if (index < blocks.size()) {
                selectedIndex = index;
                draggingBlockIndex = index;
                draggingPaletteIndex = -1;
                status = validationMessage();
                return true;
            }
        }

        int controlsY = footerY();
        int x = workspaceX();
        int gap = 5;
        int[] widths = {42, 42, 70, 64, 130};
        String[] labels = {"↑", "↓", "REMOVE", "CLEAR", "SAVE SCRIPT"};
        int cursor = x;
        for (int i = 0; i < widths.length; i++) {
            if (event.x() >= cursor && event.x() < cursor + widths[i]
                    && event.y() >= controlsY && event.y() < controlsY + 26) {
                switch (i) {
                    case 0 -> moveSelected(-1);
                    case 1 -> moveSelected(1);
                    case 2 -> removeSelected();
                    case 3 -> {
                        blocks.clear();
                        selectedIndex = -1;
                        scrollOffset = 0;
                        rebuildArgumentFields();
                        status = validationMessage();
                    }
                    case 4 -> save();
                    default -> { }
                }
                return true;
            }
            cursor += widths[i] + gap;
        }

        if (event.x() >= workspaceRight() - 74 && event.y() >= workspaceTop() - 22 && event.y() < workspaceTop()) {
            scrollOffset = Math.max(0, scrollOffset - 1);
            rebuildArgumentFields();
            return true;
        }
        if (event.x() >= workspaceRight() - 34 && event.y() >= workspaceTop() - 22 && event.y() < workspaceTop()) {
            scrollOffset = Math.min(Math.max(0, blocks.size() - visibleRows()), scrollOffset + 1);
            rebuildArgumentFields();
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        boolean inherited = super.mouseReleased(event);
        if (event.button() != 0) return inherited;

        if (draggingPaletteIndex >= 0) {
            Kind kind = Kind.palette()[draggingPaletteIndex];
            int row = workspaceRowAt(event.y());
            int insertAt = row < 0 ? blocks.size() : Math.min(blocks.size(), scrollOffset + row);
            insertBlock(kind, insertAt);
            draggingPaletteIndex = -1;
            draggingBlockIndex = -1;
            return true;
        }

        if (draggingBlockIndex >= 0) {
            int original = draggingBlockIndex;
            dropExistingBlock(original, event.y());
            draggingBlockIndex = -1;
            return true;
        }
        return inherited;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (mouseX >= workspaceX() && mouseX <= workspaceRight()) {
            int direction = scrollY > 0 ? -1 : 1;
            int newOffset = Math.max(0, Math.min(Math.max(0, blocks.size() - visibleRows()), scrollOffset + direction));
            if (newOffset != scrollOffset) {
                scrollOffset = newOffset;
                rebuildArgumentFields();
            }
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (super.keyPressed(event)) return true;
        if (event.key() == GLFW.GLFW_KEY_ENTER && (event.modifiers() & GLFW.GLFW_MOD_CONTROL) != 0) {
            save();
            return true;
        }
        if (event.key() == GLFW.GLFW_KEY_DELETE || event.key() == GLFW.GLFW_KEY_BACKSPACE) {
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
        if (event.key() == GLFW.GLFW_KEY_ESCAPE) {
            Minecraft.getInstance().setScreenAndShow(null);
            return true;
        }
        return false;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, this.width, this.height, PANEL_DARK);
        graphics.fill(paletteX() - 5, 70, paletteX() + paletteWidth() + 3, footerY() - 7, PANEL);
        graphics.fill(workspaceX() - 7, 70, workspaceRight() + 4, footerY() - 7, 0xFF202734);

        graphics.text(this.font, Component.literal("HOMUNCULUS  /  BRAIN SCRIPT"), 18, 18, 0xFFFFFFFF, false);
        graphics.text(this.font, Component.literal("SCRATCH-STYLE BLOCK EDITOR"), 18, 36, 0xFF8EDBFF, false);
        graphics.text(this.font, Component.literal("Drag blocks into the script. Edit only the small input sockets inside each block."), 18, 53, MUTED, false);

        graphics.text(this.font, Component.literal("BLOCKS"), paletteX() + 3, 77, TEXT, false);
        graphics.text(this.font, Component.literal("MY SCRIPT (" + blocks.size() + ")"), workspaceX() + 2, 77, TEXT, false);
        graphics.text(this.font, Component.literal("▲"), workspaceRight() - 69, 76, MUTED, false);
        graphics.text(this.font, Component.literal("▼"), workspaceRight() - 28, 76, MUTED, false);

        Kind[] palette = Kind.palette();
        for (int i = 0; i < palette.length; i++) {
            int col = i % 2;
            int row = i / 2;
            int cellW = (paletteWidth() - 8) / 2;
            int x = paletteX() + 3 + col * (cellW + 2);
            int y = paletteTop() + row * paletteStep();
            Kind kind = palette[i];
            boolean hovered = mouseX >= x && mouseX <= x + cellW && mouseY >= y && mouseY <= y + rowHeight();
            drawScratchBlock(graphics, x, y, cellW, rowHeight(), kind.color, hovered, false, kind.label.toUpperCase(Locale.ROOT));
        }

        for (int row = 0; row < visibleRows(); row++) {
            int index = scrollOffset + row;
            int x = workspaceX();
            int y = workspaceTop() + row * rowStep();
            int w = workspaceWidth();
            if (index >= blocks.size()) {
                graphics.fill(x + 5, y + 9, x + w - 5, y + rowHeight() - 2, 0xFF161D27);
                graphics.fill(x + 5, y + 9, x + w - 5, y + 10, 0xFF384352);
                if (index == blocks.size()) {
                    graphics.text(this.font, Component.literal("+ drop a block here"), x + 18, y + 18, 0xFF7E8A9B, false);
                }
                continue;
            }
            BrainBlock block = blocks.get(index);
            boolean hovered = mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + rowHeight();
            drawScratchBlock(graphics, x, y, w, rowHeight(), block.kind.color, hovered,
                selectedIndex == index, "");
            graphics.text(this.font, Component.literal(block.kind.label.toUpperCase(Locale.ROOT)),
                x + 12, y + 12, TEXT, false);
            if (block.kind == Kind.UNKNOWN) {
                String command = block.unsupportedCommand.length() > 40
                    ? block.unsupportedCommand.substring(0, 37) + "..." : block.unsupportedCommand;
                graphics.text(this.font, Component.literal("!" + command), x + 150, y + 12, 0xFFFFD3D3, false);
            }
        }

        int controlsY = footerY();
        int x = workspaceX();
        int gap = 5;
        int[] widths = {42, 42, 70, 64, 130};
        String[] labels = {"↑", "↓", "REMOVE", "CLEAR", "SAVE SCRIPT"};
        int cursor = x;
        for (int i = 0; i < widths.length; i++) {
            boolean enabled = i == 4 || i == 3 || (selectedIndex >= 0 && selectedIndex < blocks.size());
            boolean hovered = mouseX >= cursor && mouseX <= cursor + widths[i] && mouseY >= controlsY && mouseY <= controlsY + 26;
            int color = i == 4 ? 0xFF2E9D67 : 0xFF354052;
            if (!enabled) color = 0xFF252C38;
            graphics.fill(cursor, controlsY, cursor + widths[i], controlsY + 26, hovered ? lighten(color) : color);
            graphics.fill(cursor, controlsY, cursor + widths[i], controlsY + 2, i == 4 ? 0xFF8BFFB9 : 0xFF64748B);
            graphics.text(this.font, Component.literal(labels[i]), cursor + 5, controlsY + 9,
                enabled ? TEXT : 0xFF737D8D, false);
            cursor += widths[i] + gap;
        }

        String feedback = status == null || status.isBlank() ? validationMessage() : status;
        int feedbackColor = validationErrors().isEmpty() ? 0xFF8BFFB9 : 0xFFFFC0C0;
        if (feedback.length() > 100) feedback = feedback.substring(0, 97) + "...";
        graphics.text(this.font, Component.literal(feedback), 18, footerY() + 8, feedbackColor, false);
        graphics.text(this.font, Component.literal("Scroll to browse blocks. Ctrl+Enter saves · Ctrl+↑/↓ reorders · Delete removes selected block."),
            18, this.height - 17, MUTED, false);

        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    private void drawScratchBlock(GuiGraphicsExtractor graphics, int x, int y, int width, int height,
                                  int color, boolean hovered, boolean selected, String label) {
        int main = hovered ? lighten(color) : color;
        graphics.fill(x + 7, y, x + width - 7, y + 6, main);
        graphics.fill(x, y + 4, x + width, y + height - 4, main);
        graphics.fill(x + 7, y + height - 5, x + width - 7, y + height, main);
        // Classic block connector and socket styling.
        graphics.fill(x + 17, y - 2, x + 37, y + 4, main);
        graphics.fill(x + 22, y + height - 1, x + 43, y + height + 3, main);
        if (selected) {
            graphics.fill(x, y + 4, x + 3, y + height - 4, SELECTED);
            graphics.fill(x + 7, y, x + width - 7, y + 2, SELECTED);
        }
        if (!label.isEmpty()) {
            graphics.text(this.font, Component.literal(label), x + 8, y + (height - 8) / 2, TEXT, false);
        }
    }

    private static int lighten(int argb) {
        int a = (argb >>> 24) & 0xFF;
        int r = Math.min(255, ((argb >>> 16) & 0xFF) + 18);
        int g = Math.min(255, ((argb >>> 8) & 0xFF) + 18);
        int b = Math.min(255, (argb & 0xFF) + 18);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }
}
