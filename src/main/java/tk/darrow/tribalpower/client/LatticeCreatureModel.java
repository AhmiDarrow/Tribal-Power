package tk.darrow.tribalpower.client;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import tk.darrow.tribalpower.entity.CreatureProfile;
public class LatticeCreatureModel<T extends Mob> extends HierarchicalModel<T> {
    private final ModelPart root;
    public LatticeCreatureModel(ModelPart root) { this.root=root; }
    @Override public ModelPart root() { return root; }
    @Override public void setupAnim(T entity,float swing,float amount,float age,float yaw,float pitch) {
        root.getAllParts().forEach(ModelPart::resetPose);
        if(root.hasChild("head")) {var head=root.getChild("head");head.yRot=yaw*Mth.DEG_TO_RAD;head.xRot=pitch*Mth.DEG_TO_RAD;}
        for(int i=0;i<8;i++) if(root.hasChild("leg"+i)) {
            var leg=root.getChild("leg"+i);leg.xRot=Mth.cos(swing*.6662F+(i%3==0?Mth.PI:0))*amount*.85F;
            if(i>=4)leg.zRot=Mth.sin(swing*.6662F+i)*amount*.2F;
        }
        for(int i=0;i<2;i++) {
            if(root.hasChild("arm"+i))root.getChild("arm"+i).xRot=Mth.cos(swing*.6662F+i*Mth.PI)*amount*.65F;
            if(root.hasChild("wing"+i))root.getChild("wing"+i).yRot=Mth.sin(age*.28F)*.55F*(i==0?1:-1);
        }
        if(root.hasChild("tail"))root.getChild("tail").yRot=Mth.sin(age*.08F)*.15F;
        if(CreatureProfile.of(entity.getType()).flying)root.y=Mth.sin(age*.08F)*1.4F;
        if(entity.isBaby()) {root.xScale=.55F;root.yScale=.55F;root.zScale=.55F;root.y+=10.8F;}
    }
}
