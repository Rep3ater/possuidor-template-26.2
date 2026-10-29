package ladysnake.requiem.client.render.entity;
import ladysnake.requiem.Requiem;
import ladysnake.requiem.client.model.ImmortalHomunculusModel;
import ladysnake.requiem.common.entity.ImmortalHomunculusEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;
public class ImmortalHomunculusEntityRenderer extends MobRenderer<ImmortalHomunculusEntity,LivingEntityRenderState,ImmortalHomunculusModel>{
 private static final Identifier TEXTURE=Requiem.id("textures/entity/immortal_homunculus.png");
 public ImmortalHomunculusEntityRenderer(EntityRendererProvider.Context c){super(c,new ImmortalHomunculusModel(c.bakeLayer(ImmortalHomunculusModel.LAYER_LOCATION)),0.4F);}
 @Override public LivingEntityRenderState createRenderState(){return new LivingEntityRenderState();}
 @Override public Identifier getTextureLocation(LivingEntityRenderState state){return TEXTURE;}
}