package tk.darrow.tribalpower.client;

import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;

/**
 * The March Walker, the Spirit Wisp and the Wandering Spirit. Their bodies are generated rigs (tools/creature_gen:
 * wild_roster.py, wild_bodies.py, in GeneratedMarchLayers): a saddled tapir of a grazer, a will-o'-wisp with a face,
 * and a paper-lantern ghost. Each animates its own groups, found by name once.
 */
public class MarchCreatureModel<T extends Entity> extends HierarchicalModel<T> {
    public static final ModelLayerLocation WALKER = new ModelLayerLocation(ResourceLocation.parse("tribalpower:march_walker"), "main");
    public static final ModelLayerLocation WISP = new ModelLayerLocation(ResourceLocation.parse("tribalpower:spirit_wisp"), "main");
    public static final ModelLayerLocation WANDERER = new ModelLayerLocation(ResourceLocation.parse("tribalpower:wandering_spirit"), "main");
    private final ModelPart root;
    /** Every part, root included, gathered once: getAllParts() built a fresh stream per creature per frame. */
    private final ModelPart[] all;
    private final String kind;
    // Parts found by name once, not by building "leg"+i and friends every frame; null when absent.
    private final ModelPart head,body,tail;
    private final ModelPart[] legs=new ModelPart[4],flaps=new ModelPart[2],tendrils=new ModelPart[3],motes=new ModelPart[3];

    public MarchCreatureModel(ModelPart root, String kind) {
        this.root=root; this.kind=kind;
        all=root.getAllParts().toArray(ModelPart[]::new);
        head=part("head");body=part("body");tail=part("tail");
        for(int i=0;i<4;i++)legs[i]=part("leg"+i);
        for(int i=0;i<2;i++)flaps[i]=part("flap"+i);
        for(int i=0;i<3;i++) {tendrils[i]=part("tendril"+i);motes[i]=part("mote"+i);}
    }
    private ModelPart part(String name) { return root.hasChild(name)?root.getChild(name):null; }
    @Override public ModelPart root() { return root; }

    /** The generated rig for march_walker, spirit_wisp or wandering_spirit, on its 256 texel sheet. */
    public static LayerDefinition create(String id) {
        MeshDefinition mesh=GeneratedMarchLayers.mesh(id);
        if(mesh==null)throw new IllegalArgumentException("No generated rig for "+id);
        return LayerDefinition.create(mesh,256,256);
    }

    @Override public void setupAnim(T entity,float limbSwing,float limbAmount,float age,float yaw,float pitch) {
        for(ModelPart part:all)part.resetPose();
        float phase=entity.getId()*1.3F;
        switch(kind) {
            case "march_walker" -> {
                head.yRot=yaw*Mth.DEG_TO_RAD;head.xRot=pitch*Mth.DEG_TO_RAD;
                for(int i=0;i<4;i++)legs[i].xRot=Mth.cos(limbSwing*0.6662F+(i==0||i==3?Mth.PI:0))*1.1F*limbAmount;
                tail.yRot=Mth.sin(age*0.1F+phase)*0.25F;
            }
            case "spirit_wisp" -> {
                // It bobs and sways, flicks its little flames, trails its wisp and keeps three motes circling.
                float bob=Mth.sin(age*0.08F)*1.2F;
                body.y+=bob;body.yRot=Mth.sin(age*0.03F+phase)*0.35F;
                for(int i=0;i<2;i++) {var f=flaps[i];f.y+=bob;f.zRot=Mth.sin(age*0.2F+i*Mth.PI)*0.3F;}
                var trail=tendrils[0];trail.y+=bob;trail.yRot=Mth.sin(age*0.07F+phase)*0.3F;trail.xRot=Mth.sin(age*0.11F)*0.15F;
                for(int i=0;i<3;i++) {
                    var mote=motes[i];float angle=age*0.06F+i*Mth.TWO_PI/3;
                    mote.x=Mth.cos(angle)*6;mote.z=Mth.sin(angle)*6;mote.y=15.5F+Mth.sin(angle*1.4F)*2;
                }
            }
            case "wandering_spirit" -> {
                // The lantern drifts, swinging a little from its loop; its ribbons and tassel trail.
                float bob=Mth.sin(age*0.06F+phase)*1.0F;
                body.y+=bob;body.zRot=Mth.sin(age*0.05F+phase)*0.06F;body.xRot=Mth.cos(age*0.04F+phase)*0.05F;
                for(int i=0;i<3;i++) {
                    var t=tendrils[i];t.y+=bob;
                    t.xRot=0.15F+Mth.sin(age*0.09F+phase+i)*0.2F;t.zRot=Mth.cos(age*0.07F+phase+i*1.3F)*0.15F;
                }
            }
            default -> {}
        }
    }
}
