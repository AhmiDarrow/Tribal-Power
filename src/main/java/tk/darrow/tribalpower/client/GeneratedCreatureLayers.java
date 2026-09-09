package tk.darrow.tribalpower.client;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
/** Exported from art/creatures/tribal_bestiary.blend by tools/blender_bestiary.py. */
public final class GeneratedCreatureLayers {
public static LayerDefinition create(String id) {
MeshDefinition mesh=new MeshDefinition(); var root=mesh.getRoot();
switch(id) {
case "dawn_stag" -> {
root.addOrReplaceChild("body",CubeListBuilder.create().texOffs(0,0).addBox(-4.0F,-3.0F,-7.0F,8.0F,6.0F,14.0F),PartPose.offset(0.0F,12.0F,1.0F));
root.addOrReplaceChild("head",CubeListBuilder.create().texOffs(64,0).addBox(-3.0F,-3.0F,-4.0F,6.0F,6.0F,5.0F).texOffs(128,0).addBox(-2.0F,0.0F,-7.0F,4.0F,3.0F,3.0F).texOffs(192,0).addBox(-3.0F,-5.0F,-2.0F,2.0F,3.0F,2.0F).texOffs(0,32).addBox(1.0F,-5.0F,-2.0F,2.0F,3.0F,2.0F).texOffs(64,32).addBox(-3.2F,-1.0F,-4.2F,1.0F,1.0F,1.0F).texOffs(128,32).addBox(2.2F,-1.0F,-4.2F,1.0F,1.0F,1.0F).texOffs(192,32).addBox(-3.5F,-12.0F,-1.0F,1.0F,9.0F,1.0F).texOffs(0,64).addBox(-7.0F,-9.0F,-1.0F,4.0F,1.0F,1.0F).texOffs(64,64).addBox(-6.5F,-12.0F,-1.0F,1.0F,4.0F,1.0F).texOffs(128,64).addBox(2.5F,-12.0F,-1.0F,1.0F,9.0F,1.0F).texOffs(192,64).addBox(3.0F,-9.0F,-1.0F,4.0F,1.0F,1.0F).texOffs(0,96).addBox(5.5F,-12.0F,-1.0F,1.0F,4.0F,1.0F),PartPose.offset(0.0F,10.0F,-7.0F));
root.addOrReplaceChild("leg0",CubeListBuilder.create().texOffs(64,96).addBox(-1.0F,0.0F,-1.0F,2.0F,10.0F,2.0F),PartPose.offset(-3.0F,14.0F,-4.0F));
root.addOrReplaceChild("leg1",CubeListBuilder.create().texOffs(128,96).addBox(-1.0F,0.0F,-1.0F,2.0F,10.0F,2.0F),PartPose.offset(3.0F,14.0F,-4.0F));
root.addOrReplaceChild("leg2",CubeListBuilder.create().texOffs(192,96).addBox(-1.0F,0.0F,-1.0F,2.0F,10.0F,2.0F),PartPose.offset(-3.0F,14.0F,6.0F));
root.addOrReplaceChild("leg3",CubeListBuilder.create().texOffs(0,128).addBox(-1.0F,0.0F,-1.0F,2.0F,10.0F,2.0F),PartPose.offset(3.0F,14.0F,6.0F));
root.addOrReplaceChild("tail",CubeListBuilder.create().texOffs(64,128).addBox(-2.0F,-2.0F,0.0F,4.0F,4.0F,8.0F).texOffs(128,128).addBox(-1.5F,-1.5F,7.0F,3.0F,3.0F,3.0F),PartPose.offset(0.0F,12.0F,7.0F));
}
case "lantern_fox" -> {
root.addOrReplaceChild("body",CubeListBuilder.create().texOffs(0,0).addBox(-4.0F,-3.0F,-7.0F,8.0F,6.0F,12.0F),PartPose.offset(0.0F,17.0F,1.0F));
root.addOrReplaceChild("head",CubeListBuilder.create().texOffs(64,0).addBox(-3.0F,-3.0F,-4.0F,6.0F,6.0F,5.0F).texOffs(128,0).addBox(-2.0F,0.0F,-7.0F,4.0F,3.0F,3.0F).texOffs(192,0).addBox(-3.0F,-5.0F,-2.0F,2.0F,3.0F,2.0F).texOffs(0,32).addBox(1.0F,-5.0F,-2.0F,2.0F,3.0F,2.0F).texOffs(64,32).addBox(-3.2F,-1.0F,-4.2F,1.0F,1.0F,1.0F).texOffs(128,32).addBox(2.2F,-1.0F,-4.2F,1.0F,1.0F,1.0F),PartPose.offset(0.0F,15.0F,-7.0F));
root.addOrReplaceChild("leg0",CubeListBuilder.create().texOffs(192,32).addBox(-1.0F,0.0F,-1.0F,2.0F,5.0F,2.0F),PartPose.offset(-3.0F,19.0F,-4.0F));
root.addOrReplaceChild("leg1",CubeListBuilder.create().texOffs(0,64).addBox(-1.0F,0.0F,-1.0F,2.0F,5.0F,2.0F),PartPose.offset(3.0F,19.0F,-4.0F));
root.addOrReplaceChild("leg2",CubeListBuilder.create().texOffs(64,64).addBox(-1.0F,0.0F,-1.0F,2.0F,5.0F,2.0F),PartPose.offset(-3.0F,19.0F,6.0F));
root.addOrReplaceChild("leg3",CubeListBuilder.create().texOffs(128,64).addBox(-1.0F,0.0F,-1.0F,2.0F,5.0F,2.0F),PartPose.offset(3.0F,19.0F,6.0F));
root.addOrReplaceChild("tail",CubeListBuilder.create().texOffs(192,64).addBox(-2.0F,-2.0F,0.0F,4.0F,4.0F,8.0F).texOffs(0,96).addBox(-1.5F,-1.5F,7.0F,3.0F,3.0F,3.0F),PartPose.offset(0.0F,17.0F,7.0F));
}
case "mossback" -> {
root.addOrReplaceChild("body",CubeListBuilder.create().texOffs(0,0).addBox(-7.0F,-4.0F,-8.0F,14.0F,6.0F,16.0F).texOffs(64,0).addBox(-5.0F,-7.0F,-6.0F,10.0F,3.0F,12.0F).texOffs(128,0).addBox(-3.0F,-8.0F,-4.0F,6.0F,1.0F,8.0F).texOffs(192,0).addBox(-4.0F,-10.0F,2.0F,1.0F,3.0F,1.0F).texOffs(0,32).addBox(-6.0F,-11.0F,0.0F,5.0F,1.0F,5.0F),PartPose.offset(0.0F,19.0F,1.0F));
root.addOrReplaceChild("head",CubeListBuilder.create().texOffs(64,32).addBox(-2.0F,-2.0F,-4.0F,4.0F,4.0F,5.0F).texOffs(128,32).addBox(-2.2F,-1.0F,-4.2F,1.0F,1.0F,1.0F).texOffs(192,32).addBox(1.2F,-1.0F,-4.2F,1.0F,1.0F,1.0F),PartPose.offset(0.0F,20.0F,-8.0F));
root.addOrReplaceChild("tail",CubeListBuilder.create().texOffs(0,64).addBox(-1.0F,0.0F,0.0F,2.0F,2.0F,4.0F),PartPose.offset(0.0F,20.0F,8.0F));
root.addOrReplaceChild("leg0",CubeListBuilder.create().texOffs(64,64).addBox(-2.0F,0.0F,-2.0F,4.0F,3.0F,4.0F),PartPose.offset(-6.0F,21.0F,-5.0F));
root.addOrReplaceChild("leg1",CubeListBuilder.create().texOffs(128,64).addBox(-2.0F,0.0F,-2.0F,4.0F,3.0F,4.0F),PartPose.offset(6.0F,21.0F,-5.0F));
root.addOrReplaceChild("leg2",CubeListBuilder.create().texOffs(192,64).addBox(-2.0F,0.0F,-2.0F,4.0F,3.0F,4.0F),PartPose.offset(-6.0F,21.0F,5.0F));
root.addOrReplaceChild("leg3",CubeListBuilder.create().texOffs(0,96).addBox(-2.0F,0.0F,-2.0F,4.0F,3.0F,4.0F),PartPose.offset(6.0F,21.0F,5.0F));
}
case "ashbound" -> {
root.addOrReplaceChild("body",CubeListBuilder.create().texOffs(0,0).addBox(-4.0F,-5.0F,-3.0F,8.0F,10.0F,6.0F).texOffs(64,0).addBox(-2.0F,-3.0F,-3.3F,4.0F,4.0F,1.0F),PartPose.offset(0.0F,12.0F,-0.0F));
root.addOrReplaceChild("head",CubeListBuilder.create().texOffs(128,0).addBox(-3.0F,-5.0F,-3.0F,6.0F,6.0F,6.0F).texOffs(192,0).addBox(-2.0F,-2.0F,-3.3F,4.0F,1.0F,1.0F),PartPose.offset(0.0F,6.0F,-0.0F));
root.addOrReplaceChild("leg0",CubeListBuilder.create().texOffs(0,32).addBox(-1.5F,0.0F,-1.5F,3.0F,8.0F,3.0F),PartPose.offset(-2.5F,16.0F,-0.0F));
root.addOrReplaceChild("leg1",CubeListBuilder.create().texOffs(64,32).addBox(-1.5F,0.0F,-1.5F,3.0F,8.0F,3.0F),PartPose.offset(2.5F,16.0F,-0.0F));
root.addOrReplaceChild("arm0",CubeListBuilder.create().texOffs(128,32).addBox(-1.5F,0.0F,-1.5F,3.0F,10.0F,3.0F),PartPose.offset(-6.0F,8.0F,-0.0F));
root.addOrReplaceChild("arm1",CubeListBuilder.create().texOffs(192,32).addBox(-1.5F,0.0F,-1.5F,3.0F,10.0F,3.0F),PartPose.offset(6.0F,8.0F,-0.0F));
}
case "rootbound" -> {
root.addOrReplaceChild("body",CubeListBuilder.create().texOffs(0,0).addBox(-6.0F,-5.0F,-3.0F,12.0F,10.0F,6.0F).texOffs(64,0).addBox(-2.0F,-3.0F,-3.3F,4.0F,4.0F,1.0F).texOffs(128,0).addBox(-8.0F,-8.0F,0.0F,2.0F,8.0F,2.0F).texOffs(192,0).addBox(6.0F,-10.0F,0.0F,2.0F,10.0F,2.0F).texOffs(0,32).addBox(-10.0F,-9.0F,-1.0F,6.0F,2.0F,4.0F),PartPose.offset(0.0F,12.0F,-0.0F));
root.addOrReplaceChild("head",CubeListBuilder.create().texOffs(64,32).addBox(-3.0F,-5.0F,-3.0F,6.0F,6.0F,6.0F).texOffs(128,32).addBox(-2.0F,-2.0F,-3.3F,4.0F,1.0F,1.0F),PartPose.offset(0.0F,6.0F,-0.0F));
root.addOrReplaceChild("leg0",CubeListBuilder.create().texOffs(192,32).addBox(-1.5F,0.0F,-1.5F,3.0F,8.0F,3.0F),PartPose.offset(-2.5F,16.0F,-0.0F));
root.addOrReplaceChild("leg1",CubeListBuilder.create().texOffs(0,64).addBox(-1.5F,0.0F,-1.5F,3.0F,8.0F,3.0F),PartPose.offset(2.5F,16.0F,-0.0F));
root.addOrReplaceChild("arm0",CubeListBuilder.create().texOffs(64,64).addBox(-1.5F,0.0F,-1.5F,3.0F,10.0F,3.0F),PartPose.offset(-8.0F,8.0F,-0.0F));
root.addOrReplaceChild("arm1",CubeListBuilder.create().texOffs(128,64).addBox(-1.5F,0.0F,-1.5F,3.0F,10.0F,3.0F),PartPose.offset(8.0F,8.0F,-0.0F));
}
case "reed_stalker" -> {
root.addOrReplaceChild("body",CubeListBuilder.create().texOffs(0,0).addBox(-4.0F,-3.0F,-7.0F,8.0F,6.0F,12.0F).texOffs(64,0).addBox(-3.0F,-10.0F,3.0F,1.0F,8.0F,1.0F).texOffs(128,0).addBox(2.0F,-8.0F,5.0F,1.0F,6.0F,1.0F).texOffs(192,0).addBox(-0.5F,-12.0F,4.0F,1.0F,10.0F,1.0F),PartPose.offset(0.0F,17.0F,1.0F));
root.addOrReplaceChild("head",CubeListBuilder.create().texOffs(0,32).addBox(-3.0F,-3.0F,-4.0F,6.0F,6.0F,5.0F).texOffs(64,32).addBox(-2.0F,0.0F,-7.0F,4.0F,3.0F,3.0F).texOffs(128,32).addBox(-3.0F,-5.0F,-2.0F,2.0F,3.0F,2.0F).texOffs(192,32).addBox(1.0F,-5.0F,-2.0F,2.0F,3.0F,2.0F).texOffs(0,64).addBox(-3.2F,-1.0F,-4.2F,1.0F,1.0F,1.0F).texOffs(64,64).addBox(2.2F,-1.0F,-4.2F,1.0F,1.0F,1.0F),PartPose.offset(0.0F,15.0F,-7.0F));
root.addOrReplaceChild("leg0",CubeListBuilder.create().texOffs(128,64).addBox(-1.0F,0.0F,-1.0F,2.0F,5.0F,2.0F),PartPose.offset(-3.0F,19.0F,-4.0F));
root.addOrReplaceChild("leg1",CubeListBuilder.create().texOffs(192,64).addBox(-1.0F,0.0F,-1.0F,2.0F,5.0F,2.0F),PartPose.offset(3.0F,19.0F,-4.0F));
root.addOrReplaceChild("leg2",CubeListBuilder.create().texOffs(0,96).addBox(-1.0F,0.0F,-1.0F,2.0F,5.0F,2.0F),PartPose.offset(-3.0F,19.0F,6.0F));
root.addOrReplaceChild("leg3",CubeListBuilder.create().texOffs(64,96).addBox(-1.0F,0.0F,-1.0F,2.0F,5.0F,2.0F),PartPose.offset(3.0F,19.0F,6.0F));
root.addOrReplaceChild("tail",CubeListBuilder.create().texOffs(128,96).addBox(-2.0F,-2.0F,0.0F,4.0F,4.0F,8.0F).texOffs(192,96).addBox(-1.5F,-1.5F,7.0F,3.0F,3.0F,3.0F),PartPose.offset(0.0F,17.0F,7.0F));
}
case "shardback" -> {
root.addOrReplaceChild("body",CubeListBuilder.create().texOffs(0,0).addBox(-6.0F,-4.0F,-5.0F,12.0F,7.0F,12.0F).texOffs(64,0).addBox(-4.0F,-9.0F,0.0F,2.0F,6.0F,2.0F).texOffs(128,0).addBox(1.0F,-12.0F,3.0F,3.0F,9.0F,3.0F).texOffs(192,0).addBox(-1.0F,-7.0F,-3.0F,2.0F,4.0F,2.0F),PartPose.offset(0.0F,17.0F,2.0F));
root.addOrReplaceChild("head",CubeListBuilder.create().texOffs(0,32).addBox(-4.0F,-2.0F,-4.0F,8.0F,4.0F,5.0F).texOffs(64,32).addBox(-3.0F,-1.0F,-4.2F,2.0F,1.0F,1.0F).texOffs(128,32).addBox(1.0F,-1.0F,-4.2F,2.0F,1.0F,1.0F),PartPose.offset(0.0F,18.0F,-5.0F));
root.addOrReplaceChild("leg0",CubeListBuilder.create().texOffs(192,32).addBox(-7.0F,0.0F,-0.7F,7.0F,2.0F,1.4F).texOffs(0,64).addBox(-7.0F,1.0F,-0.7F,2.0F,6.0F,1.4F),PartPose.offset(-5.0F,17.0F,-3.0F));
root.addOrReplaceChild("leg1",CubeListBuilder.create().texOffs(64,64).addBox(0.0F,0.0F,-0.7F,7.0F,2.0F,1.4F).texOffs(128,64).addBox(5.0F,1.0F,-0.7F,2.0F,6.0F,1.4F),PartPose.offset(5.0F,17.0F,-3.0F));
root.addOrReplaceChild("leg2",CubeListBuilder.create().texOffs(192,64).addBox(-7.0F,0.0F,-0.7F,7.0F,2.0F,1.4F).texOffs(0,96).addBox(-7.0F,1.0F,-0.7F,2.0F,6.0F,1.4F),PartPose.offset(-5.0F,17.0F,-0.0F));
root.addOrReplaceChild("leg3",CubeListBuilder.create().texOffs(64,96).addBox(0.0F,0.0F,-0.7F,7.0F,2.0F,1.4F).texOffs(128,96).addBox(5.0F,1.0F,-0.7F,2.0F,6.0F,1.4F),PartPose.offset(5.0F,17.0F,-0.0F));
root.addOrReplaceChild("leg4",CubeListBuilder.create().texOffs(192,96).addBox(-7.0F,0.0F,-0.7F,7.0F,2.0F,1.4F).texOffs(0,128).addBox(-7.0F,1.0F,-0.7F,2.0F,6.0F,1.4F),PartPose.offset(-5.0F,17.0F,3.0F));
root.addOrReplaceChild("leg5",CubeListBuilder.create().texOffs(64,128).addBox(0.0F,0.0F,-0.7F,7.0F,2.0F,1.4F).texOffs(128,128).addBox(5.0F,1.0F,-0.7F,2.0F,6.0F,1.4F),PartPose.offset(5.0F,17.0F,3.0F));
root.addOrReplaceChild("leg6",CubeListBuilder.create().texOffs(192,128).addBox(-7.0F,0.0F,-0.7F,7.0F,2.0F,1.4F).texOffs(0,160).addBox(-7.0F,1.0F,-0.7F,2.0F,6.0F,1.4F),PartPose.offset(-5.0F,17.0F,6.0F));
root.addOrReplaceChild("leg7",CubeListBuilder.create().texOffs(64,160).addBox(0.0F,0.0F,-0.7F,7.0F,2.0F,1.4F).texOffs(128,160).addBox(5.0F,1.0F,-0.7F,2.0F,6.0F,1.4F),PartPose.offset(5.0F,17.0F,6.0F));
}
case "hollow_sentinel" -> {
root.addOrReplaceChild("body",CubeListBuilder.create().texOffs(0,0).addBox(-4.0F,-5.0F,-3.0F,8.0F,10.0F,6.0F).texOffs(64,0).addBox(-2.0F,-3.0F,-3.3F,4.0F,4.0F,1.0F),PartPose.offset(0.0F,9.0F,-0.0F));
root.addOrReplaceChild("head",CubeListBuilder.create().texOffs(128,0).addBox(-3.0F,-5.0F,-3.0F,6.0F,6.0F,6.0F).texOffs(192,0).addBox(-2.0F,-2.0F,-3.3F,4.0F,1.0F,1.0F).texOffs(0,32).addBox(-5.0F,-8.0F,-1.0F,2.0F,9.0F,2.0F).texOffs(64,32).addBox(3.0F,-8.0F,-1.0F,2.0F,9.0F,2.0F).texOffs(128,32).addBox(-1.0F,-6.0F,-3.3F,2.0F,1.0F,1.0F),PartPose.offset(0.0F,3.0F,-0.0F));
root.addOrReplaceChild("leg0",CubeListBuilder.create().texOffs(192,32).addBox(-1.5F,0.0F,-1.5F,3.0F,11.0F,3.0F),PartPose.offset(-2.5F,13.0F,-0.0F));
root.addOrReplaceChild("leg1",CubeListBuilder.create().texOffs(0,64).addBox(-1.5F,0.0F,-1.5F,3.0F,11.0F,3.0F),PartPose.offset(2.5F,13.0F,-0.0F));
root.addOrReplaceChild("arm0",CubeListBuilder.create().texOffs(64,64).addBox(-1.5F,0.0F,-1.5F,3.0F,10.0F,3.0F),PartPose.offset(-6.0F,5.0F,-0.0F));
root.addOrReplaceChild("arm1",CubeListBuilder.create().texOffs(128,64).addBox(-1.5F,0.0F,-1.5F,3.0F,10.0F,3.0F),PartPose.offset(6.0F,5.0F,-0.0F));
}
case "storm_moth" -> {
root.addOrReplaceChild("body",CubeListBuilder.create().texOffs(0,0).addBox(-2.0F,-5.0F,-2.0F,4.0F,11.0F,4.0F),PartPose.offset(0.0F,14.0F,-0.0F));
root.addOrReplaceChild("head",CubeListBuilder.create().texOffs(64,0).addBox(-3.0F,-3.0F,-3.0F,6.0F,4.0F,5.0F).texOffs(128,0).addBox(-4.0F,-7.0F,-1.0F,1.0F,5.0F,1.0F).texOffs(192,0).addBox(3.0F,-7.0F,-1.0F,1.0F,5.0F,1.0F),PartPose.offset(0.0F,8.0F,-0.0F));
root.addOrReplaceChild("wing0",CubeListBuilder.create().texOffs(0,32).addBox(-14.0F,-6.0F,0.0F,14.0F,11.0F,1.0F).texOffs(64,32).addBox(-10.0F,5.0F,0.0F,10.0F,6.0F,1.0F).texOffs(128,32).addBox(-10.0F,-3.0F,-0.2F,6.0F,4.0F,1.0F),PartPose.offset(-2.0F,12.0F,-0.0F));
root.addOrReplaceChild("wing1",CubeListBuilder.create().texOffs(192,32).addBox(0.0F,-6.0F,0.0F,14.0F,11.0F,1.0F).texOffs(0,64).addBox(0.0F,5.0F,0.0F,10.0F,6.0F,1.0F).texOffs(64,64).addBox(4.0F,-3.0F,-0.2F,6.0F,4.0F,1.0F),PartPose.offset(2.0F,12.0F,-0.0F));
}
case "cinder_imp" -> {
root.addOrReplaceChild("body",CubeListBuilder.create().texOffs(0,0).addBox(-4.0F,-5.0F,-3.0F,8.0F,10.0F,6.0F).texOffs(64,0).addBox(-2.0F,-3.0F,-3.3F,4.0F,4.0F,1.0F),PartPose.offset(0.0F,16.0F,-0.0F));
root.addOrReplaceChild("head",CubeListBuilder.create().texOffs(128,0).addBox(-3.0F,-5.0F,-3.0F,6.0F,6.0F,6.0F).texOffs(192,0).addBox(-2.0F,-2.0F,-3.3F,4.0F,1.0F,1.0F).texOffs(0,32).addBox(-5.0F,-6.0F,-1.0F,2.0F,5.0F,2.0F).texOffs(64,32).addBox(3.0F,-6.0F,-1.0F,2.0F,5.0F,2.0F),PartPose.offset(0.0F,10.0F,-0.0F));
root.addOrReplaceChild("leg0",CubeListBuilder.create().texOffs(128,32).addBox(-1.5F,0.0F,-1.5F,3.0F,4.0F,3.0F),PartPose.offset(-2.5F,20.0F,-0.0F));
root.addOrReplaceChild("leg1",CubeListBuilder.create().texOffs(192,32).addBox(-1.5F,0.0F,-1.5F,3.0F,4.0F,3.0F),PartPose.offset(2.5F,20.0F,-0.0F));
root.addOrReplaceChild("arm0",CubeListBuilder.create().texOffs(0,64).addBox(-1.5F,0.0F,-1.5F,3.0F,6.0F,3.0F),PartPose.offset(-6.0F,12.0F,-0.0F));
root.addOrReplaceChild("arm1",CubeListBuilder.create().texOffs(64,64).addBox(-1.5F,0.0F,-1.5F,3.0F,6.0F,3.0F),PartPose.offset(6.0F,12.0F,-0.0F));
}
case "mourning_bell" -> {
root.addOrReplaceChild("body",CubeListBuilder.create().texOffs(0,0).addBox(-5.0F,-7.0F,-5.0F,10.0F,9.0F,10.0F).texOffs(64,0).addBox(-7.0F,2.0F,-7.0F,14.0F,2.0F,14.0F).texOffs(128,0).addBox(-2.0F,-11.0F,-2.0F,4.0F,4.0F,4.0F).texOffs(192,0).addBox(-4.0F,-3.0F,-5.2F,8.0F,1.0F,1.0F),PartPose.offset(0.0F,14.0F,-0.0F));
root.addOrReplaceChild("tail",CubeListBuilder.create().texOffs(0,32).addBox(-1.0F,0.0F,-1.0F,2.0F,6.0F,2.0F).texOffs(64,32).addBox(-2.0F,5.0F,-2.0F,4.0F,2.0F,4.0F),PartPose.offset(0.0F,18.0F,-0.0F));
}
case "rift_hound" -> {
root.addOrReplaceChild("body",CubeListBuilder.create().texOffs(0,0).addBox(-4.0F,-3.0F,-7.0F,8.0F,6.0F,12.0F).texOffs(64,0).addBox(-1.0F,-6.0F,-4.0F,2.0F,3.0F,10.0F).texOffs(128,0).addBox(-2.0F,-5.0F,0.0F,4.0F,1.0F,4.0F),PartPose.offset(0.0F,17.0F,1.0F));
root.addOrReplaceChild("head",CubeListBuilder.create().texOffs(192,0).addBox(-3.0F,-3.0F,-4.0F,6.0F,6.0F,5.0F).texOffs(0,32).addBox(-2.0F,0.0F,-7.0F,4.0F,3.0F,3.0F).texOffs(64,32).addBox(-3.0F,-5.0F,-2.0F,2.0F,3.0F,2.0F).texOffs(128,32).addBox(1.0F,-5.0F,-2.0F,2.0F,3.0F,2.0F).texOffs(192,32).addBox(-3.2F,-1.0F,-4.2F,1.0F,1.0F,1.0F).texOffs(0,64).addBox(2.2F,-1.0F,-4.2F,1.0F,1.0F,1.0F),PartPose.offset(0.0F,15.0F,-7.0F));
root.addOrReplaceChild("leg0",CubeListBuilder.create().texOffs(64,64).addBox(-1.0F,0.0F,-1.0F,2.0F,5.0F,2.0F),PartPose.offset(-3.0F,19.0F,-4.0F));
root.addOrReplaceChild("leg1",CubeListBuilder.create().texOffs(128,64).addBox(-1.0F,0.0F,-1.0F,2.0F,5.0F,2.0F),PartPose.offset(3.0F,19.0F,-4.0F));
root.addOrReplaceChild("leg2",CubeListBuilder.create().texOffs(192,64).addBox(-1.0F,0.0F,-1.0F,2.0F,5.0F,2.0F),PartPose.offset(-3.0F,19.0F,6.0F));
root.addOrReplaceChild("leg3",CubeListBuilder.create().texOffs(0,96).addBox(-1.0F,0.0F,-1.0F,2.0F,5.0F,2.0F),PartPose.offset(3.0F,19.0F,6.0F));
root.addOrReplaceChild("tail",CubeListBuilder.create().texOffs(64,96).addBox(-2.0F,-2.0F,0.0F,4.0F,4.0F,8.0F).texOffs(128,96).addBox(-1.5F,-1.5F,7.0F,3.0F,3.0F,3.0F),PartPose.offset(0.0F,17.0F,7.0F));
}
case "echo_weaver" -> {
root.addOrReplaceChild("body",CubeListBuilder.create().texOffs(0,0).addBox(-6.0F,-4.0F,-5.0F,12.0F,7.0F,12.0F),PartPose.offset(0.0F,17.0F,2.0F));
root.addOrReplaceChild("head",CubeListBuilder.create().texOffs(64,0).addBox(-4.0F,-2.0F,-4.0F,8.0F,4.0F,5.0F).texOffs(128,0).addBox(-3.0F,-1.0F,-4.2F,2.0F,1.0F,1.0F).texOffs(192,0).addBox(1.0F,-1.0F,-4.2F,2.0F,1.0F,1.0F),PartPose.offset(0.0F,18.0F,-5.0F));
root.addOrReplaceChild("leg0",CubeListBuilder.create().texOffs(0,32).addBox(-7.0F,0.0F,-0.7F,7.0F,2.0F,1.4F).texOffs(64,32).addBox(-7.0F,1.0F,-0.7F,2.0F,6.0F,1.4F),PartPose.offset(-5.0F,17.0F,-3.0F));
root.addOrReplaceChild("leg1",CubeListBuilder.create().texOffs(128,32).addBox(0.0F,0.0F,-0.7F,7.0F,2.0F,1.4F).texOffs(192,32).addBox(5.0F,1.0F,-0.7F,2.0F,6.0F,1.4F),PartPose.offset(5.0F,17.0F,-3.0F));
root.addOrReplaceChild("leg2",CubeListBuilder.create().texOffs(0,64).addBox(-7.0F,0.0F,-0.7F,7.0F,2.0F,1.4F).texOffs(64,64).addBox(-7.0F,1.0F,-0.7F,2.0F,6.0F,1.4F),PartPose.offset(-5.0F,17.0F,-0.0F));
root.addOrReplaceChild("leg3",CubeListBuilder.create().texOffs(128,64).addBox(0.0F,0.0F,-0.7F,7.0F,2.0F,1.4F).texOffs(192,64).addBox(5.0F,1.0F,-0.7F,2.0F,6.0F,1.4F),PartPose.offset(5.0F,17.0F,-0.0F));
root.addOrReplaceChild("leg4",CubeListBuilder.create().texOffs(0,96).addBox(-7.0F,0.0F,-0.7F,7.0F,2.0F,1.4F).texOffs(64,96).addBox(-7.0F,1.0F,-0.7F,2.0F,6.0F,1.4F),PartPose.offset(-5.0F,17.0F,3.0F));
root.addOrReplaceChild("leg5",CubeListBuilder.create().texOffs(128,96).addBox(0.0F,0.0F,-0.7F,7.0F,2.0F,1.4F).texOffs(192,96).addBox(5.0F,1.0F,-0.7F,2.0F,6.0F,1.4F),PartPose.offset(5.0F,17.0F,3.0F));
root.addOrReplaceChild("leg6",CubeListBuilder.create().texOffs(0,128).addBox(-7.0F,0.0F,-0.7F,7.0F,2.0F,1.4F).texOffs(64,128).addBox(-7.0F,1.0F,-0.7F,2.0F,6.0F,1.4F),PartPose.offset(-5.0F,17.0F,6.0F));
root.addOrReplaceChild("leg7",CubeListBuilder.create().texOffs(128,128).addBox(0.0F,0.0F,-0.7F,7.0F,2.0F,1.4F).texOffs(192,128).addBox(5.0F,1.0F,-0.7F,2.0F,6.0F,1.4F),PartPose.offset(5.0F,17.0F,6.0F));
}
case "tribal_kin" -> {
root.addOrReplaceChild("head",CubeListBuilder.create().texOffs(0,0).addBox(-4.0F,-8.0F,-4.0F,8.0F,8.0F,8.0F).texOffs(64,0).addBox(-4.5F,-8.5F,-5.0F,9.0F,9.0F,1.0F).texOffs(128,0).addBox(-4.5F,-9.0F,-4.5F,9.0F,5.0F,9.0F).texOffs(192,0).addBox(-4.5F,-2.0F,3.5F,1.0F,6.0F,1.0F).texOffs(0,32).addBox(3.5F,-2.0F,3.5F,1.0F,6.0F,1.0F),PartPose.offset(0.0F,0.0F,-0.0F));
root.addOrReplaceChild("body",CubeListBuilder.create().texOffs(64,32).addBox(-4.0F,0.0F,-2.0F,8.0F,12.0F,4.0F).texOffs(128,32).addBox(-4.5F,7.0F,-2.5F,9.0F,2.0F,5.0F),PartPose.offset(0.0F,0.0F,-0.0F));
root.addOrReplaceChild("cloak",CubeListBuilder.create().texOffs(192,32).addBox(-5.0F,0.0F,0.0F,10.0F,14.0F,2.0F),PartPose.offset(0.0F,0.0F,2.2F));
root.addOrReplaceChild("arm0",CubeListBuilder.create().texOffs(0,64).addBox(-3.0F,-2.0F,-2.0F,4.0F,12.0F,4.0F),PartPose.offset(-5.0F,2.0F,-0.0F));
root.addOrReplaceChild("arm1",CubeListBuilder.create().texOffs(64,64).addBox(-1.0F,-2.0F,-2.0F,4.0F,12.0F,4.0F),PartPose.offset(5.0F,2.0F,-0.0F));
root.addOrReplaceChild("leg0",CubeListBuilder.create().texOffs(128,64).addBox(-2.0F,0.0F,-2.0F,4.0F,12.0F,4.0F),PartPose.offset(-1.9F,12.0F,-0.0F));
root.addOrReplaceChild("leg1",CubeListBuilder.create().texOffs(192,64).addBox(-2.0F,0.0F,-2.0F,4.0F,12.0F,4.0F),PartPose.offset(1.9F,12.0F,-0.0F));
}
case "the_unsung" -> {
root.addOrReplaceChild("body",CubeListBuilder.create().texOffs(0,0).addBox(-10.0F,-6.0F,-10.0F,9.0F,13.0F,1.0F).texOffs(64,0).addBox(-10.0F,-6.0F,9.0F,9.0F,13.0F,1.0F).texOffs(128,0).addBox(1.0F,-6.0F,-10.0F,9.0F,13.0F,1.0F).texOffs(192,0).addBox(1.0F,-6.0F,9.0F,9.0F,13.0F,1.0F).texOffs(0,32).addBox(-10.0F,-6.0F,-9.0F,1.0F,13.0F,8.0F).texOffs(64,32).addBox(9.0F,-6.0F,-9.0F,1.0F,13.0F,8.0F).texOffs(128,32).addBox(-10.0F,-6.0F,1.0F,1.0F,13.0F,8.0F).texOffs(192,32).addBox(9.0F,-6.0F,1.0F,1.0F,13.0F,8.0F).texOffs(0,64).addBox(-11.0F,-11.0F,-11.0F,22.0F,1.0F,22.0F),PartPose.offset(0.0F,13.0F,-0.0F));
root.addOrReplaceChild("band0",CubeListBuilder.create().texOffs(128,64).addBox(-12.0F,-10.0F,-12.0F,24.0F,4.0F,24.0F),PartPose.offset(0.0F,13.0F,-0.0F));
root.addOrReplaceChild("band1",CubeListBuilder.create().texOffs(0,96).addBox(-12.0F,7.0F,-12.0F,24.0F,4.0F,24.0F),PartPose.offset(0.0F,13.0F,-0.0F));
root.addOrReplaceChild("head",CubeListBuilder.create().texOffs(128,96).addBox(-4.0F,-7.0F,-4.0F,8.0F,7.0F,8.0F).texOffs(192,96).addBox(-4.0F,-8.0F,-5.0F,8.0F,8.0F,1.0F).texOffs(0,128).addBox(-6.0F,-6.0F,-1.0F,2.0F,6.0F,2.0F).texOffs(64,128).addBox(4.0F,-6.0F,-1.0F,2.0F,6.0F,2.0F),PartPose.offset(0.0F,-3.0F,-0.0F));
root.addOrReplaceChild("halo",CubeListBuilder.create().texOffs(128,128).addBox(-6.0F,0.0F,-6.0F,12.0F,1.0F,1.0F).texOffs(192,128).addBox(-6.0F,0.0F,5.0F,12.0F,1.0F,1.0F).texOffs(0,160).addBox(-6.0F,0.0F,-5.0F,1.0F,1.0F,10.0F).texOffs(64,160).addBox(5.0F,0.0F,-5.0F,1.0F,1.0F,10.0F),PartPose.offset(0.0F,-12.0F,-0.0F));
root.addOrReplaceChild("arm0",CubeListBuilder.create().texOffs(128,160).addBox(-1.5F,0.0F,-1.5F,3.0F,15.0F,3.0F).texOffs(192,160).addBox(-3.0F,15.0F,-2.0F,6.0F,3.0F,4.0F).texOffs(0,192).addBox(-3.0F,18.0F,-1.0F,1.5F,4.0F,2.0F).texOffs(64,192).addBox(-0.75F,18.0F,-1.0F,1.5F,4.0F,2.0F).texOffs(128,192).addBox(1.5F,18.0F,-1.0F,1.5F,4.0F,2.0F),PartPose.offset(-14.0F,3.0F,-0.0F));
root.addOrReplaceChild("arm1",CubeListBuilder.create().texOffs(192,192).addBox(-1.5F,0.0F,-1.5F,3.0F,15.0F,3.0F).texOffs(0,224).addBox(-3.0F,15.0F,-2.0F,6.0F,3.0F,4.0F).texOffs(64,224).addBox(-3.0F,18.0F,-1.0F,1.5F,4.0F,2.0F).texOffs(128,224).addBox(-0.75F,18.0F,-1.0F,1.5F,4.0F,2.0F).texOffs(192,224).addBox(1.5F,18.0F,-1.0F,1.5F,4.0F,2.0F),PartPose.offset(14.0F,3.0F,-0.0F));
}
default -> throw new IllegalArgumentException("Unknown creature: "+id);
}
return LayerDefinition.create(mesh,256,256);
}
}
