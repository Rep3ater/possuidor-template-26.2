package ladysnake.requiem.client.render.entity;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;

/** Uses the vanilla player renderer so shell entities retain their captured player skin. */
public class PlayerShellEntityRenderer extends PlayerRenderer {
    public PlayerShellEntityRenderer(EntityRendererProvider.Context context) {
        super(context, false);
    }
}
