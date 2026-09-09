package tk.darrow.tribalpower.client;

import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import tk.darrow.tribalpower.tribe.KinRole;
import tk.darrow.tribalpower.tribe.TribalKinEntity;

/**
 * Masked, cloaked humanoid. Geometry comes from the Blender roster (art/creatures/roster_tribes.json, exported
 * by tools/blender_bestiary.py into {@link GeneratedCreatureLayers}) on a 256x256 atlas painted per role by
 * tools/art/creatures.py: parts {@code head} (skull, mask plate, hood, two braids), {@code body} (torso, belt),
 * {@code cloak}, {@code arm0}/{@code arm1} (right/left) and {@code leg0}/{@code leg1} (right/left).
 * The tinted cloak overlay (kin_cloak_&lt;tribe&gt;.png) shares the same layout; only hood and cloak are painted.
 */
public class TribalKinModel extends HierarchicalModel<TribalKinEntity> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(ResourceLocation.parse("tribalpower:tribal_kin"), "main");
    private final ModelPart root, head, body, cloak, rightArm, leftArm, rightLeg, leftLeg;

    public TribalKinModel(ModelPart root) {
        this.root = root;
        head = root.getChild("head");
        body = root.getChild("body");
        cloak = root.getChild("cloak");
        rightArm = root.getChild("arm0");
        leftArm = root.getChild("arm1");
        rightLeg = root.getChild("leg0");
        leftLeg = root.getChild("leg1");
    }

    public static LayerDefinition create() {
        return GeneratedCreatureLayers.create("tribal_kin");
    }

    @Override public ModelPart root() { return root; }

    @Override
    public void setupAnim(TribalKinEntity entity, float limbSwing, float limbAmount, float age, float yaw, float pitch) {
        root.getAllParts().forEach(ModelPart::resetPose);
        head.yRot = yaw * Mth.DEG_TO_RAD;
        head.xRot = pitch * Mth.DEG_TO_RAD;
        float swing = Mth.cos(limbSwing * 0.6662F) * 1.2F * limbAmount;
        rightLeg.xRot = swing;
        leftLeg.xRot = -swing;
        rightArm.xRot = -swing * 0.8F;
        leftArm.xRot = swing * 0.8F;
        rightArm.zRot += Mth.cos(age * 0.09F) * 0.05F + 0.05F;
        leftArm.zRot -= Mth.cos(age * 0.09F) * 0.05F + 0.05F;
        body.y = 0; // the cloak hangs from the shoulders and swings with the stride
        cloak.xRot = 0.08F + limbAmount * 0.35F + Mth.sin(age * 0.07F) * 0.02F;
        KinRole role = entity.role();
        if (role == KinRole.DRUMMER) {
            // arms raised over an unseen drum; on a beat both strike down
            float beat = entity.beatProgress(0);
            float strike = beat > 0 ? Mth.sin(beat * Mth.PI) : 0;
            rightArm.xRot = -1.35F + strike * 1.1F + Mth.sin(age * 0.2F) * 0.08F;
            leftArm.xRot = -1.35F + strike * 1.1F - Mth.sin(age * 0.2F) * 0.08F;
            rightArm.zRot = -0.15F;
            leftArm.zRot = 0.15F;
        } else if (role == KinRole.WEAVER) {
            // hands working a loom in front of the body
            rightArm.xRot = -0.9F + Mth.sin(age * 0.15F) * 0.25F;
            leftArm.xRot = -0.9F - Mth.sin(age * 0.15F) * 0.25F;
            rightArm.zRot = -0.25F;
            leftArm.zRot = 0.25F;
        } else if (role == KinRole.HUNTER && entity.isAggressive()) {
            rightArm.xRot = -1.5F + Mth.sin(attackTime * Mth.PI) * 1.2F;
        } else if (role == KinRole.ELDER) {
            rightArm.xRot -= 0.35F; // leaning on a staff
        }
    }
}
