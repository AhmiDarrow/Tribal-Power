package tk.darrow.tribalpower.boss.client;

import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import tk.darrow.tribalpower.boss.TheUnsungEntity;
import tk.darrow.tribalpower.client.GeneratedCreatureLayers;

/**
 * Hollow standing drum with two lacquered ring bands and a taut hide top, two long rune arms with knuckle
 * clubs and a cracked mask head. Geometry comes from the Blender roster (art/creatures/roster_tribes.json,
 * exported by tools/blender_bestiary.py into {@link GeneratedCreatureLayers}; the 256x256 atlas and glow map are
 * painted by tools/art/creatures.py). Authored at half scale and rendered at 2x by the renderer. Parts are flat
 * root children: {@code body} (six lacquer panels + two hide halves), {@code band0}/{@code band1} (four rails
 * each), {@code head} (skull, mask, crest, horns) and {@code arm0}/{@code arm1} (arm + club).
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
        band0 = root.getChild("band0");
        band1 = root.getChild("band1");
    }

    public static LayerDefinition create() {
        return GeneratedCreatureLayers.create("the_unsung");
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
        band0.zRot = body.zRot; band1.zRot = body.zRot;
        if (entity.isStunned()) {
            body.xRot = 0.28F; band0.xRot = 0.28F; band1.xRot = 0.28F;
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
            body.yRot = age * 0.02F; band0.yRot = body.yRot; band1.yRot = body.yRot;
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
