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
/** Editable geometry template; edit this Java model and its 64x64 entity texture independently. */
public class HomunculusModel extends EntityModel<LivingEntityRenderState> {
    public static final ModelLayerLocation LAYER_LOCATION=new ModelLayerLocation(Requiem.id("homunculus"),"main");
    private final ModelPart rightArm,leftArm,rightLeg,leftLeg;
    public HomunculusModel(ModelPart root){super(root);rightArm=root.getChild("right_arm");leftArm=root.getChild("left_arm");rightLeg=root.getChild("right_leg");leftLeg=root.getChild("left_leg");}
    public static LayerDefinition createBodyLayer(){
        MeshDefinition mesh=new MeshDefinition();PartDefinition root=mesh.getRoot();CubeDeformation d=new CubeDeformation(0.0F);
        root.addOrReplaceChild("head",CubeListBuilder.create().texOffs(0,0).addBox(-4,-8,-4,8,8,8,d),PartPose.offset(0,0,0));
        root.addOrReplaceChild("body",CubeListBuilder.create().texOffs(16,16).addBox(-4,0,-2,8,12,4,d),PartPose.offset(0,0,0));
        root.addOrReplaceChild("right_arm",CubeListBuilder.create().texOffs(40,16).addBox(-3,-2,-2,4,12,4,d),PartPose.offset(-5,2,0));
        root.addOrReplaceChild("left_arm",CubeListBuilder.create().texOffs(40,16).mirror().addBox(-1,-2,-2,4,12,4,d),PartPose.offset(5,2,0));
        root.addOrReplaceChild("right_leg",CubeListBuilder.create().texOffs(0,16).addBox(-2,0,-2,4,12,4,d),PartPose.offset(-2,12,0));
        root.addOrReplaceChild("left_leg",CubeListBuilder.create().texOffs(0,16).mirror().addBox(-2,0,-2,4,12,4,d),PartPose.offset(2,12,0));
        return LayerDefinition.create(mesh,64,64);
    }
    @Override public void setupAnim(LivingEntityRenderState state){rightArm.xRot=0;leftArm.xRot=0;rightLeg.xRot=0;leftLeg.xRot=0;}
}