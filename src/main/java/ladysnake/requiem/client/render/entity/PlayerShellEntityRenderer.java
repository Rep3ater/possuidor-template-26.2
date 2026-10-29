package ladysnake.requiem.client.render.entity;

import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.player.PlayerModelType;

public class PlayerShellEntityRenderer extends AvatarRenderer<AbstractClientPlayer> {
    private final PlayerModel defaultModel;
    private final PlayerModel slimModel;

    public PlayerShellEntityRenderer(EntityRendererProvider.Context context) {
        super(context, false);
        this.defaultModel = this.model;
        this.slimModel = new PlayerModel(context.bakeLayer(ModelLayers.PLAYER_SLIM), true);
    }

    @Override
    public void extractRenderState(AbstractClientPlayer entity, AvatarRenderState state, float partialTicks) {
        if (entity.getSkin().model() == PlayerModelType.SLIM) {
            this.model = this.slimModel;
        } else {
            this.model = this.defaultModel;
        }
        super.extractRenderState(entity, state, partialTicks);
    }
}
