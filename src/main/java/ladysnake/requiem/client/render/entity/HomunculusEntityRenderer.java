package ladysnake.requiem.client.render.entity;
import ladysnake.requiem.Requiem;
import ladysnake.requiem.client.model.HomunculusModel;
import ladysnake.requiem.common.entity.HomunculusEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;
public class HomunculusEntityRenderer extends MobRenderer<HomunculusEntity,LivingEntityRenderState,HomunculusModel>{
 private static final Identifier TEXTURE=Requiem.id("textures/entity/homunculus.png");
 public HomunculusEntityRenderer(EntityRendererProvider.Context c){super(c,new HomunculusModel(c.bakeLayer(HomunculusModel.LAYER_LOCATION)),0.35F);}
 @Override public LivingEntityRenderState createRenderState(){return new LivingEntityRenderState();}
 @Override public Identifier getTextureLocation(LivingEntityRenderState state){return TEXTURE;}
}