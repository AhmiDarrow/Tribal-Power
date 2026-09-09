package tk.darrow.tribalpower.boss.client;

import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import tk.darrow.tribalpower.boss.TheUnsungEntity;

/**
 * Hollow standing drum with two lacquered ring bands and a taut hide top, two long rune arms with knuckle
 * clubs and a cracked mask head. Authored at half scale (128x128 sheet) and rendered at 2x by the renderer.
 */
public class TheUnsungModel extends HierarchicalModel<TheUnsungEntity> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath("tribalpower", "the_unsung"), "main");
    private final ModelPart root, body, head, arm0, arm1, band0, band1;

    public TheUnsungModel(ModelPart root) {
        this.root = root;
        body = root.getChild("body");
        head = root.getChild("head");
        arm0 = root.getChild("arm0");
        arm1 = root.getChild("arm1");
        band0 = body.getChild("band0");
        band1 = body.getChild("band1");
    }

    public static LayerDefinition create() {
        MeshDefinition mesh = new MeshDefinition();
        var root = mesh.getRoot();
        // drum body: four hollow side panels + hide top (open below), pivot at the drum's centre
        var body = root.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-10F, -9F, -10F, 20F, 18F, 1F)      // north panel
                .texOffs(0, 0).addBox(-10F, -9F, 9F, 20F, 18F, 1F)       // south panel
                .texOffs(44, 0).addBox(-10F, -9F, -9F, 1F, 18F, 18F)     // west panel
                .texOffs(44, 0).addBox(9F, -9F, -9F, 1F, 18F, 18F)       // east panel
                .texOffs(0, 42).addBox(-10F, -10F, -10F, 20F, 1F, 20F),  // taut hide top
                PartPose.offset(0F, 13F, 0F));
        body.addOrReplaceChild("band0", CubeListBuilder.create().texOffs(0, 64).addBox(-11F, -8F, -11F, 22F, 1F, 22F), PartPose.ZERO);
        body.addOrReplaceChild("band1", CubeListBuilder.create().texOffs(0, 64).addBox(-11F, 6F, -11F, 22F, 1F, 22F), PartPose.ZERO);
        // cracked mask head with a crest, sitting on the hide
        root.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(28, 88).addBox(-4F, -7F, -4F, 8F, 7F, 8F)
                .texOffs(62, 88).addBox(-4F, -8F, -5F, 8F, 8F, 1F)     // mask plate
                .texOffs(84, 88).addBox(-1F, -13F, -1F, 2F, 5F, 2F)     // crest
                .texOffs(84, 96).addBox(-6F, -6F, -1F, 2F, 6F, 2F)      // left horn
                .texOffs(84, 96).addBox(4F, -6F, -1F, 2F, 6F, 2F),      // right horn
                PartPose.offset(0F, 3F, 0F));
        // long rune arms pivoting at the drum's upper rim, ending in knuckle clubs
        root.addOrReplaceChild("arm0", CubeListBuilder.create()
                .texOffs(0, 88).addBox(-1.5F, 0F, -1.5F, 3F, 18F, 3F)
                .texOffs(0, 110).addBox(-2.5F, 17F, -2.5F, 5F, 4F, 5F), PartPose.offset(-12F, 5F, 0F));
        root.addOrReplaceChild("arm1", CubeListBuilder.create()
                .texOffs(14, 88).addBox(-1.5F, 0F, -1.5F, 3F, 18F, 3F)
                .texOffs(22, 110).addBox(-2.5F, 17F, -2.5F, 5F, 4F, 5F), PartPose.offset(12F, 5F, 0F));
        return LayerDefinition.create(mesh, 128, 128);
    }

    @Override public ModelPart root() { return root; }

    @Override
    public void setupAnim(TheUnsungEntity entity, float swing, float amount, float age, float yaw, float pitch) {
        root.getAllParts().forEach(ModelPart::resetPose);
        long time = entity.level().getGameTime();
        float partial = age - (int) age;
        head.yRot = yaw * Mth.DEG_TO_RAD;
        head.xRot = pitch * Mth.DEG_TO_RAD;
        // idle pulse: the hollow body breathes, bands lag slightly behind
        float pulse = 1F + 0.035F * Mth.sin(age * 0.16F);
        body.xScale = pulse; body.zScale = pulse;
        float bandPulse = 1F + 0.03F * Mth.sin(age * 0.16F - 0.6F);
        band0.xScale = bandPulse; band0.zScale = bandPulse; band1.xScale = bandPulse; band1.zScale = bandPulse;
        // hover bob
        root.y = Mth.sin(age * 0.09F) * 0.8F;
        // walking rocks the drum
        body.zRot = Mth.cos(swing * 0.5F) * amount * 0.12F;
        if (entity.isStunned()) {
            body.xRot = 0.28F;
            head.xRot += 0.6F;
            arm0.xRot = 0.9F; arm1.xRot = 0.9F;
            arm0.zRot = 0.5F; arm1.zRot = -0.5F;
            return;
        }
        int elapsed = (int) (time - entity.swipeTime());
        int beat = (int) (time - entity.beatTime());
        if (entity.inSilence()) {
            // arms spread and drift, body slowly turning about its axis
            arm0.zRot = 1.1F + Mth.sin(age * 0.12F) * 0.15F;
            arm1.zRot = -1.1F - Mth.sin(age * 0.12F + 1F) * 0.15F;
            arm0.xRot = -0.4F; arm1.xRot = -0.4F;
            body.yRot = age * 0.02F;
            if (elapsed >= 0 && elapsed < 12) { // bolt cast: both arms thrust forward
                float f = elapsed / 12F;
                arm0.xRot = -1.6F + f * 0.8F; arm1.xRot = -1.6F + f * 0.8F;
                arm0.zRot = 0.3F; arm1.zRot = -0.3F;
            }
            return;
        }
        // arm-drum: arms rise and strike the hide in alternation
        float drum = age * 0.35F;
        arm0.xRot = -1.9F + Mth.sin(drum) * 0.45F;
        arm1.xRot = -1.9F + Mth.sin(drum + Mth.PI) * 0.45F;
        arm0.zRot = 0.55F; arm1.zRot = -0.55F;
        if (beat >= 0 && beat < 10) { // shockwave: both arms slam down together, body flares
            float f = 1F - beat / 10F;
            arm0.xRot = -2.4F + f * 0.9F; arm1.xRot = -2.4F + f * 0.9F;
            arm0.zRot = 0.3F; arm1.zRot = -0.3F;
            body.xScale += 0.08F * f; body.zScale += 0.08F * f;
        }
        if (elapsed >= 0 && elapsed < 10) { // melee swipe with the leading arm
            float f = elapsed / 10F;
            arm0.xRot = -2.6F + f * 3.2F;
            arm0.zRot = -0.2F + f * 0.8F;
            head.xRot += 0.25F;
        }
        arm0.xRot += Mth.cos(swing * 0.6F) * amount * 0.3F;
        arm1.xRot += Mth.cos(swing * 0.6F + Mth.PI) * amount * 0.3F;
    }
}
