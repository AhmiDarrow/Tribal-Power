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
 * Hollow standing drum: a slit lacquer shell between two wide bands, a taut hide top, a mask head floating
 * half a block above the hide under a thin rune halo, and two long rune arms hanging from shoulder pivots at
 * the top band that end in three-fingered hands. Geometry comes from the Blender roster
 * (art/creatures/roster_tribes.json, exported by tools/blender_bestiary.py into {@link GeneratedCreatureLayers};
 * the 256x256 atlas and glow map are painted by tools/art/creatures.py). Authored at half scale and rendered
 * at 2x by the renderer. Parts are flat root children: {@code body} (eight shell panels + hide), {@code band0}
 * (top), {@code band1} (bottom), {@code head}, {@code halo}, {@code arm0} (left, -x) and {@code arm1}.
 */
public class TheUnsungModel extends HierarchicalModel<TheUnsungEntity> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath("tribalpower", "the_unsung"), "main");
    private final ModelPart root, body, head, halo, arm0, arm1, band0, band1;

    public TheUnsungModel(ModelPart root) {
        this.root = root;
        body = root.getChild("body");
        head = root.getChild("head");
        halo = root.getChild("halo");
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
        head.yRot = yaw * Mth.DEG_TO_RAD;
        head.xRot = pitch * Mth.DEG_TO_RAD;
        // idle: the hollow body breathes (bands lag slightly), the mask bobs in its gap, the halo turns
        float pulse = 1F + 0.035F * Mth.sin(age * 0.16F);
        body.xScale = pulse; body.zScale = pulse;
        float bandPulse = 1F + 0.03F * Mth.sin(age * 0.16F - 0.6F);
        band0.xScale = bandPulse; band0.zScale = bandPulse; band1.xScale = bandPulse; band1.zScale = bandPulse;
        head.y += Mth.sin(age * 0.12F) * 1.2F;
        halo.y += Mth.sin(age * 0.12F + 0.8F) * 0.8F;
        halo.yRot = age * 0.04F;
        root.y = Mth.sin(age * 0.09F) * 0.8F;
        // walking rocks the drum; everything stacked on the body rocks with it
        body.zRot = Mth.cos(swing * 0.5F) * amount * 0.12F;
        band0.zRot = body.zRot; band1.zRot = body.zRot;
        // hanging arms: slightly spread, swaying with the stride
        float sway = Mth.cos(swing * 0.6F) * amount * 0.3F;
        arm0.zRot = 0.18F + Mth.sin(age * 0.11F) * 0.04F; arm1.zRot = -arm0.zRot;
        arm0.xRot = sway; arm1.xRot = -sway;
        if (entity.isStunned()) {
            body.xRot = 0.28F; band0.xRot = 0.28F; band1.xRot = 0.28F;
            head.xRot += 0.6F; head.y += 3F; halo.y += 4F;
            arm0.xRot = 0.6F; arm1.xRot = 0.6F;
            arm0.zRot = 0.45F; arm1.zRot = -0.45F;
            return;
        }
        int elapsed = (int) (time - entity.swipeTime());
        int beat = (int) (time - entity.beatTime());
        if (entity.inSilence()) {
            // arms spread wide and drift, the body slowly turning about its axis
            arm0.zRot = 1.3F + Mth.sin(age * 0.12F) * 0.15F;
            arm1.zRot = -1.3F - Mth.sin(age * 0.12F + 1F) * 0.15F;
            arm0.xRot = -0.3F; arm1.xRot = -0.3F;
            body.yRot = age * 0.02F; band0.yRot = body.yRot; band1.yRot = body.yRot;
            if (elapsed >= 0 && elapsed < 12) { // bolt cast: both hands thrust forward
                float f = elapsed / 12F;
                arm0.xRot = -1.7F + f * 0.9F; arm1.xRot = -1.7F + f * 0.9F;
                arm0.zRot = 0.35F; arm1.zRot = -0.35F;
            }
            return;
        }
        // drum-beat: both arms rise out and over the hide, then slam down onto it together
        if (beat >= 0 && beat < 12) {
            float f = beat / 12F;
            float lift = f < 0.4F ? Mth.sin(f / 0.4F * Mth.HALF_PI) : 1F - Mth.sin((f - 0.4F) / 0.6F * Mth.HALF_PI) * 0.55F;
            arm0.zRot = 0.18F + lift * 2.3F; arm1.zRot = -arm0.zRot;
            arm0.xRot = -0.25F * lift; arm1.xRot = arm0.xRot;
            float flare = f >= 0.4F ? 1F - (f - 0.4F) / 0.6F : 0F;
            body.xScale += 0.08F * flare; body.zScale += 0.08F * flare;
            head.y -= flare * 2F;
        } else {
            // between beats the arms rise slowly, alternating, ready for the next strike
            float drum = age * 0.35F;
            arm0.zRot += 0.35F + Mth.sin(drum) * 0.25F;
            arm1.zRot -= 0.35F + Mth.sin(drum + Mth.PI) * 0.25F;
        }
        if (elapsed >= 0 && elapsed < 10) { // melee swipe: the leading arm sweeps forward across the front
            float f = elapsed / 10F;
            arm0.xRot = -2.4F + f * 3.0F;
            arm0.zRot = 0.9F - f * 0.6F;
            head.xRot += 0.25F;
        }
    }
}
