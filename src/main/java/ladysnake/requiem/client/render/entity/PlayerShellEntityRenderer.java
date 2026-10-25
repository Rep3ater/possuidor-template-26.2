package ladysnake.requiem.client.render.entity;

import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;

public class PlayerShellEntityRenderer extends AvatarRenderer<AbstractClientPlayer> {
    public PlayerShellEntityRenderer(EntityRendererProvider.Context context) {
        super(context, false);
    }
}
