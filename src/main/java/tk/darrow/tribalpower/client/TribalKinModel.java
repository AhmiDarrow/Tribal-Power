package tk.darrow.tribalpower.client;

import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import tk.darrow.tribalpower.tribe.KinRole;
import tk.darrow.tribalpower.tribe.TribalKinEntity;

/**
 * Masked, cloaked humanoid. 64x64 texture layout:
 *   head (0,0) 8x8x8 · mask (32,0) 9x9x1 · body (16,16) 8x12x4 · right arm (40,16) 4x12x4 · left arm (0,16) 4x12x4
 *   right leg (0,32) 4x12x4 · left leg (16,32) 4x12x4 · cloak (32,32) 10x14x2 · hood (0,48) 9x5x9
 * The cloak overlay texture (kin_cloak.png) shares this layout; only mask, cloak and hood regions are painted.
 */
public class TribalKinModel extends HierarchicalModel<TribalKinEntity> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(ResourceLocation.parse("tribalpower:tribal_kin"), "main");
    private final ModelPart root, head, body, cloak, rightArm, leftArm, rightLeg, leftLeg;

    public TribalKinModel(ModelPart root) {
        this.root = root;
        head = root.getChild("head");
        body = root.getChild("body");
        cloak = body.getChild("cloak");
        rightArm = root.getChild("right_arm");
        leftArm = root.getChild("left_arm");
        rightLeg = root.getChild("right_leg");
        leftLeg = root.getChild("left_leg");
    }

    public static LayerDefinition create() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-4, -8, -4, 8, 8, 8)
                .texOffs(32, 0).addBox(-4.5F, -8.5F, -5, 9, 9, 1)
                .texOffs(0, 48).addBox(-4.5F, -9, -4.5F, 9, 5, 9, new CubeDeformation(0.25F)), PartPose.offset(0, 0, 0));
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(16, 16).addBox(-4, 0, -2, 8, 12, 4), PartPose.offset(0, 0, 0));
        body.addOrReplaceChild("cloak", CubeListBuilder.create()
                .texOffs(32, 32).addBox(-5, 0, 0, 10, 14, 2), PartPose.offsetAndRotation(0, 0, 2.2F, 0.08F, 0, 0));
        root.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(40, 16).addBox(-3, -2, -2, 4, 12, 4), PartPose.offset(-5, 2, 0));
        root.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(0, 16).mirror().addBox(-1, -2, -2, 4, 12, 4), PartPose.offset(5, 2, 0));
        root.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(0, 32).addBox(-2, 0, -2, 4, 12, 4), PartPose.offset(-1.9F, 12, 0));
        root.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(16, 32).mirror().addBox(-2, 0, -2, 4, 12, 4), PartPose.offset(1.9F, 12, 0));
        return LayerDefinition.create(mesh, 64, 64);
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
