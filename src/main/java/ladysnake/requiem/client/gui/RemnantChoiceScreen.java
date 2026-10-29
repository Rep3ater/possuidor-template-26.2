package ladysnake.requiem.client.gui;

import ladysnake.requiem.common.network.RequiemNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class RemnantChoiceScreen extends Screen {

    private enum DialogueState {
        INTRODUCTION,
        EXPLANATION,
        INTERESTED,
        DISINTERESTED,
        VERY_INTERESTED,
        VERY_DISINTERESTED
    }

    private record DialogueChoice(Component text, Runnable onSelect) {}

    private DialogueState currentState = DialogueState.INTRODUCTION;
    private final List<DialogueChoice> currentChoices = new ArrayList<>();
    private Component currentDialogueText = Component.empty();
    private int hoveredChoice = -1;

    public RemnantChoiceScreen() {
        super(Component.literal("The Whispering Soul"));
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    protected void init() {
        super.init();
        loadState(this.currentState);
    }

    private void loadState(DialogueState state) {
        this.currentState = state;
        this.currentChoices.clear();
        this.hoveredChoice = -1;

        switch (state) {
            case INTRODUCTION -> {
                this.currentDialogueText = Component.translatable("requiem:dialogue.first_contact.introduction.text");
                this.currentChoices.add(new DialogueChoice(
                    Component.translatable("requiem:dialogue.first_contact.introduction.explain"),
                    () -> loadState(DialogueState.EXPLANATION)
                ));
                this.currentChoices.add(new DialogueChoice(
                    Component.translatable("requiem:dialogue.first_contact.introduction.yes"),
                    () -> loadState(DialogueState.INTERESTED)
                ));
                this.currentChoices.add(new DialogueChoice(
                    Component.translatable("requiem:dialogue.first_contact.introduction.no"),
                    () -> loadState(DialogueState.DISINTERESTED)
                ));
            }
            case EXPLANATION -> {
                this.currentDialogueText = Component.translatable("requiem:dialogue.first_contact.explanation.text");
                this.currentChoices.add(new DialogueChoice(
                    Component.translatable("requiem:dialogue.first_contact.explanation.ok"),
                    () -> loadState(DialogueState.INTRODUCTION)
                ));
            }
            case INTERESTED -> {
                this.currentDialogueText = Component.translatable("requiem:dialogue.first_contact.interested.text");
                this.currentChoices.add(new DialogueChoice(
                    Component.translatable("requiem:dialogue.first_contact.doubt"),
                    () -> loadState(DialogueState.INTRODUCTION)
                ));
                this.currentChoices.add(new DialogueChoice(
                    Component.translatable("requiem:dialogue.first_contact.confirm"),
                    () -> loadState(DialogueState.VERY_INTERESTED)
                ));
            }
            case DISINTERESTED -> {
                this.currentDialogueText = Component.translatable("requiem:dialogue.first_contact.disinterested.text");
                this.currentChoices.add(new DialogueChoice(
                    Component.translatable("requiem:dialogue.first_contact.doubt"),
                    () -> loadState(DialogueState.INTRODUCTION)
                ));
                this.currentChoices.add(new DialogueChoice(
                    Component.translatable("requiem:dialogue.first_contact.confirm"),
                    () -> loadState(DialogueState.VERY_DISINTERESTED)
                ));
            }
            case VERY_INTERESTED -> {
                this.currentDialogueText = Component.translatable("requiem:dialogue.first_contact.very_interested.text");
                this.currentChoices.add(new DialogueChoice(
                    Component.translatable("requiem:dialogue.first_contact.final_doubt"),
                    () -> loadState(DialogueState.INTERESTED)
                ));
                this.currentChoices.add(new DialogueChoice(
                    Component.translatable("requiem:dialogue.first_contact.final_confirm"),
                    () -> {
                        RequiemNetworking.sendDialogueActionMessage(0);
                        playSelectSound();
                        this.onClose();
                    }
                ));
            }
            case VERY_DISINTERESTED -> {
                this.currentDialogueText = Component.translatable("requiem:dialogue.first_contact.very_disinterested.text");
                this.currentChoices.add(new DialogueChoice(
                    Component.translatable("requiem:dialogue.first_contact.final_doubt"),
                    () -> loadState(DialogueState.DISINTERESTED)
                ));
                this.currentChoices.add(new DialogueChoice(
                    Component.translatable("requiem:dialogue.first_contact.final_confirm"),
                    () -> {
                        RequiemNetworking.sendDialogueActionMessage(1);
                        playSelectSound();
                        this.onClose();
                    }
                ));
            }
        }
    }

    private void playSelectSound() {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK.value(), 1.0F));
    }

    private int getChoiceIndexAt(double mouseX, double mouseY) {
        int boxWidth = Math.min(this.width - 40, 520);
        int boxHeight = 155;
        int boxLeft = (this.width - boxWidth) / 2;
        int boxTop = this.height - boxHeight - 20;
        int choiceY = boxTop + 65;

        for (int i = 0; i < this.currentChoices.size(); i++) {
            int itemTop = choiceY + i * 26;
            int itemBottom = itemTop + 22;
            int itemLeft = boxLeft + 14;
            int itemRight = boxLeft + boxWidth - 14;

            if (mouseX >= itemLeft && mouseX <= itemRight && mouseY >= itemTop && mouseY <= itemBottom) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int clicked = getChoiceIndexAt(mouseX, mouseY);
            if (clicked >= 0 && clicked < this.currentChoices.size()) {
                playSelectSound();
                this.currentChoices.get(clicked).onSelect().run();
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        int keyCode = keyCode;
        if (keyCode >= GLFW.GLFW_KEY_1 && keyCode <= GLFW.GLFW_KEY_9) {
            int index = keyCode - GLFW.GLFW_KEY_1;
            if (index < this.currentChoices.size()) {
                playSelectSound();
                this.currentChoices.get(index).onSelect().run();
                return true;
            }
        }
        if (keyCode == GLFW.GLFW_KEY_UP) {
            if (!this.currentChoices.isEmpty()) {
                if (this.hoveredChoice <= 0) {
                    this.hoveredChoice = this.currentChoices.size() - 1;
                } else {
                    this.hoveredChoice--;
                }
                playSelectSound();
                return true;
            }
        }
        if (keyCode == GLFW.GLFW_KEY_DOWN) {
            if (!this.currentChoices.isEmpty()) {
                if (this.hoveredChoice < 0 || this.hoveredChoice >= this.currentChoices.size() - 1) {
                    this.hoveredChoice = 0;
                } else {
                    this.hoveredChoice++;
                }
                playSelectSound();
                return true;
            }
        }
        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_SPACE) {
            if (this.hoveredChoice >= 0 && this.hoveredChoice < this.currentChoices.size()) {
                playSelectSound();
                this.currentChoices.get(this.hoveredChoice).onSelect().run();
                return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // Deep ethereal darkening overlay
        graphics.fill(0, 0, this.width, this.height, 0xC0060912);

        int boxWidth = Math.min(this.width - 40, 520);
        int boxHeight = 155;
        int boxLeft = (this.width - boxWidth) / 2;
        int boxTop = this.height - boxHeight - 20;
        int boxRight = boxLeft + boxWidth;
        int boxBottom = boxTop + boxHeight;

        // Dialogue box background
        graphics.fill(boxLeft, boxTop, boxRight, boxBottom, 0xEE10141D);

        // Ornate Borders
        graphics.fill(boxLeft, boxTop, boxRight, boxTop + 2, 0xFF3D526C);
        graphics.fill(boxLeft, boxBottom - 2, boxRight, boxBottom, 0xFF3D526C);
        graphics.fill(boxLeft, boxTop, boxLeft + 2, boxBottom, 0xFF3D526C);
        graphics.fill(boxRight - 2, boxTop, boxRight, boxBottom, 0xFF3D526C);

        // Speaker tag tab
        int speakerTagWidth = 170;
        int speakerTagHeight = 18;
        graphics.fill(boxLeft + 12, boxTop - 10, boxLeft + 12 + speakerTagWidth, boxTop + 8, 0xFF192130);
        graphics.fill(boxLeft + 12, boxTop - 10, boxLeft + 12 + speakerTagWidth, boxTop - 8, 0xFF5D789D);
        graphics.drawString(this.font, Component.literal("§6§l✦ A Whispering Voice ✦"), boxLeft + 20, boxTop - 6, 0xFFFFFFFF, true);

        // Dialogue speech text wrapped
        int textLeft = boxLeft + 16;
        int textTop = boxTop + 16;
        int textWrapWidth = boxWidth - 32;
        graphics.drawWordWrap(this.font, this.currentDialogueText, textLeft, textTop, textWrapWidth, 0xFFE0E0E0, true);

        // Choices section below
        int choiceY = boxTop + 65;
        int mouseChoice = getChoiceIndexAt(mouseX, mouseY);
        if (mouseChoice >= 0) {
            this.hoveredChoice = mouseChoice;
        }

        for (int i = 0; i < this.currentChoices.size(); i++) {
            DialogueChoice choice = this.currentChoices.get(i);
            int itemTop = choiceY + i * 26;
            int itemBottom = itemTop + 22;
            int itemLeft = boxLeft + 14;
            int itemRight = boxRight - 14;

            boolean isHovered = (this.hoveredChoice == i);

            // Choice button background
            int bgColor = isHovered ? 0x60256880 : 0x35141E2B;
            int borderColor = isHovered ? 0xFF50D0FF : 0xFF2A3A4D;

            graphics.fill(itemLeft, itemTop, itemRight, itemBottom, bgColor);
            graphics.fill(itemLeft, itemTop, itemRight, itemTop + 1, borderColor);
            graphics.fill(itemLeft, itemBottom - 1, itemRight, itemBottom, borderColor);
            graphics.fill(itemLeft, itemTop, itemLeft + 1, itemBottom, borderColor);
            graphics.fill(itemRight - 1, itemTop, itemRight, itemBottom, borderColor);

            String hotkeyBadge = "§e[" + (i + 1) + "]§r ";
            Component label = Component.literal(hotkeyBadge).append(choice.text());
            int textColor = isHovered ? 0xFFFFFFFF : 0xFFCCCCCC;
            graphics.drawString(this.font, label, itemLeft + 10, itemTop + 7, textColor, true);
        }

        super.render(graphics, mouseX, mouseY, partialTick);
    }
}
