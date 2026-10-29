package ladysnake.requiem.client.model;
import ladysnake.requiem.Requiem;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
/** Independently editable, bulkier body template for the immortal form and its 64x64 texture. */
public class ImmortalHomunculusModel extends EntityModel<LivingEntityRenderState> {
    public static final ModelLayerLocation LAYER_LOCATION=new ModelLayerLocation(Requiem.id("immortal_homunculus"),"main");
    public ImmortalHomunculusModel(ModelPart root){super(root);}
    public static LayerDefinition createBodyLayer(){
        MeshDefinition mesh=new MeshDefinition();PartDefinition root=mesh.getRoot();CubeDeformation d=new CubeDeformation(0.0F);
        root.addOrReplaceChild("head",CubeListBuilder.create().texOffs(0,0).addBox(-4.5F,-9,-4.5F,9,9,9,d),PartPose.offset(0,-1,0));
        root.addOrReplaceChild("jaw",CubeListBuilder.create().texOffs(0,32).addBox(-3,-1,-4,6,3,5,d),PartPose.offset(0,-1,0));
        root.addOrReplaceChild("body",CubeListBuilder.create().texOffs(16,18).addBox(-4.5F,0,-2.5F,9,13,5,d),PartPose.offset(0,0,0));
        root.addOrReplaceChild("left_shoulder_spike",CubeListBuilder.create().texOffs(40,0).addBox(0,-2,-2,5,5,5,d),PartPose.offset(4,1,0));
        root.addOrReplaceChild("right_shoulder_spike",CubeListBuilder.create().texOffs(40,0).mirror().addBox(-5,-2,-2,5,5,5,d),PartPose.offset(-4,1,0));
        root.addOrReplaceChild("right_arm",CubeListBuilder.create().texOffs(32,18).addBox(-3,-2,-2.5F,4,13,5,d),PartPose.offset(-6,2,0));
        root.addOrReplaceChild("left_arm",CubeListBuilder.create().texOffs(32,18).mirror().addBox(-1,-2,-2.5F,4,13,5,d),PartPose.offset(6,2,0));
        root.addOrReplaceChild("right_leg",CubeListBuilder.create().texOffs(0,18).addBox(-2.2F,0,-2.2F,4,13,4,d),PartPose.offset(-2,12,0));
        root.addOrReplaceChild("left_leg",CubeListBuilder.create().texOffs(0,18).mirror().addBox(-2.2F,0,-2.2F,4,13,4,d),PartPose.offset(2,12,0));
        return LayerDefinition.create(mesh,64,64);
    }
    @Override public void setupAnim(LivingEntityRenderState state){}
}