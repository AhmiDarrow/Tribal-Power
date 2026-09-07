package tk.darrow.tribalpower.client;

import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;

/** Low-poly spirit silhouettes, with vanilla lighting and a small animated part budget. */
public class MarchCreatureModel<T extends Entity> extends HierarchicalModel<T> {
    public static final ModelLayerLocation WALKER = new ModelLayerLocation(ResourceLocation.parse("tribalpower:march_walker"), "main");
    public static final ModelLayerLocation WISP = new ModelLayerLocation(ResourceLocation.parse("tribalpower:spirit_wisp"), "main");
    private final ModelPart root;
    private final boolean wisp;
    public MarchCreatureModel(ModelPart root, boolean wisp) { this.root=root; this.wisp=wisp; }
    @Override public ModelPart root() { return root; }
    public static LayerDefinition walker() {
        MeshDefinition mesh=new MeshDefinition();var root=mesh.getRoot();
        root.addOrReplaceChild("body",CubeListBuilder.create().texOffs(0,20).addBox(-5,-5,-8,10,10,16),PartPose.offset(0,13,1));
        var head=root.addOrReplaceChild("head",CubeListBuilder.create().texOffs(0,0).addBox(-4,-4,-6,8,8,7)
                .texOffs(32,0).addBox(-3,-1,-9,6,4,3),PartPose.offset(0,10,-7));
        for(int side : new int[]{-1,1}) {
            head.addOrReplaceChild("antler"+side,CubeListBuilder.create().texOffs(48,0).addBox(-1,-9,-1,2,9,2)
                    .texOffs(48,12).addBox(side<0?-4:0,-7,-1,4,2,2),PartPose.offset(side*3,-3,-1));
        }
        for(int i=0;i<4;i++)root.addOrReplaceChild("leg"+i,CubeListBuilder.create().texOffs(0,48).addBox(-1.5F,0,-1.5F,3,8,3),PartPose.offset(i%2==0?-3.5F:3.5F,16,i<2?-4:6));
        root.addOrReplaceChild("crest",CubeListBuilder.create().texOffs(48,20).addBox(-1,-5,-5,2,5,10),PartPose.offset(0,8,2));
        return LayerDefinition.create(mesh,64,64);
    }
    public static LayerDefinition wisp() {
        MeshDefinition mesh=new MeshDefinition();var root=mesh.getRoot();
        var core=root.addOrReplaceChild("core",CubeListBuilder.create().texOffs(0,0).addBox(-3,-3,-3,6,6,6),PartPose.offsetAndRotation(0,17,0,0,0,0.7854F));
        core.addOrReplaceChild("crown",CubeListBuilder.create().texOffs(24,0).addBox(-1,-7,-1,2,4,2),PartPose.ZERO);
        for(int i=0;i<3;i++)root.addOrReplaceChild("mote"+i,CubeListBuilder.create().texOffs(32,0).addBox(-1,-1,-1,2,2,2),PartPose.offset(0,17,0));
        return LayerDefinition.create(mesh,64,64);
    }
    @Override public void setupAnim(T entity,float limbSwing,float limbAmount,float age,float yaw,float pitch) {
        root.getAllParts().forEach(ModelPart::resetPose);
        if(wisp) {
            var core=root.getChild("core");core.y+=Mth.sin(age*0.08F)*1.2F;core.yRot=age*0.035F;
            for(int i=0;i<3;i++) {var mote=root.getChild("mote"+i);float angle=age*0.06F+i*Mth.TWO_PI/3;mote.x=Mth.cos(angle)*6;mote.z=Mth.sin(angle)*6;mote.y=17+Mth.sin(angle*1.4F)*2;}
        } else {
            var head=root.getChild("head");head.yRot=yaw*Mth.DEG_TO_RAD;head.xRot=pitch*Mth.DEG_TO_RAD;
            for(int i=0;i<4;i++)root.getChild("leg"+i).xRot=Mth.cos(limbSwing*0.6662F+(i==0||i==3?Mth.PI:0))*1.1F*limbAmount;
        }
    }
}
