package tk.darrow.tribalpower.client;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import tk.darrow.tribalpower.entity.CreatureProfile;
public class LatticeCreatureModel<T extends Mob> extends HierarchicalModel<T> {
    private final ModelPart root;
    // Parts found by name once, not by building "leg"+i and friends for every creature every frame; null when absent.
    private final ModelPart head,tail;
    private final ModelPart[] legs=new ModelPart[8],arms=new ModelPart[2],wings=new ModelPart[2],flaps=new ModelPart[2],segs=new ModelPart[8],tendrils=new ModelPart[8];
    public LatticeCreatureModel(ModelPart root) {
        this.root=root;
        head=part("head");tail=part("tail");
        for(int i=0;i<8;i++) {legs[i]=part("leg"+i);segs[i]=part("seg"+i);tendrils[i]=part("tendril"+i);}
        for(int i=0;i<2;i++) {arms[i]=part("arm"+i);wings[i]=part("wing"+i);flaps[i]=part("flap"+i);}
    }
    private ModelPart part(String name) { return root.hasChild(name)?root.getChild(name):null; }
    @Override public ModelPart root() { return root; }
    @Override public void setupAnim(T entity,float swing,float amount,float age,float yaw,float pitch) {
        root.getAllParts().forEach(ModelPart::resetPose);
        if(head!=null) {head.yRot=yaw*Mth.DEG_TO_RAD;head.xRot=pitch*Mth.DEG_TO_RAD;}
        for(int i=0;i<8;i++) if(legs[i]!=null) {
            var leg=legs[i];leg.xRot=Mth.cos(swing*.6662F+(i%3==0?Mth.PI:0))*amount*.85F;
            if(i>=4)leg.zRot=Mth.sin(swing*.6662F+i)*amount*.2F;
        }
        for(int i=0;i<2;i++) {
            if(arms[i]!=null)arms[i].xRot=Mth.cos(swing*.6662F+i*Mth.PI)*amount*.65F;
            if(wings[i]!=null)wings[i].yRot=Mth.sin(age*.28F)*.55F*(i==0?1:-1);
            // Wings held out flat (birds, moths, a ray's fins) beat up and down instead; flap0 is the -x side.
            if(flaps[i]!=null)flaps[i].zRot=Mth.sin(age*.35F)*.5F*(i==0?-1:1);
        }
        // Legless creatures: a wave travels down a serpent or a coil, and trailing parts drift.
        for(int i=0;i<8;i++) {
            if(segs[i]!=null) {var seg=segs[i];seg.yRot=Mth.sin(age*.12F-i*.55F)*.22F+Mth.sin(swing*.6F-i*.5F)*amount*.35F;}
            if(tendrils[i]!=null) {var t=tendrils[i];t.xRot=Mth.cos(age*.09F+i*.7F)*.18F;t.zRot=Mth.sin(age*.07F+i*.9F)*.18F;}
        }
        if(tail!=null)tail.yRot=Mth.sin(age*.08F)*.15F;
        if(CreatureProfile.of(entity.getType()).flying)root.y=Mth.sin(age*.08F)*1.4F;
        if(entity.isBaby()) {root.xScale=.55F;root.yScale=.55F;root.zScale=.55F;root.y+=10.8F;}
    }
}
