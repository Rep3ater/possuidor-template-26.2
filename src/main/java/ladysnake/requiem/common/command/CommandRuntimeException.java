package ladysnake.requiem.common.command;

import net.minecraft.network.chat.Component;

public class CommandRuntimeException extends RuntimeException {
    private final Component messageComponent;

    public CommandRuntimeException(Component messageComponent) {
        super(messageComponent.getString());
        this.messageComponent = messageComponent;
    }

    public Component getComponent() {
        return messageComponent;
    }
}
